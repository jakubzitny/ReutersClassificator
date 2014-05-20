package tw.edu.ntut.reutersclassificator;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import tw.edu.ntut.reutersclassificator.entity.Document;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * ClassificatorConsumer
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 22:53 2014
 */
public class ClassificatorConsumer implements Runnable {

    /** number of results */
    private static final int HITS_PER_PAGE = 10;

    /** default search field in Document */
    private static final String DEFAULT_SEARCH_FIELD = "text";

    /** system-specific temp directory with place to write indexes */
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir") + "/WarcSearch/" + System.nanoTime();

    /** lucene analyzer */
    private static StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);

    /** lucene directory - use RAMDirectory to store indexes in memory, FSDirectory in file */
    //private static Directory _index = new RAMDirectory();
    private FSDirectory _dir;

    /** lucene index reader, searcher, collector */
    private IndexWriterConfig _config;
    private IndexWriter _writer;
    private IndexReader _reader;
    private IndexSearcher _searcher;
    private TopScoreDocCollector _collector;

    /** produced documents go to this shared queue */
    private final LinkedBlockingQueue<Document> _queue;

    /** number of consuming threads */
    private final int _threadNo;

    /**
     * Indexer constructor
     * prepares Indexer default needs
     * IndexWriterConfig specifies IndexWriters behaviour
     * FSDirectory opens tmp file as index storage
     */
    public ClassificatorConsumer(LinkedBlockingQueue<Document> queue, int threadNo) {
        _queue = queue;
        _threadNo = threadNo;
        _config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
        _config.setOpenMode(OpenMode.CREATE);
        _config.setSimilarity(new DefaultSimilarity()); // DefaultSimilarity is subclass of TFIDFSimilarity
        try {
            _dir = FSDirectory.open(new File(TMP_DIR));
        } catch (IOException e) {
            System.err.println("There was a problem with tmp dir in your system.");
            System.err.println(e.getMessage());
            e.getStackTrace();
        }
    }

    /**
     * output point
     * searches query in indexed Documents
     * @param querystr string o user's query
     * @return array of Result objects sorted by ranking
     */
//    public ArrayList<Result> search(String querystr) {
//        Query query = prepareQuery(querystr);
//        ArrayList<Result> results = new ArrayList<Result>();
//        try {
//            openReader();
//            _searcher.search(query, _collector);
//            ScoreDoc[] hits = _collector.topDocs().scoreDocs;
//            for (int i = 0; i < hits.length; ++i) {
//                int docId = hits[i].doc;
//                Document d = _searcher.doc(docId);
//                results.add(new Result(d, docId, hits[i].score));
//            }
//            closeReader();
//        } catch (IOException e) {
//            System.err.println("There was a problem with searching Documents.");
//            System.err.println(e.getMessage());
//            e.printStackTrace();
//        }
//        return results;
//    }

    /**
     * creates Query from user's input query string
     * @param querystr
     * @return Query
     */
    private Query prepareQuery(String querystr) {
        Query query = null;
        try {
            query = new QueryParser(Version.LUCENE_47, DEFAULT_SEARCH_FIELD, analyzer).parse(querystr);
        } catch (org.apache.lucene.queryparser.classic.ParseException e) {
            System.err.println("There was a problem with parsing your query.");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return query;
    }

    /**
     * opens configured IndexReader for indexing
     * @throws IOException
     */
    private void openReader() throws IOException {
        try {
            _reader = DirectoryReader.open(_dir);
            _searcher = new IndexSearcher(_reader);
            _collector = TopScoreDocCollector.create(HITS_PER_PAGE, true);
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }
    }

    /**
     * closes opened IndexReader
     * @throws IOException
     */
    private void closeReader() throws IOException {
        _reader.close();
    }

    /**
     * runs the indexer
     * delegates tasks (taking from queue and adding to indexwriter)
     * to IndexerTask threads
     */
    @Override
    public void run() {
        try {
            _writer = new IndexWriter(_dir, _config);
            List<Thread> threads = new ArrayList<Thread>();
            for (int i = 0; i < _threadNo; i++) {
                threads.add(new Thread(new IndexerTask(_writer, _queue, i)));
            }
            for (Thread t: threads)
                t.start();
            for (Thread t: threads)
                t.join();
            _writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * runnable IndexerTask class for executing the heavy stuff
     * - taking produced records from queue and adding them to indexwriter
     */
    private class IndexerTask implements Runnable {

        private IndexWriter _writer;
        private LinkedBlockingQueue<Document> _queue;
        private int _threadNumber;

        /**
         * constructor
         * sets up reference to opened indexwriter and queue
         * holds the information which thread number it is for debugging
         * @param writer
         * @param queue
         * @param threadNumber
         */
        public IndexerTask(IndexWriter writer, LinkedBlockingQueue<Document> queue, int threadNumber) {
            _writer = writer;
            _queue = queue;
            _threadNumber = threadNumber;
        }

        /**
         * the heavy duty
         */
        @Override
        public void run() {
            int i = 0;
            while (true) {
                try {
                    Document doc = _queue.take();
                    if (doc.isTerminator()) break;
                    _writer.addDocument(doc.getLuceneDocument());
                    if (++i%1000==0)
                        System.out.println("INFO: Thread#"+_threadNumber+" processed " + i);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

}
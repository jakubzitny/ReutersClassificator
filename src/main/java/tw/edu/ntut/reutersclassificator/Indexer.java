package tw.edu.ntut.reutersclassificator;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import tw.edu.ntut.reutersclassificator.entity.Document;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import tw.edu.ntut.reutersclassificator.entity.TermVector;

/**
 * ClassificatorConsumer
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 22:53 2014
 */
public class Indexer implements Runnable {

    /** number of results */
    private static final int HITS_PER_PAGE = 10;

    /** lucene version for initialization of control objects */
    private static final Version LUCENE_VERSION = Version.LUCENE_48;

    /** default search field in Document */
    private static final String DEFAULT_SEARCH_FIELD = "body";

    /** system-specific temp directory with place to write indexes */
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir")
            + "/ReutersClassificator/" + System.nanoTime();

    /** lucene analyzer */
    private static StandardAnalyzer mAnalyzer = new StandardAnalyzer(LUCENE_VERSION);

    /** lucene directory - use RAMDirectory to store indexes in memory, FSDirectory in file */
    //private static Directory _index = new RAMDirectory();
    private FSDirectory mDir;

    /** lucene index reader, searcher, collector */
    private IndexWriterConfig mConfig;
    private IndexWriter mWriter;
    private IndexReader mReader;
    private IndexSearcher mSearcher;
    private TopScoreDocCollector mCollector;

    /** produced documents go to this shared queue */
    private LinkedBlockingQueue<Document> mQueue = null;

    /** number of consuming threads */
    private int mThreadNo = 0;

    private Map<Integer, Document> mDocuments;

    /**
     * Indexer constructor
     * prepares Indexer default needs
     * IndexWriterConfig specifies IndexWriters behaviour
     * FSDirectory opens tmp file as index storage
     */
    public Indexer(LinkedBlockingQueue<Document> queue, int threadNo) {
        mQueue = queue;
        mThreadNo = threadNo;
        mConfig = new IndexWriterConfig(LUCENE_VERSION, mAnalyzer);
        mConfig.setOpenMode(OpenMode.CREATE);
        mConfig.setSimilarity(new DefaultSimilarity()); // DefaultSimilarity is subclass of TFIDFSimilarity
        try {
            mDir = FSDirectory.open(new File(TMP_DIR));
        } catch (IOException e) {
            System.err.println("There was a problem with tmp dir in your system.");
            System.err.println(e.getMessage());
            e.getStackTrace();
        }
    }

    public Indexer (Map<Integer, Document> docs) {
        mConfig = new IndexWriterConfig(LUCENE_VERSION, mAnalyzer);
        mConfig.setOpenMode(OpenMode.CREATE);
        mConfig.setSimilarity(new DefaultSimilarity()); // DefaultSimilarity is subclass of TFIDFSimilarity
        try {
            mDir = FSDirectory.open(new File(TMP_DIR));
        } catch (IOException e) {
            System.err.println("There was a problem with tmp dir in your system.");
            System.err.println(e.getMessage());
            e.getStackTrace();
        }
        mDocuments = docs;
    }


    /**
     * IndexReader http://bit.ly/1jXBg1F
     *
     */
    public void retrieveTermVectors() {
        try {
            mReader = DirectoryReader.open(mDir);
            for (int i = 0; i < mReader.maxDoc(); i++) {
//                if (mReader.isDeleted(i))
//                    continue;
                org.apache.lucene.document.Document doc = mReader.document(i);
                long tf = mReader.getSumTotalTermFreq(DEFAULT_SEARCH_FIELD);
                long idf = -1;
                try {
                    idf = mReader.getTermVector(i, DEFAULT_SEARCH_FIELD).getSumDocFreq();
                } catch (NullPointerException e) {
                    System.out.println("e");
                }
                TermVector tfIdf = TermVector.create(tf, idf);
                mDocuments.get(Integer.parseInt(doc.get("newId"))).setTermVector(tfIdf);
            }
        } catch (IOException e) {
            System.err.println("There was a problem with searching indexed Documents.");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * output point
     * searches query in indexed Documents
     * @param querystr string o user's query
     * @return array of Result objects sorted by ranking
     */
    public ArrayList<Double> search(String querystr) {
        Query query = prepareQuery(querystr);
        ArrayList<Double> results = new ArrayList<Double>();
        try {
            openReader();
            mSearcher.search(query, mCollector);
            ScoreDoc[] hits = mCollector.topDocs().scoreDocs;
            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                org.apache.lucene.document.Document d = mSearcher.doc(docId);
                mSearcher.getIndexReader().getTermVector(docId, "body"); // mam tfidf vector pre doc yayy
                results.add(new Double(hits[i].score));
            }
            closeReader();
        } catch (IOException e) {
            System.err.println("There was a problem with searching Documents.");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    /**
     * creates Query from user's input query string
     * @param querystr
     * @return Query
     */
    private Query prepareQuery(String querystr) {
        Query query = null;
        try {
            query = new QueryParser(LUCENE_VERSION, DEFAULT_SEARCH_FIELD, mAnalyzer).parse(querystr);
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
            mReader = DirectoryReader.open(mDir);
            mSearcher = new IndexSearcher(mReader);
            mCollector = TopScoreDocCollector.create(HITS_PER_PAGE, true);
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
        mReader.close();
    }

    /**
     * runs the indexer
     * delegates tasks (taking from queue and adding to indexwriter)
     * to IndexerTask threads
     */
    @Override
    public void run() {
        indexParallel();
    }

    public void index() {
        try {
            mWriter = new IndexWriter(mDir, mConfig);
            for (Integer docKey : mDocuments.keySet()) {
                Document doc = mDocuments.get(docKey);
                if (doc.getBody() == null) continue;
                try {
                    mWriter.addDocument(doc.getLuceneDocument());
//                if (++i%1000==0)
//                    System.out.println("INFO: Thread#"+mThreadNumber+" processed " + i);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            mWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void indexParallel() {
        try {
            mWriter = new IndexWriter(mDir, mConfig);
            List<Thread> threads = new ArrayList<Thread>();
            for (int i = 0; i < mThreadNo; i++) {
                threads.add(new Thread(new IndexerTask(mWriter, mQueue, i)));
            }
            for (Thread t: threads)
                t.start();
            for (Thread t: threads)
                t.join();
            mWriter.close();
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

        private IndexWriter mWriter;
        private LinkedBlockingQueue<Document> mQueue;
        private int mThreadNumber;

        /**
         * constructor
         * sets up reference to opened indexwriter and queue
         * holds the information which thread number it is for debugging
         * @param writer
         * @param queue
         * @param threadNumber
         */
        public IndexerTask(IndexWriter writer, LinkedBlockingQueue<Document> queue, int threadNumber) {
            mWriter = writer;
            mQueue = queue;
            mThreadNumber = threadNumber;
        }

        /**
         * the heavy duty
         */
        @Override
        public void run() {
            int i = 0;
            while (true) {
                try {
                    Document doc;
                    doc = mQueue.take();
                    if (doc.isTerminator()) break;
                    mWriter.addDocument(doc.getLuceneDocument());
                    if (++i%1000==0)
                        System.out.println("INFO: Thread#"+mThreadNumber+" processed " + i);
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
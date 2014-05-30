package tw.edu.ntut.reutersclassificator;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.lucene46.Lucene46Codec;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.BytesRef;
import tw.edu.ntut.reutersclassificator.entity.Document;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class Indexer {

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

    /** produced documents go to this shared queue */
    private LinkedBlockingQueue<Document> mQueue = null;

    /** number of consuming threads */
    private int mThreadNo = 0;

    private Map<Integer, Document> mDocuments;

    public static Indexer create (Map<Integer, Document> docs) {
        return new Indexer(docs);
    }

    private Indexer (Map<Integer, Document> docs) {
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
     * retrieve trem vectors for all previously indexed docs
     * parallel version
     */
    public void retrieveTermVectorsParallel (int threads) {
        try {
            mReader = DirectoryReader.open(mDir);
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < mReader.maxDoc(); i++) {
                Runnable worker = new DocTermVectorRetriever(mReader, i);
                executor.execute(worker);
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
        } catch (IOException e) {
            System.err.println("There was a problem with searching indexed Documents.");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * IndexReader http://bit.ly/1jXBg1F
     * retrieve trem vectors for all previously indexed docs
     */
    public void retrieveTermVectors() {
        try {
            mReader = DirectoryReader.open(mDir);
            for (int i = 0; i < mReader.maxDoc(); i++) {
                org.apache.lucene.document.Document doc = mReader.document(i);
                Terms terms = mReader.getTermVector(i, DEFAULT_SEARCH_FIELD);
                TermsEnum termsEnum = terms.iterator(null);
                BytesRef text;
                TermVector tfIdf = TermVector.create();
                while((text = termsEnum.next()) != null) {
                    String termString = text.utf8ToString();
                    Term termInstance  = new Term(DEFAULT_SEARCH_FIELD, termString);
                    long tf = termsEnum.totalTermFreq(); // <---- for this doc
                    long df = mReader.docFreq(termInstance); // <---- total doc freq
                    double tfIdfForTerm = tf * (mReader.getDocCount(DEFAULT_SEARCH_FIELD)/df);
                    tfIdf.addMember(tfIdfForTerm, termInstance);
                }
                mDocuments.get(Integer.parseInt(doc.get("newId"))).setTermVector(tfIdf);
            }
        } catch (IOException e) {
            System.err.println("There was a problem with searching indexed Documents.");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
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
     * run the indexing
     */
    public void index() {
        try {
            mWriter = new IndexWriter(mDir, mConfig);
            for (Integer docKey : mDocuments.keySet()) {
                Document doc = mDocuments.get(docKey);
                if (doc.getBody() == null) continue;
                try {
                    mWriter.addDocument(doc.getLuceneDocument());
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


    /**
     * runnable task for retrieving term vectors
     */
    private class DocTermVectorRetriever implements Runnable {

        private IndexReader mReader;
        private int mPosition;

        public DocTermVectorRetriever(IndexReader reader, int i) {
            mReader = reader;
            mPosition = i;
        }

        @Override
        public void run() {
            try {
                org.apache.lucene.document.Document doc = mReader.document(mPosition);
                Terms terms = mReader.getTermVector(mPosition, DEFAULT_SEARCH_FIELD);
                TermsEnum termsEnum = terms.iterator(null);
                BytesRef text;
                TermVector tfIdf = TermVector.create();
                while ((text = termsEnum.next()) != null) {
                    String termString = text.utf8ToString();
                    Term termInstance = new Term(DEFAULT_SEARCH_FIELD, termString);
                    long tf = termsEnum.totalTermFreq(); // <---- for this doc
                    long df = mReader.docFreq(termInstance); // <---- total doc freq
                    double tfIdfForTerm = tf * (mReader.getDocCount(DEFAULT_SEARCH_FIELD) / df);
                    tfIdf.addMember(tfIdfForTerm, termInstance);
                }
                mDocuments.get(Integer.parseInt(doc.get("newId"))).setTermVector(tfIdf);
            } catch (IOException e) {
                System.err.println("There was a problem with searching indexed Documents.");
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
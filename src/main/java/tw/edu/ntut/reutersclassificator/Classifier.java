package tw.edu.ntut.reutersclassificator;

import tw.edu.ntut.reutersclassificator.entity.*;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Classifier
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 30 17:02 2014
 */
public class Classifier {

    private static final int REF_MIN = 1000;
    private static final boolean DEBUG = false;

    /** dataset */
    private Map<Integer, Document> mTestDocuments;
    private Map<String, Category> mTopics;
    private Map<String, Category> mReferenceTopics;

    /** evaluation */
    private Map<String, Measure> mMeasureMap = new HashMap<String, Measure>();
    private Map<String, Double> mRefCatCs = new HashMap<String, Double>();
    private Map<String, Double> mAllCatCs = new HashMap<String, Double>();
    private TreeMap<String,Double> sortedAllCatCs;
    private DebugMapComparator bvc;
    private int mEvalCorrect = 0;
    private int mEvalInCorrect = 0;


    /**
     * factory method
     * @param dataSet dataset
     * @return classifier instance
     */
    public static Classifier create (DataSet dataSet) {
        Classifier classifier = new Classifier(dataSet.getTestDocuments(),
            dataSet.getTopics(), dataSet.getReferenceTopics());
        return classifier;
    }

    /**
     * constructor
     * @param testDocs
     * @param topics
     * @param refTopics
     */
    private Classifier (Map<Integer, Document> testDocs,
            Map<String, Category> topics,  Map<String, Category> refTopics) {
        mTestDocuments = testDocs;
        mTopics = topics;
        mReferenceTopics = refTopics;
        bvc = new DebugMapComparator(mAllCatCs);
        sortedAllCatCs= new TreeMap<String,Double>(bvc);
    }

    /**
     * classification
     */
    public void classify() {
        for (Integer docId: mTestDocuments.keySet()) {
            Document doc = mTestDocuments.get(docId);
            double min = REF_MIN;
            Category minCat = null;
            // calc cosine similarity (cs) for each category
            for (String key: mTopics.keySet()) {
                Category cat = mTopics.get(key);
                Double dotProduct = TermVector.dotProduct(
                        cat.getPrototypeTermVector(),doc.getTermVector());
                Double cs = Math.cos(dotProduct/(cat.getPrototypeTermVector().size()
                        * doc.getTermVector().size()));
                // find the lowest one
                if (cs < min) {
                    min = cs;
                    minCat = cat;
                }
                // assign debugging similarities
                if (DEBUG) {
                    mAllCatCs.put(cat.getName(), cs);
                    if (doc.getTopics().contains(cat.getName())) {
                        mRefCatCs.put(cat.getName(), cs);
                    }
                }
            }
            // sort allcat for debugging
            if (DEBUG) sortedAllCatCs.putAll(mAllCatCs);
            // assign to closest topic
            minCat.addTestDoc(doc);
            // test for proper assignment
            testAssignment(doc, minCat);

        }

    }

    /**
     * test whether doc was properly assigned to category
     * @param doc tested doc
     * @param minCat assigned category
     */
    private boolean testAssignment (Document doc, Category minCat) {
        boolean found = false;
        for (String topic: doc.getTopics()) {
            if (minCat.getName().equals(topic)) {
                found = true;
                break;
            }
        }
        if (!found) {
            ++mEvalInCorrect;
            return false;
        } else {
            ++mEvalCorrect;
            return true;
        }
    }

    /**
     * evaluate the classification
     * @param fullEvaluation print not-assigned/not-retrieved topics?
     */
    public void evaluate(boolean fullEvaluation) {
        System.out.println("Recall\tPrecision\tF-measure\tTopic");
        System.out.println("-------------------------------------------");
        // measure
        measure(fullEvaluation);
        System.out.println("===========================================");
        double accuracy = (100.0 * mEvalCorrect) / (mEvalInCorrect + mEvalCorrect);
        System.out.printf("Overall accuracy: %.2f%%\n", accuracy);
    }

    /**
     * measure the success
     * @param fullEvaluation print not-assigned/not-retrieved topics?
     */
    private void measure(boolean fullEvaluation) {
        for (String key: mTopics.keySet()) {
            List<Document> retrieved = mTopics.get(key).getDocs();
            List<Document> relevant = new ArrayList<Document>();
            if (mReferenceTopics.containsKey(key)) {
                Category refCat = mReferenceTopics.get(key);
                relevant = refCat.getDocs();
            }
            List<Document> relevantRetrieved = new ArrayList<Document>(retrieved);
            relevantRetrieved.removeAll(relevant);
            // precision
            double precision;
            if (retrieved.size() == 0) {
                precision = Float.NaN;
                if (!fullEvaluation) continue;
            } else {
                precision = (double) relevantRetrieved.size()/retrieved.size();
            }
            //recall
            double recall;
            if (relevant.size() == 0) {
                recall = Float.NaN;
                if (!fullEvaluation) continue;
            } else {
                recall = (double) relevantRetrieved.size() / relevant.size();
            }
            // f1 measure
            double f1 = 2.0 * ((precision * recall)/(precision + recall));
            DecimalFormat df = new DecimalFormat("00.00");
            System.out.println(df.format(recall) + "\t" + df.format(precision) +
                    "\t\t" + ((Double.isNaN(f1)) ? f1 + " " : df.format(f1)) + "\t\t> " + key);
        }
    }

}

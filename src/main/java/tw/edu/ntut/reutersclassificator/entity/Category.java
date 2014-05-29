package tw.edu.ntut.reutersclassificator.entity;

import org.apache.lucene.index.Term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Category
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 22 15:33 2014
 */
public class Category {

    private Map<Integer, TrainDocument> mTrainDocs = new HashMap<Integer, TrainDocument>();
    private List<Document> mDocs = new ArrayList<Document>();
    private TermVector mPrototypeTermVector = null; // centroid
    private String mName;

    public static Category create (String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }

    public void addTrainDoc (TrainDocument doc) {
        mTrainDocs.put(doc.getmNewId(), doc);
    }

    public void addTestDoc (Document doc) {
        mDocs.add(doc);
    }

    public void setDocumentTermVector (int docId, TermVector termVector) {
        mTrainDocs.get(docId).setTermVector(termVector);
    }

    /**
     * ugh! spam spam spam
     * calculates centroid for this category
     * ref at http://stanford.io/1ntGfLr
     * @return
     */
    public TermVector calcCentroid () {
        if (mPrototypeTermVector == null) {
            // build refmap
            Map<Term, List<Double>> refMap = new HashMap<Term, List<Double>>();
            for (Map.Entry<Integer, TrainDocument> docEntry: mTrainDocs.entrySet()) {
                TrainDocument doc = docEntry.getValue();
                for (Term t: doc.getTermVector().x().keySet()) {
                    if (!refMap.containsKey(t)) {
                        List<Double> occurenceList = new ArrayList<Double>();
                        occurenceList.add(doc.getTermVector().x().get(t));
                        refMap.put(t, occurenceList);
                    } else {
                        refMap.get(t).add(doc.getTermVector().x().get(t));
                    }
                }
            }
            double n = mTrainDocs.size();
            // build centroid
            mPrototypeTermVector = TermVector.create();
            for (Map.Entry<Term, List<Double>> refMapEntry: refMap.entrySet()) {
                Term term = refMapEntry.getKey();
                List<Double> occurenceList = refMapEntry.getValue();
                double centroidXiValueSum = 0.0;
                for (Double occ: occurenceList) {
                    centroidXiValueSum += occ;
                }
                double centroidXiValue = centroidXiValueSum/n;
                mPrototypeTermVector.addMember(centroidXiValue, term);
            }
        }
        return mPrototypeTermVector;
    }

    private Category () {

    }

    public Map<Integer, TrainDocument> getTrainDocs() {
        return mTrainDocs;
    }

    public void setTrainDocs(Map<Integer, TrainDocument> mTrainDocs) {
        this.mTrainDocs = mTrainDocs;
    }

    public TermVector getPrototypeTermVector() {
        return mPrototypeTermVector;
    }

    public void setPrototypeTermVector(TermVector mPrototypeTermVector) {
        this.mPrototypeTermVector = mPrototypeTermVector;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public List<Document> getDocs() {
        return mDocs;
    }



}

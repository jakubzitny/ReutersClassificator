package tw.edu.ntut.reutersclassificator.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Category
 *
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
     * ref at http://stanford.io/1ntGfLr
     * @return
     */
    public TermVector calcCentroid () {
        if (mPrototypeTermVector == null) {
            double xSum = 0;
            double ySum = 0;
            for (Map.Entry<Integer, TrainDocument> docEntry: mTrainDocs.entrySet()) {
                TrainDocument doc = docEntry.getValue();
                xSum += doc.getTermVector().x();
                ySum += doc.getTermVector().y();
            }
            double n = mTrainDocs.size();
            mPrototypeTermVector = TermVector.create(xSum/n, ySum/n);
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

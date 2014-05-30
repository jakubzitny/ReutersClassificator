package tw.edu.ntut.reutersclassificator.entity;

import java.util.Map;

/**
 * DataSet
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 30 17:43 2014
 */
public class DataSet {

    private Map<Integer, Document> mTestDocuments;
    private Map<String, Category> mTopics;
    private Map<String, Category> mReferenceTopics;

    /**
     * factory method
     * @param docs test docs
     * @param topics topics
     * @param refTopics reference topics
     * @return dataset instance
     */
    public static DataSet create(Map<Integer, Document> docs, Map<String, Category> topics,
        Map<String, Category> refTopics) {
        DataSet dataSet = new DataSet();
        dataSet.setTestDocuments(docs);
        dataSet.setTopics(topics);
        dataSet.setReferenceTopics(refTopics);
        return dataSet;
    }

    /**
     * make constructor private
     */
    private DataSet () { }

    public Map<Integer, Document> getTestDocuments() {
        return mTestDocuments;
    }

    public void setTestDocuments(Map<Integer, Document> mTestDocuments) {
        this.mTestDocuments = mTestDocuments;
    }

    public Map<String, Category> getTopics() {
        return mTopics;
    }

    public void setTopics(Map<String, Category> mTopics) {
        this.mTopics = mTopics;
    }

    public Map<String, Category> getReferenceTopics() {
        return mReferenceTopics;
    }

    public void setReferenceTopics(Map<String, Category> mReferenceTopics) {
        this.mReferenceTopics = mReferenceTopics;
    }
}

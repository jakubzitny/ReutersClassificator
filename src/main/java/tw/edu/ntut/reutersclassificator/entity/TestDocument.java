package tw.edu.ntut.reutersclassificator.entity;

import tw.edu.ntut.reutersclassificator.entity.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * TrainDocument
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 14:20 2014
 */
public class TestDocument extends Document {

    private static final String SPLIT = "TEST";

    public static String getSplit() {
        return SPLIT;
    }

    private List<String> mClassifiedTopics = new ArrayList<String>();

    public TestDocument (int oldId, int newId) {
        super(oldId, newId);
    }

    public static TestDocument create(int oldId, int newId) {
        return new TestDocument(oldId, newId);
    }

    public static TestDocument create(Document doc) {
        return (TestDocument) doc;
    }

    public List<String> getClassifiedTopics() {
        return mClassifiedTopics;
    }

    public void setClassifiedTopics(List<String> mClassifiedTopics) {
        this.mClassifiedTopics = mClassifiedTopics;
    }

    public void addClassifiedTopic (String topic) {
        mClassifiedTopics.add(topic);
    }

}

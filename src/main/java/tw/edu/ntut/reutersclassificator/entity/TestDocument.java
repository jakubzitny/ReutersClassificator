package tw.edu.ntut.reutersclassificator.entity;

import tw.edu.ntut.reutersclassificator.entity.Document;

/**
 * TrainDocument
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 14:20 2014
 */
public class TestDocument extends Document {

    private static final String SPLIT = "TEST";

    public TestDocument (int oldId, int newId) {
        super(oldId, newId);
    }

    public static TestDocument create(int oldId, int newId) {
        return new TestDocument(oldId, newId);
    }

    public static TestDocument create(Document doc) {
        return (TestDocument) doc;
    }
}

package tw.edu.ntut.reutersclassificator.entity;

import tw.edu.ntut.reutersclassificator.entity.Document;

/**
 * TrainDocument
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 14:20 2014
 */
public class TrainDocument extends Document {

    private static final String SPLIT = "TRAIN";

    public TrainDocument (int oldId, int newId) {
        super(oldId, newId);
    }

    public static TrainDocument create (int oldId, int newId) {
        return new TrainDocument(oldId, newId);
    }

    public static TrainDocument create (Document doc) {
        return (TrainDocument) doc;
    }

}

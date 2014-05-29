package tw.edu.ntut.reutersclassificator.entity;

/**
 * Measure
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 30 05:24 2014
 */
public class Measure {

    private String mTopicName;
    private int relevant = 0;
    private int retrieved = 0;

    public Measure (String topicName) {
        mTopicName = topicName;
    }

    public void addRelevant() {
        relevant++;
    }

    public void addRetrieved() {
        retrieved++;
    }

}

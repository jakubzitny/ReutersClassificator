package tw.edu.ntut.reutersclassificator.entity;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.util.List;

/**
 * Document
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 14:14 2014
 */
public class Document extends Object {

    protected String mTitle;
    protected String mBody;
    protected List<String> mTopics;
    protected int mNewId;
    protected int mOldId;
    protected static final String SPLIT = "";
    public static final String SPLIT_TEST = "TEST";
    public static final String SPLIT_TRAIN = "TRAIN";

    protected boolean mIsTerminator;

    public String getLewisSplit() {
        return SPLIT;
    }

    public Document () {
        mIsTerminator = false;
    }

    public Document (int oldId, int newId) {
        mIsTerminator = false;
        mOldId = oldId;
        mNewId = newId;
    }

    public Document (boolean isTerminator) {
        mIsTerminator = isTerminator;
    }

    public String getTitle() {
        if (mTitle == null) {
            return "(none)";
        }
        return mTitle;
    }

    /**
     * convert document to lucene indexable document
     * ref http://bit.ly/1jBtCzL and http://bit.ly/SgIAQc
     * TODO: add title
     * @return
     */
    public org.apache.lucene.document.Document getLuceneDocument () {
        org.apache.lucene.document.Document luceneDocument = new org.apache.lucene.document.Document();
        luceneDocument.add(new TextField("body", mBody, Field.Store.YES));
        for (String topic: mTopics) {
            luceneDocument.add(new StringField("topic", topic, Field.Store.YES));
        }
        return luceneDocument;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getBody() {
        return mBody;
    }

    public void setBody(String mBody) {
        this.mBody = mBody;
    }

    public List<String> getTopics() {
        return mTopics;
    }

    public void setTopics(List<String> mTopics) {
        this.mTopics = mTopics;
    }

    public boolean isTerminator() {
        return mIsTerminator;
    }

    public int getmNewId() {
        return mNewId;
    }

    public void setmNewId(int mNewId) {
        this.mNewId = mNewId;
    }

    public int getmOldId() {
        return mOldId;
    }

    public void setmOldId(int mOldId) {
        this.mOldId = mOldId;
    }
}

package tw.edu.ntut.reutersclassificator.entity;

import org.apache.lucene.document.*;
import org.apache.lucene.index.FieldInfo;

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
    protected TermVector mTermVector;

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
        mBody = "";
        mTitle = "";
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
     * convert document to indexable lucene document
     * ref http://bit.ly/1jBtCzL and http://bit.ly/SgIAQc
     * fieldtype ref http://bit.ly/1nuaYYR
     * field ref http://bit.ly/1ja51fm
     * @return lucene document representation of this Document
     */
    public org.apache.lucene.document.Document getLuceneDocument () {
        org.apache.lucene.document.Document luceneDocument = new org.apache.lucene.document.Document();
        luceneDocument.add(new Field("body", mBody, prepareFieldType()));
        luceneDocument.add(new TextField("title", mTitle, Field.Store.YES));
        luceneDocument.add(new StringField("lewisSplit", SPLIT, Field.Store.YES));
        luceneDocument.add(new IntField("newId", mNewId, Field.Store.YES));
        luceneDocument.add(new IntField("oldId", mOldId, Field.Store.YES));
        for (String topic: mTopics) {
            luceneDocument.add(new StringField("topic", topic, Field.Store.YES));
        }
        return luceneDocument;
    }

    /**
     * prepare field type that stores TFIDF weighs
     * @return prepared fieldtype
     */
    public FieldType prepareFieldType () {
        FieldType fieldType = new FieldType();
        fieldType.setIndexed(true);
        fieldType.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        fieldType.setTokenized(true);
        fieldType.setStored(true);
        fieldType.setStoreTermVectors(true);
        fieldType.setStoreTermVectorPositions(true);
        fieldType.setStoreTermVectorOffsets(true);
        fieldType.setStoreTermVectorPayloads(true);
        fieldType.freeze();
        return fieldType;
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

    public TermVector getTermVector() {
        return mTermVector;
    }

    public void setTermVector(TermVector mTermVector) {
        this.mTermVector = mTermVector;
    }
}

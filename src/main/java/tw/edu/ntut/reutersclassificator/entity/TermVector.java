package tw.edu.ntut.reutersclassificator.entity;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TermVector
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 22 15:42 2014
 */
public class TermVector {

//    private double x;
//    private double y;

    private Map<Term, Double> x = new HashMap<Term, Double>();

    public static TermVector create() {
        return new TermVector();
    }

    /**
     * TODO faster?
     * @param a b
     * @param b a
     * @return dot product
     */
    public static Double dotProduct(TermVector a, TermVector b) {
        // build refmap
        Double dotProduct = 0.0;
        for (Term t: b.x().keySet()) {
            if (!a.x().containsKey(t)) {
                continue;
            } else {
                dotProduct += a.x().get(t) * b.x().get(t);
            }
        }
        return dotProduct;
    }

    /**
     * calculate size for this vector
     * used in cosine similarity
     * @return double size
     */
    public double size () {
        Double sum = 0.0;
        for (Double xi: x.values()) {
            sum += xi * xi;
        }
        return Math.sqrt(sum);
    }

    private TermVector () {

    }

    public void addMember (double mem, Term term) {
        x.put(term, mem);
    }

    public Map<Term, Double> x() {
        return x;
    }

}

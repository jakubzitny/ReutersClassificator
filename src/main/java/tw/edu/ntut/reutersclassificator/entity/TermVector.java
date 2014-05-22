package tw.edu.ntut.reutersclassificator.entity;

import org.apache.lucene.index.Terms;

/**
 * TermVector
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 22 15:42 2014
 */
public class TermVector {

    private double x;
    private double y;

    public static TermVector create(double x, double y) {
        TermVector tv = new TermVector();
        tv.setX(x);
        tv.setY(y);
        return tv;
    }

    public double size () {
        return Math.sqrt(x * x + y * y);
    }

    private TermVector () {

    }

    public double x() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double y() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

}

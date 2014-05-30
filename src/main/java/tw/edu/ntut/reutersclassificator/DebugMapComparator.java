package tw.edu.ntut.reutersclassificator;

import java.util.Comparator;
import java.util.Map;

/**
 * DebugMapComparator
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 30 16:57 2014
 */
public class DebugMapComparator implements Comparator<String> {

    Map<String, Double> base;
    public DebugMapComparator(Map<String, Double> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}
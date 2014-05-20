package tw.edu.ntut.reutersclassificator.exception;

/**
 * UnexpectedEOFException
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 17:27 2014
 */
public class UnexpectedEOFException extends Exception {
    public UnexpectedEOFException() {
        super("Unexpected end of file in the middle of a document.");
    }
}

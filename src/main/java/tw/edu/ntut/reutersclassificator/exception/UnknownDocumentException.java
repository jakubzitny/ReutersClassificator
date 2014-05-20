package tw.edu.ntut.reutersclassificator.exception;

/**
 * UnknownDocumentException
 * TODO: maybe factory method?, maybe add throwable?
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 16:51 2014
 */
public class UnknownDocumentException extends Exception {

    public UnknownDocumentException(String lewisSplitType) {
        super("Document of unknown LEWISSPLIT type found ("
                + lewisSplitType + "). Skipping.");
    }

}

package tw.edu.ntut.reutersclassificator.exception;

import java.io.File;

/**
 * NoSgmlFilesException
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 18:31 2014
 */
public class NoSgmlFilesException extends Exception {
    public NoSgmlFilesException (File file) {
        super(file.toString() + " contains no sgm files.");
    }
}

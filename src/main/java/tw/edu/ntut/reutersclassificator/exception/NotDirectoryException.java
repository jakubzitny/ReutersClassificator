package tw.edu.ntut.reutersclassificator.exception;

import java.io.File;

/**
 * NotDirectoryException
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 18:15 2014
 */
public class NotDirectoryException extends Exception {
    public NotDirectoryException (File file) {
        super(file.toString() + " is not a directory.");
    }
}

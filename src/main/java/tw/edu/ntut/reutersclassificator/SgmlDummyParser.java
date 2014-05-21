package tw.edu.ntut.reutersclassificator;

import tw.edu.ntut.reutersclassificator.entity.Document;
import tw.edu.ntut.reutersclassificator.entity.TestDocument;
import tw.edu.ntut.reutersclassificator.entity.TrainDocument;
import tw.edu.ntut.reutersclassificator.exception.UnexpectedEOFException;
import tw.edu.ntut.reutersclassificator.exception.UnknownDocumentException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SgmlDummyParser
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 15:57 2014
 */
public class SgmlDummyParser implements Runnable {

    private static final String sgmlLewisHead = "<!DOCTYPE lewis SYSTEM \"lewis.dtd\">";

    private static final String REUTERS_OPEN = "<REUTERS.*LEWISSPLIT=\"(.*?)\".*>";
    private static final String REUTERS_CLOSE = "</REUTERS>";
    private static final String TOPICS_LINE = "<TOPICS>.*</TOPICS>";
    private static final String TEXT_LINE_UNPROC = ".*<TEXT TYPE=\"(.*?)\">.*";
    private static final String TEXT_LINE_OPEN = ".*<TEXT>.*";
    private static final String TEXT_LINE_CLOSE = ".*</TEXT>.*";
    private static final String TITLE_OPEN = ".*<TITLE>.*";
    private static final String TITLE_CLOSE = ".*</TITLE>.*";
    private static final String TITLE_LINE = "<TITLE>.*</TITLE>";
    private static final String BODY_OPEN = ".*<BODY>.*";
    private static final String BODY_CLOSE = ".*</BODY>.*";

    private final Pattern mPatternReutersOpen;
    private final Pattern mPatternReutersClose;
    private final Pattern mPatternTopicsLine;
    private final Pattern mPatternTitleLine;
    private final Pattern mPatternTitleOpen;
    private final Pattern mPatternTitleClose;
    private final Pattern mPatternBodyOpen;
    private final Pattern mPatternBodyClose;
    private final Pattern mPatternTextOpen;
    private final Pattern mPatternTextLineUnproc;
    private final Pattern mPatternTextClose;

    private List<File> mFiles;
    private LinkedBlockingQueue<Document> mQueue;
    private final int mThreadNo;
    private int mNo = 0;

    /**
     * factory method
     * @param files
     * @param queue
     * @return
     */
    public static SgmlDummyParser create(List<File> files, LinkedBlockingQueue<Document> queue, int threadNo) {
        return new SgmlDummyParser(files, queue, threadNo);
    }

    /**
     * private constructor
     * @param files
     * @param queue
     */
    private SgmlDummyParser(List<File> files, LinkedBlockingQueue<Document> queue, int threadNo) {
        // prepare final patterns
        mPatternReutersOpen = Pattern.compile(REUTERS_OPEN);
        mPatternReutersClose = Pattern.compile(REUTERS_CLOSE);
        mPatternTopicsLine = Pattern.compile(TOPICS_LINE);
        mPatternTitleLine = Pattern.compile(TITLE_LINE);
        mPatternTitleOpen = Pattern.compile(TITLE_OPEN);
        mPatternTitleClose = Pattern.compile(TITLE_CLOSE);
        mPatternBodyOpen = Pattern.compile(BODY_OPEN);
        mPatternBodyClose = Pattern.compile(BODY_CLOSE);
        mPatternTextOpen = Pattern.compile(TEXT_LINE_OPEN);
        mPatternTextClose = Pattern.compile(TEXT_LINE_CLOSE);
        mPatternTextLineUnproc = Pattern.compile(TEXT_LINE_UNPROC);
        // prepare vars
        mFiles = files;
        mQueue = queue;
        mThreadNo = threadNo;
    }

    /**
     * runs this thread
     * initiates the parsing of given list of files
     */
    @Override
    public void run() {
        parseFiles();
    }

    /**
     * parses the list of files
     * catches all the exceptions during parsing phase
     * terminates the queue
     * TODO: threads for each file
     */
    private void parseFiles () {
        try {
            for (File file: mFiles) {
                try {
                    parseFile(file);
                } catch (UnknownDocumentException e) {
                    // skip the file (thank you gc)
                    System.err.println(e.getMessage());
                    // continue;
                } catch (FileNotFoundException e) {
                    // skip the file maybe there are others
                    System.err.println("File not found. Skipping.");
                    System.err.println(e.getMessage());
                    // continue;
                }
            }
            // send terminate "signal" to the queue
            for (int i = 0; i < mThreadNo + 1; i++) {
                mQueue.put(new Document(true));
            }
        } catch (IOException e) {
            // TODO: think
            System.err.println("Parsing fail..");
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("Thread communication fail.");
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (UnexpectedEOFException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    /**
     * parses single sgml file
     * saves the entity to queue
     * @param file
     * @throws IOException
     * @throws UnknownDocumentException
     * @throws InterruptedException
     * @throws UnexpectedEOFException
     */
    private void parseFile (File file) throws IOException, UnknownDocumentException,
            InterruptedException, UnexpectedEOFException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String currentLine;
        while ((currentLine = br.readLine()) != null) {
            if (currentLine.equals(sgmlLewisHead)) {
                // skip the stupid sgml header
                continue;
            } else if (currentLine.matches(mPatternReutersOpen.toString())) {
                // process attrs and parse child elements separately
                Matcher matcher = mPatternReutersOpen.matcher(currentLine);
                Document currentDocument = new Document();
                if (matcher.find()) {
                    String lewisSplit = matcher.group(1);
                    if (lewisSplit.equals(Document.SPLIT_TEST)) {
                        currentDocument = parseDocument(br, TestDocument.create());
                    } else if (lewisSplit.equals(Document.SPLIT_TRAIN)) {
                        currentDocument = parseDocument(br, TrainDocument.create());
                    } else {
                        throw new UnknownDocumentException(lewisSplit);
                    }
                }
//                int length = (currentDocument.getBody() == null) ? 0 : currentDocument.getBody().length();
//                System.out.println("processed doc " + currentDocument.getTitle() + " (" +
//                        length + ")");
//                for (String topic: currentDocument.getTopics()) {
//                    System.out.print(topic+ ", ");
//                }
//                System.out.println();
//                write the document to the queue for consumers
                  mQueue.put(currentDocument);
            }
        }
    }

    /**
     * parses the insides of single Reuters document
     * TODO: elements
     * @param br buffered reader from inside the file
     * @param doc entity to fill the data into
     * @return
     * @throws IOException
     * @throws UnexpectedEOFException
     */
    private Document parseDocument (BufferedReader br, Document doc)
            throws IOException, UnexpectedEOFException {
        while (true) {
            String currentLine = br.readLine();
            if (currentLine == null){
                throw new UnexpectedEOFException();
            } else if (currentLine.matches(mPatternTopicsLine.toString())) {
                doc.setTopics(parseTopics(currentLine));
                continue;
            } else if (currentLine.matches(mPatternTitleOpen.toString())) {
                doc.setTitle(parseElement(br, currentLine, "TITLE"));
                continue;
            } else if (currentLine.matches(mPatternBodyOpen.toString())) {
                doc.setBody(parseElement(br, currentLine, "BODY"));
            } else if (currentLine.matches(mPatternTextLineUnproc.toString())) {
                // TODO: finish this thing with titles
                doc.setBody(parseElement(br, currentLine, "TEXT"));
            } else if (currentLine.matches(mPatternTextOpen.toString())) {
                doc.setBody(parseElement(br, currentLine, "TEXT"));
            } else if (currentLine.matches(mPatternReutersClose.toString())) {
                break;
            } else {
                // ignore all other child elements
            }
        }
        return doc;
    }

    /**
     * parses topics element and its insides
     * @param topicsLine
     * @return
     * @throws IOException
     */
    private List<String> parseTopics (String topicsLine) throws IOException {
        String refinedTopicsLine = topicsLine.replace("<TOPICS><D>", "").replace("</D></TOPICS>", "")
                .replace("</D><D>", "~").replace("<TOPICS>", "").replace("</TOPICS>","");
        return Arrays.asList(refinedTopicsLine.split("~"));
    }

    /**
     * parses general element with string content
     * @param br buffered reader from inside the parent element
     * @param startLine first line where the open tag was found
     * @param element what kind of element is this?
     * @return string body of the doc
     * @throws IOException
     * @throws UnexpectedEOFException
     */
    private String parseElement(BufferedReader br, String startLine, String element)
            throws IOException, UnexpectedEOFException {
        String elementOpen = "<" + element + ">";
        String elementClose = "</" + element + ">";
        if (startLine.matches(".*"+elementOpen+".*") && startLine.matches(".*"+elementClose+".*")) {
            // TODO: use regex
            return startLine.replace(elementOpen, "").replace(elementClose,"");
        }
        String elementPayload = startLine.substring(startLine.indexOf(elementOpen) + elementOpen.length());
        while (true) {
            String currentLine = br.readLine();
            if (currentLine == null) {
                throw new UnexpectedEOFException();
            } else if (currentLine.matches(".*"+elementClose+".*")) {
                elementPayload += startLine.substring(0, startLine.indexOf(elementClose) + elementClose.length());
                break;
            } else {
                elementPayload += "\n" + currentLine;
            }
        }
        return elementPayload;
    }

}

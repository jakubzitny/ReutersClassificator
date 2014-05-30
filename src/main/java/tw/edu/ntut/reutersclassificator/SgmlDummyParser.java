package tw.edu.ntut.reutersclassificator;

import tw.edu.ntut.reutersclassificator.entity.*;
import tw.edu.ntut.reutersclassificator.exception.UnexpectedEOFException;
import tw.edu.ntut.reutersclassificator.exception.UnknownDocumentException;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SgmlDummyParser
 *
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 20 15:57 2014
 */
public class SgmlDummyParser {

    private static final String SGML_LEWIS_HEAD = "<!DOCTYPE lewis SYSTEM \"lewis.dtd\">";
    private static final String REUTERS_OPEN =
            "<REUTERS.*LEWISSPLIT=\"(.*?)\".*OLDID=\"(.*?)\".*NEWID=\"(.*?)\".*>";
    private static final String REUTERS_CLOSE = "</REUTERS>";
    private static final String TOPICS_LINE = "<TOPICS>.*</TOPICS>";
    private static final String TEXT_LINE_UNPROC = ".*<TEXT TYPE=\"(.*?)\">.*";
    private static final String TEXT_LINE_OPEN = ".*<TEXT.*?>.*";
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

    private Map<String, Category> mTopics = new HashMap<String, Category>();
    private Map<Integer, Document> mDocuments = new HashMap<Integer, Document>();
    private Map<Integer, Document> mTestDocuments = new HashMap<Integer, Document>();
    private Map<String, Category> mRefTopics = new HashMap<String, Category>();

    /**
     * factory method simple (sequentialň
     * @param files
     * @return
     */
    public static SgmlDummyParser create(List<File> files) {
        return new SgmlDummyParser(files);
    }

    /**
     * private constructor
     * @param files
     */
    private SgmlDummyParser(List<File> files) {
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
    }

    /**
     * parses the list of files
     * catches all the exceptions during parsing phase
     * terminates the queue
     * TODO: threads for each file
     */
    public DataSet parseFiles () {
        try {
            for (File file: mFiles) {
                try {
                    parseFile(file);
                } catch (UnknownDocumentException e) {
                    // skip the file (thank you gc)
                    System.err.println(e.getMessage());
                } catch (FileNotFoundException e) {
                    // skip the file maybe there are others
                    System.err.println("File not found. Skipping.");
                }
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
        return DataSet.create(mTestDocuments, mTopics, mRefTopics);
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
            if (currentLine.equals(SGML_LEWIS_HEAD)) {
                // skip the stupid sgml header
                continue;
            } else if (currentLine.matches(mPatternReutersOpen.toString())) {
                // process attrs and parse child elements separately
                Matcher matcher = mPatternReutersOpen.matcher(currentLine);
                Document currentDocument = new Document();
                if (matcher.find()) {
                    String lewisSplit = matcher.group(1);
                    int oldId = Integer.parseInt(matcher.group(2));
                    int newId = Integer.parseInt(matcher.group(3));
                    if (lewisSplit.equals(Document.SPLIT_TEST)) {
                        currentDocument = parseDocument(br, TestDocument.create(oldId, newId));
                    } else if (lewisSplit.equals(Document.SPLIT_TRAIN)) {
                        currentDocument = parseDocument(br, TrainDocument.create(oldId, newId));
                    } else {
                        // found document that is not test nor train
                        // move br to the end of this doc
                        while (!br.readLine().matches(mPatternReutersClose.toString()));
                        continue;
                    }
                }
                if (currentDocument.getBody().equals("")) {
                    // ignore empty bodies
                    continue;
                }
                mDocuments.put(currentDocument.getmNewId(), currentDocument);
                if (currentDocument instanceof TestDocument) {
                    // ignore docs with empty topics
                    boolean hasTopics = false;
                    for (String topic: currentDocument.getTopics()) {
                        if (!topic.equals("")){
                            hasTopics = true;
                            break;
                        }
                    }
                    if (currentDocument.getTopics().size() != 0 && hasTopics) {
                        mTestDocuments.put(currentDocument.getmNewId(), currentDocument);
                        // build ref topic set
                        for (String topicName: currentDocument.getTopics()) {
                            if (!mRefTopics.containsKey(topicName)) {
                                mRefTopics.put(topicName, Category.create(topicName));
                            }
                            mRefTopics.get(topicName).addTestDoc(currentDocument);
                        }
                    }
                } else {
                    for (String topicName: currentDocument.getTopics()) {
                        if (topicName.equals("")) {
                            // ignore docs with empty topics ("")
                            continue;
                        }
                        if (mTopics.containsKey(topicName)) {
                            // TODO: careful here with the cast
                            if (currentDocument instanceof TestDocument) {
                                continue;
                            }
                            mTopics.get(topicName).addTrainDoc((TrainDocument) currentDocument);
                        } else {
                            mTopics.put(topicName, Category.create(topicName));
                            mTopics.get(topicName).addTrainDoc((TrainDocument) currentDocument);
                        }
                    }
                }
            }
        }
    }

    /**
     * parses the insides of single Reuters document
     * TODO: element entities
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
            }  else if (currentLine.matches(mPatternTextOpen.toString())) {
                parseText(doc, br, currentLine);
            } else if (currentLine.matches(mPatternReutersClose.toString())) {
                break;
            } else {
                // ignore all other child elements
            }
        }
        return doc;
    }

    /**
     *
     * @param currentDocument
     * @param br buffered reader from inside the parent element
     * @param startLine first line where the open tag was found
     * @throws IOException
     * @throws UnexpectedEOFException
     */
    private void parseText (Document currentDocument, BufferedReader br, String startLine)
            throws IOException, UnexpectedEOFException {
        if (startLine.matches(mPatternTextClose.toString())) {
            return;
        }
        while (true) {
            String currentLine = br.readLine();
            if (currentLine == null) {
                throw new UnexpectedEOFException();
            } else if (currentLine.matches(mPatternTitleOpen.toString())) {
                currentDocument.setTitle(parseElement(br, currentLine, "TITLE"));
                continue;
            } else if (currentLine.matches(mPatternBodyOpen.toString())) {
                currentDocument.setBody(parseElement(br, currentLine, "BODY"));
                break;
            } else if (currentLine.matches(mPatternTextClose.toString())) {
                break;
            } else {
                // skip other possible body elements
            }
        }
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
                if (startLine.indexOf(elementClose) > 0) {
                    elementPayload += startLine.substring(0, startLine.indexOf(elementClose));
                }
                break;
            } else {
                elementPayload += "\n" + currentLine;
            }
        }
        return elementPayload;
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

    public Map<Integer, Document> getDocuments() {
        return mDocuments;
    }

    public Map<Integer, Document> getTestDocuments() {
        return mTestDocuments;
    }

    public Map<String, Category> getTopics() {
        return mTopics;
    }
    public Map<String, Category> getRefTopics() {
        return mRefTopics;
    }
}

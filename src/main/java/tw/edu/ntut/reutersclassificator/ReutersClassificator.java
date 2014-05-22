package tw.edu.ntut.reutersclassificator;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.filefilter.RegexFileFilter;
import tw.edu.ntut.reutersclassificator.entity.Category;
import tw.edu.ntut.reutersclassificator.entity.Document;
import tw.edu.ntut.reutersclassificator.entity.TermVector;
import tw.edu.ntut.reutersclassificator.entity.TestDocument;
import tw.edu.ntut.reutersclassificator.exception.NoSgmlFilesException;
import tw.edu.ntut.reutersclassificator.exception.NotDirectoryException;

/**
 * ReutersClassificator main class
 * @author Jakub Zitny <t102012001@ntut.edu.tw>
 * @since May 19 17:39 2014
 */
public class ReutersClassificator {

    /** CLI parser, options, help */
    private static CommandLineParser parser = new GnuParser();
    private static HelpFormatter formatter = new HelpFormatter();
    private static Options options = new Options();
    private static Option[] option_array = new Option[] {
            new Option("h", "help", false, "displays this help message"),
            new Option("d", "docfile", true, "document collection file"),
            new Option("t", "threads", true, "(optional) number of threas"),
    };

    /**
     * dispatches the main stuff
     * @param docFiles path to file with reuters docs
     * @param threadNo number of threads
     */
    public static void runParallel(List<File> docFiles, int threadNo) throws InterruptedException {
        // prepare queue and threads
        System.out.println("Configuring tools.");
        LinkedBlockingQueue<Document> sharedQueue = new LinkedBlockingQueue<Document>(512);
        SgmlDummyParser parser = SgmlDummyParser.create(docFiles, sharedQueue, threadNo);
        Indexer classificator = new Indexer(sharedQueue, threadNo);
        Thread produce = new Thread(parser);
        Thread consume = new Thread(classificator);
        System.out.println("produce start");
        produce.start();
//        System.out.println("consume start");
//        consume.start();
//        System.out.println("produce join");
        produce.join();
//        System.out.println("consume join");
//        consume.join();
//        List<Double> res = classificator.search("cocoa");
//        System.out.println("Found: " + res.size());
//        for (Double r: res) {
//            System.out.println(r.floatValue());
//        }

    }

    public static void runSequential(List<File> docFiles) throws InterruptedException {
        System.out.println("Parsing documents.");
        SgmlDummyParser parser = SgmlDummyParser.create(docFiles);
        parser.parseFiles();
        // index docs
        Indexer indexer = new Indexer(parser.getDocuments());
        System.out.println("Indexing.");
        indexer.index();
        // calc each doc tv
        System.out.println("Assigning vector values.");
        indexer.retrieveTermVectors(); // TODO: check if they are there
        // cals centroids for each cat
        System.out.println("Calculating centroids.");
        Collection<Category> categories = parser.getTopics().values();
        for (Category c: categories) {
            c.calcCentroid();
        }
        // assign test docs
        System.out.println("Testing.");
        for (Integer docId: parser.getTestDocuments().keySet()) {
            Document doc = parser.getTestDocuments().get(docId);
            Map<String, Double> cosineSimilarities = new HashMap<String, Double>();
            double min = 1000;
            Category minCat = null;
            for (String key: parser.getTopics().keySet()) {
                Category cat = parser.getTopics().get(key);
                TermVector a = cat.getPrototypeTermVector();
                TermVector b = doc.getTermVector();
                Double dotProduct = a.x() * b.x() /*+ a.x() * b.y() + a.y() * b.x()*/ + a.y() * b.y();
                Double cs = Math.acos(dotProduct/(a.size() * b.size()));
                if (cs < min) {
                    min = cs;
                    minCat = cat;
                }
                cosineSimilarities.put(cat.getName(), cs);
            }
            // assign
            try {
                minCat.addTestDoc(doc);
            } catch (NullPointerException e) {
                System.out.println("e");
            }
        }
        for (Category c: categories) {
            if (c.getTrainDocs().size() > 0) {
                System.out.println(c.getName() + "," + (c.getTrainDocs().size() + c.getDocs().size()));
            }
        }
        System.out.println("Done.");

    }

    /**
     * main
     * parses the command line arguments
     * if run in interctive mode then asks for query and archive and runs search
     * if run with proper arguments then just runs search
     * otherwise displays help message
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // prepares options (stupid library)
        for (Option o: option_array) {
            options.addOption(o);
        }
        // parses the options
        try {
            CommandLine cli = parser.parse(options, args);
            if (cli.hasOption("d")) {
                String workingDir = cli.getOptionValue("d");
                int threads = determineNumberOfThreads(cli.getOptionValue("t", "0"));
                runSequential(inspectWorkingDir(new File(workingDir)));
            } else {
                formatter.printHelp("reutersclassificator", options);
                System.exit(1);
            }
        } catch (ParseException e) {
            System.err.println("There was a problem with parsing arguments.");
            e.getStackTrace();
        } catch (InterruptedException e) {
            System.err.println("There was a problem with threads.");
            e.getStackTrace();
        } catch (NotDirectoryException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (NoSgmlFilesException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     *
     * @param workingDir
     * @return
     */
    public static List<File> inspectWorkingDir (File workingDir)
            throws NotDirectoryException, NoSgmlFilesException {
        if (workingDir.listFiles() == null) {
            throw new NotDirectoryException(workingDir);
        }
        FileFilter filter = new RegexFileFilter(".*\\.sgm");
        File[] files = workingDir.listFiles(filter);
        if (files == null || files.length == 0) {
            throw new NoSgmlFilesException(workingDir);
        }
        return new ArrayList<File>(Arrays.asList(files));
    }

    /**
     * determines the number of consument threads to use for indexing
     * is based on cli argument, but makes adjustments
     * if no or wrong option or "0" from -i is specified - set to number of logical cores
     * limit to no more than 4 times number of logical cores
     * @param threadsNoStr string option -t parsed from command line
     * @return number of threads integer
     */
    public static int determineNumberOfThreads(String threadsNoStr) {
        int threadNo;
        int cores = Runtime.getRuntime().availableProcessors();
        try {
            threadNo = Integer.parseInt(threadsNoStr);
            if (threadNo < 1) {
                throw new NumberFormatException();
            } else if (threadNo > cores * 4) {
                threadNo = cores * 4;
                System.out.println("Adjusting number of threads to " + threadNo + ".");
            }
        } catch (NumberFormatException e) {
            threadNo = cores;
        }
        return threadNo;
    }

}

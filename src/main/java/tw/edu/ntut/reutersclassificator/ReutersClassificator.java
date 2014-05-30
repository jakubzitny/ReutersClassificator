package tw.edu.ntut.reutersclassificator;

import java.io.*;
import java.text.DecimalFormat;
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
import tw.edu.ntut.reutersclassificator.entity.*;
import tw.edu.ntut.reutersclassificator.exception.NoSgmlFilesException;
import tw.edu.ntut.reutersclassificator.exception.NotDirectoryException;

/**
 * TODO: how far from correct assignment?
 * ReutersClassificator main class
 * ref http://goo.gl/OhSUTH http://goo.gl/zayG5U
 * http://goo.gl/p1Zor and http://goo.gl/9GjT6  r
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
            new Option("d", "docfile", true, "reuters dataset directory"),
            new Option("t", "threads", true, "(optional) number of threas"),
            new Option("f", "fulleval", true, "(optional) print full evaluation results"),
    };

    /**
     * controls the main tasks
     * measures their execution time
     * @param docFiles file files with sgml content
     * @throws InterruptedException
     */
    public static void run(List<File> docFiles, int threads, boolean fullEvaluation)
            throws InterruptedException {
        //parse
        System.out.printf("Parsing documents.. ");
        long startTime = System.nanoTime();
        SgmlDummyParser parser = SgmlDummyParser.create(docFiles);
        DataSet dataSet = parser.parseFiles();
        double delta = (System.nanoTime() - startTime)/1000000000.0;
        System.out.printf("[done in %.2f]\n", delta);
        // index docs and calc tvs for each doc
        Indexer indexer = Indexer.create(parser.getDocuments());
        System.out.printf("Calculating TVs.. ");
        startTime = System.nanoTime();
        indexer.index();
        indexer.retrieveTermVectorsParallel(threads);
        delta = (System.nanoTime() - startTime)/1000000000.0;
        System.out.printf("[done in %.2f]\n", delta);
        // calc centroids for each cat
        System.out.printf("Calculating centroids.. ");
        startTime = System.nanoTime();
        Collection<Category> categories = dataSet.getTopics().values();
        for (Category c: categories) {
            c.calcCentroid();
        }
        delta = (System.nanoTime() - startTime)/1000000000.0;
        System.out.printf("[done in %.2f]\n", delta);
        // assign test docs and evaluate
        System.out.printf("Classification.. ");
        startTime = System.nanoTime();
        Classifier classifier = Classifier.create(dataSet);
        classifier.classify();
        delta = (System.nanoTime() - startTime)/1000000000.0;
        System.out.printf("[done in %.2f]\n", delta);
        System.out.println("===============");
        System.out.println("Evaluation:");
        if (!fullEvaluation) {
            System.out.println("\tignoring not-assigned/not-retrieved topics\n");
        }
        classifier.evaluate(fullEvaluation);
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
                run(inspectWorkingDir(new File(workingDir)), threads, cli.hasOption("f"));
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
     * inspects given directory for sgm files
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

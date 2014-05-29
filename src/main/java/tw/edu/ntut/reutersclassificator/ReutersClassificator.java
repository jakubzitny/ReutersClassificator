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
import tw.edu.ntut.reutersclassificator.entity.*;
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
     * controls the main tasks
     * @param docFiles file files with sgml content
     * @throws InterruptedException
     */
    public static void runSequential(List<File> docFiles) throws InterruptedException {
        System.out.println("Parsing documents..");
        SgmlDummyParser parser = SgmlDummyParser.create(docFiles);
        parser.parseFiles();
        // index docs and calc tv for each doc
        Indexer indexer = new Indexer(parser.getDocuments());
        System.out.println("Calculating TVs..");
        indexer.index();
        indexer.retrieveTermVectorsParallel();
        // calc centroids for each cat
        System.out.println("Calculating centroids..");
        Collection<Category> categories = parser.getTopics().values();
        for (Category c: categories) {
            c.calcCentroid();
        }
        Map<String, Measure> measureMap = new HashMap<String, Measure>();
        // assign test docs
        System.out.println("Assigning test documents to categories..");
        for (Integer docId: parser.getTestDocuments().keySet()) {
            Document doc = parser.getTestDocuments().get(docId);
            double min = 1000;
            Category minCat = null;
            // prepare debug maps
            Map<String, Double> refCatCs = new HashMap<String, Double>();
            Map<String, Double> allCatCs = new HashMap<String, Double>();
            MapComparator bvc =  new MapComparator(allCatCs);
            TreeMap<String,Double> sortedAllCatCs= new TreeMap<String,Double>(bvc);
            // calc similarity for each category
            for (String key: parser.getTopics().keySet()) {
                Category cat = parser.getTopics().get(key);
                TermVector a = cat.getPrototypeTermVector();
                TermVector b = doc.getTermVector();
                Double dotProduct = TermVector.dotProduct(a, b);
                Double cs = Math.cos(dotProduct/(a.size() * b.size()));
                // find the lowest one
                if (cs < min) {
                    min = cs;
                    minCat = cat;
                }
                // assign debug similarities
                allCatCs.put(cat.getName(), cs);
                if (doc.getTopics().contains(cat.getName())) {
                    refCatCs.put(cat.getName(), cs);
                }
            }
            // sort allcat for debugging
            sortedAllCatCs.putAll(allCatCs);
            // assign really
            minCat.addTestDoc(doc);
            // test if properly assigned
//            boolean found = false;
//            for (String topic: doc.getTopics()) {
//                if (minCat.getName().equals(topic)) {
//                    found = true;
//                    break;
//                }
//            }
//            if (!found) {
//                if (!measureMap.containsKey(minCat.getName())) {
//                    Measure measure = new Measure(minCat.getName());
//                    measureMap.put(minCat.getName(), measure);
//                }
//                measureMap.get(minCat.getName()).addRetrieved();
//                //System.out.println("Wrongly assigned " + doc.getmNewId() + " to >" + minCat.getName() + "<");
//            } else {
//                if (!measureMap.containsKey(minCat.getName())) {
//                    Measure measure = new Measure(minCat.getName());
//                    measureMap.put(minCat.getName(), measure);
//                }
//                measureMap.get(minCat.getName()).addRelevant();
//                measureMap.get(minCat.getName()).addRetrieved();
//
//                //System.out.println("Correctly assigned " + doc.getmNewId() + " to >" + minCat.getName() + "<");
//            }
        }

        System.out.println("===============");
        System.out.println("Category\tRecall\tPrecision\tF-measure");
        // measure
        for (String key: parser.getTopics().keySet()) {
            List<Document> retrieved = parser.getTopics().get(key).getDocs();
            List<Document> relevant = new ArrayList<Document>();
            if (parser.getRefTopics().containsKey(key)) {
                Category refCat = parser.getRefTopics().get(key);
                relevant = refCat.getDocs();
            }
            List<Document> relevantRetrieved = new ArrayList<Document>(retrieved);
            relevantRetrieved.removeAll(relevant);
            double precision = relevantRetrieved.size()/retrieved.size();
            double recall = relevantRetrieved.size()/relevant.size();
            double f1 = 2 * ((precision * recall)/(precision + recall));
            System.out.println(key + "\t" + recall + "\t" + precision + "\t" + f1);
        }


//        System.out.println("=============================");
//        // print results for each category
//        for (Category c: categories) {
////            if (c.getDocs().size() > 0) {
////                System.out.println(c.getDocs().size() +
////                        " testing documents have been added to category '" + c.getName() + "'.");
////            }
//            for (Document d: c.getDocs()) {
//                String ass = c.getName();
//                boolean found = false;
//                for (String topic: d.getTopics()) {
//                    if (ass.equals(topic)) {
//                        found = true;
//                        break;
//                    }
//                }
//                if (!found) {
//                    System.out.println("Wrongly assigned >" + ass + "< to " + d.getmNewId());
//                } else {
//                    System.out.println("Correctly assigned >" + ass + "< to " + d.getmNewId() + "<<<<<");
//                }
//            }
//        }
    }

    private static class MapComparator implements Comparator<String> {

        Map<String, Double> base;
        public MapComparator(Map<String, Double> base) {
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

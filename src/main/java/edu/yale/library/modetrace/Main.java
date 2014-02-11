package edu.yale.library.modetrace;

import org.slf4j.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Run application
 */
public class Main {
    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final CacheStore cacheStore;

    private String cacheStorePath = "";

    private List<String> keys = new ArrayList(); //TODO

    private final MODE operation;

    public static void main(String[] args) throws FileNotFoundException {
        final Main main = newInstance(args);
        switch (main.operation) {
            case READ_NODE:
                switch (main.cacheStore) {
                    case FileCacheStore:
                        printNodesFileData(main.getCacheStorePath(), main.keys);
                        break;
                    default:
                        LOGGER.error("Unrecognized store.");
                        break;
                }
                break;
            case READ_FOLDER:
                switch (main.cacheStore) {
                    case FileCacheStore:
                        printFileCacheStoreArtifacts(main.getCacheStorePath());
                        break;
                    default:
                        LOGGER.error("Unrecognized store.");
                        break;
                }
                break;
            default:
                LOGGER.error("Not yet . . ."); //
                break;
        }
    }

    private static Main newInstance(final String args[]) throws IllegalArgumentException, FileNotFoundException {
        Main m = null;
        String type, location = "";
        final Options ops = createOptions();
        final CommandLineParser parser = new BasicParser();
        try {
            CommandLine cli = parser.parse(ops, args);
            if (cli.hasOption("h")) {
                printUsage(ops);
                System.exit(0);
            }
            if (cli.hasOption("t") && cli.hasOption("f")) {
                type = cli.getOptionValue("t");
                location = cli.getOptionValue("f");
                if (EnumUtils.isValidEnum(CacheStore.class, type) == false) {
                    throw new IllegalArgumentException("Invalid or unrecognized store : " + type);
                }
                if (cli.hasOption("n")) {
                    m = new Main(MODE.READ_NODE, CacheStore.valueOf(type), location, getKeys(cli.getOptionValue("n")));
                } else {
                    m = new Main(MODE.READ_FOLDER, CacheStore.valueOf(type), location);
                }
            } else {
                LOGGER.error("Error running program.");
                printUsage(ops);
                System.exit(1);
            }
        } catch (ParseException p) {
            p.printStackTrace();
        }
        return m;
    }

    /**
     * Prints file-node mapping
     *
     * @param path
     */
    public static void printNodesFileData(final String path, final List<String> keys) {
        LOGGER.debug("Printing node(s) data");
        String output = SimpleFileStoreReader.readNodes(path, keys);
        System.out.println(output); //TODO
    }

    /**
     * Prints file-node mapping
     *
     * @param path
     */
    public static void printFileCacheStoreArtifacts(final String path) {
        LOGGER.debug("Printing file cache store data");
        String output = SimpleFileStoreReader.readFolder(path);
        System.out.println(output); //TODO
    }

    @SuppressWarnings("static-access")
    private static Options createOptions() {
        Options ops = new Options();
        ops.addOption("h", "help", false, "print the help screen");
        ops.addOption(OptionBuilder
                .withArgName("filepath")
                .withDescription(
                        "Directory path. ")
                .withLongOpt("filepath").hasArg().create('f'));
        ops.addOption(OptionBuilder.withArgName("type").withDescription(
                "Type of the Infinispan store.")
                .withLongOpt("type").hasArg().create('t'));
        ops.addOption(OptionBuilder.withArgName("nodes").withDescription(
                "Text file with nodes.")
                .withLongOpt("nodes").hasArg().create('n'));
        return ops;
    }

    public static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar target/modetrace-jar-with-dependencies.jar -t {FileCacheStore} -f {/tmp/fcrepo4-data}", options);
    }

    //TODO
    static List getKeys(final String nodeFilePath) throws FileNotFoundException {
        List<String> list = new ArrayList();
        //e.g. ("87a0a8c317f1e7/jcr:system/jcr:nodeTypes/mix:lastModified");
        Scanner s = new Scanner(new File(nodeFilePath));
        while (s.hasNext()) {
            list.add(s.next());
        }
        return list;
    }

    public String getCacheStorePath() {
        return cacheStorePath;
    }

    public Main(MODE operation, CacheStore cacheStore, String path) {
        this.operation = operation;
        this.cacheStore = cacheStore;
        this.cacheStorePath = path;
    }

    public Main(MODE operation, CacheStore cacheStore, String path, List<String> keys) {
        this.operation = operation;
        this.cacheStore = cacheStore;
        this.cacheStorePath = path;
        this.keys = keys;
    }

    private enum MODE {
        READ_FOLDER, READ_NODE;
    }

    private enum CacheStore {

        FileCacheStore("FileCacheStore"),
        LevelDB("LevelDB");

        private String type;
        // other cache stories properties of interest?

        CacheStore(String type) {
            this.type = type;
        }

        String getType() {
            return type;
        }

        void setType(String type) {
            this.type = type;
        }
    }
}

package edu.yale.library.modetrace;

import org.slf4j.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.*;

/**
 * Run application
 */
public class Main {
    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final CacheStore cacheStore;

    private String cacheStorePath = "";

    public static void main(String[] args) {
        final Main main = newInstance(args);
        switch (main.cacheStore()) {
            case FileCacheStore:
                LOGGER.debug("Printing file cache store contents.");
                printFileCacheStoreArtifacts(main.getCacheStorePath());
                break;
            default:
                System.out.println("Currently unrecognized store.");
                break;
        }
    }

    /**
     * Prints file-node mapping
     *
     * @param path
     */
    public static void printFileCacheStoreArtifacts(String path) {
        String output = SimpleFileStoreReader.readFolder(path);
        System.out.println(output);
    }

    private static Main newInstance(final String args[]) throws IllegalArgumentException {
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
                m = new Main(CacheStore.valueOf(type), location);
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
        return ops;
    }

    public static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar target/modetrace-jar-with-dependencies.jar -t {FileCacheStore} -f {/tmp/fcrepo4-data}", options);
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

    public CacheStore cacheStore() {
        return cacheStore;
    }

    public Main(CacheStore cacheStore, String path) {
        this.cacheStore = cacheStore;
        this.cacheStorePath = path;
    }

    public String getCacheStorePath() {
        return cacheStorePath;
    }
}

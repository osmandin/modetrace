package edu.yale.library.modetrace;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.JTree;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * Minimal application launcher for displaying ModeShape artifacts.
 *
 * DISCLAIMER: This program is meant to read binary artifacts by examining raw files backing a particular store.
 * Although the program does NOT instantiate a cache store or ModeShape or Fedora 4 to read contents (and where possible
 * it's set up to get a connection in a read-only mode), do exercise care to avoid any file tampering.
 *
 * @author Osman Din
 */
public final class Main extends JPanel implements TreeSelectionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /**
     * Supported ispn cache stores.
     */
    enum CacheStore {
        FileCacheStore("FileCacheStore"),
        LevelDB("LevelDB");

        private String type;

        CacheStore(String type) {
            this.type = type;
        }
    }

    /** ispn cache store that the user wants to examine */
    private CacheStore cacheStore;

    private String cacheStorePath = "";
    private JSplitPane splitPane;
    private JTree tree;
    JTextArea textArea;

    /** Contains node to content mapping */
    Map<String, String> nodeMap = new TreeMap();

    public Main(CacheStore cacheStore, String path) {
        this.cacheStore = cacheStore;
        this.cacheStorePath = path;
    }

    public static void main(String[] args) throws FileNotFoundException {
        final Main instance = newInstance(args);
        LOGGER.debug("\nReading:{}\n", instance.getStorePath());
        switch (instance.cacheStore) {
            case FileCacheStore:
                instance.showFileCacheStoreArtifacts(instance.getStorePath());
                break;
            case LevelDB:
                instance.showLevelDBArtifacts(instance.getStorePath());
                break;
            default:
                break;
        }
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                displayFrame(instance);
                LOGGER.debug("\nCtrl+C to terminate\n");
            }
        });
    }

    private static Main newInstance(final String args[]) throws IllegalArgumentException, FileNotFoundException {
        Main instance = null;
        String type, location;
        final Options ops = CommandLineUtil.createOptions();
        final CommandLineParser parser = new BasicParser();
        try {
            CommandLine commandLine = parser.parse(ops, args);
            if (commandLine.hasOption("h")) {
                CommandLineUtil.printUsage(ops);
                System.exit(0);
            }
            if (commandLine.hasOption("t") && commandLine.hasOption("f")) {
                type = commandLine.getOptionValue("t");
                location = commandLine.getOptionValue("f");
                if (EnumUtils.isValidEnum(CacheStore.class, type) == false) {
                    throw new IllegalArgumentException("Invalid or unrecognized store : " + type);
                }
                instance = new Main(CacheStore.valueOf(type), location);
            } else {
                LOGGER.error("Error running program.");
                CommandLineUtil.printUsage(ops);
                System.exit(1);
            }
        } catch (ParseException p) {
            p.printStackTrace();
        }
        return instance;
    }

    /**
     * FileCacheStore
     *
     * @param path folder containing filecachestore binary files. This folder would contain files such as -123456789
     */
    private void showFileCacheStoreArtifacts(final String path) {
        AbstractReader reader = new FileStoreReader();
        nodeMap = reader.readFolder(path);
        initPane(nodeMap);
    }

    /**
     * LevelDB
     *
     * @param path folder containing leveldb files. This folder would contain files such as .log, .sst, LOCK
     */
    private void showLevelDBArtifacts(final String path) {
        AbstractReader reader = new LevelDBReader();
        nodeMap = reader.readFolder(path);
        initPane(nodeMap);
    }

    /**
     * Helps populate the tree with fedora nodes
     *
     * @param top
     * @param map
     */
    private void populateNodes(final DefaultMutableTreeNode top, Map<String, String> map) {
        final DefaultMutableTreeNode category;
        category = new DefaultMutableTreeNode("Fedora Nodes");
        top.add(category);

        for (Map.Entry<String, String> entry : map.entrySet()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(entry.getKey().toString());
            category.add(node);
        }
    }

    /**
     * Shows node content on tree click event
     * @param e
     */
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode nodeIdentifier = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (nodeIdentifier == null) return;
        if (nodeIdentifier.isLeaf()) {
            textArea.setText(getContents(nodeIdentifier.toString()));
        }
    }

    /**
     * Return contents for node
     * @param node String identifying node (e.g. file name for FileCacheStore; node for LevelDB)
     * @return
     */
    private String getContents(String node) {
        assert (nodeMap != null);
        return nodeMap.get(node).toString();
    }

    /**
     * GUI frame setup
     * @param main
     */
    private static void displayFrame(Main main) {
        JFrame frame = new JFrame("modetrace");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(main.getSplitPane());
        frame.pack();
        frame.setVisible(true);
    }

    private String getStorePath() {
        return cacheStorePath;
    }

    private JSplitPane getSplitPane() {
        return splitPane;
    }

    /**
     * Constructs JSplitPane, JTextArea, and JTree.
     *
     * @see #displayFrame(Main)
     * @param output
     */
    private void initPane(Map output) {
        /* Tree */
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("FileSystem Directory");
        populateNodes(top, output);
        tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(this);
        JScrollPane listScrollPane = new JScrollPane(tree);

        /* Text */
        textArea = defaultTextArea();
        JScrollPane textScrollPane = new JScrollPane(textArea);

        /* Split Pane */
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, textScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(400);
        Dimension minimumSize = new Dimension(600, 600);
        listScrollPane.setMinimumSize(minimumSize);
        textScrollPane.setMinimumSize(minimumSize);
        splitPane.setPreferredSize(new Dimension(800, 800));
    }

    /**
     * Get a JTextArea with some reasonable display settings
     * @return
     */
    private JTextArea defaultTextArea() {
        textArea = new JTextArea();
        textArea.setColumns(20);
        textArea.setLineWrap(true);
        textArea.setRows(30);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        return textArea;
    }

    /**
     * Usage helper
     */
    static class CommandLineUtil {

        @SuppressWarnings("static-access")
        private static Options createOptions() {
            Options o = new Options();
            o.addOption("h", "help", false, "print the help screen");
            o.addOption(argOption("path", "Directory containing ModeShape artifacts", "filepath", 'f'));
            o.addOption(argOption("store type", "Infinispan store type. Valid values=" +
                    Arrays.asList(CacheStore.values()), "type", 't'));
            return o;
        }

        private static Option argOption(final String arg, final String desc, final String longDesc, final char s) {
            return OptionBuilder.withArgName(arg).withDescription(desc)
                    .withLongOpt(longDesc).hasArg().create(s);
        }

        private static void printUsage(Options options) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar modetrace-jar-with-dependencies.jar " +
                    "-t store -f path", options);

        }
    }

}

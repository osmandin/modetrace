package edu.yale.library.modetrace;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.FileCacheStoreConfiguration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.infinispan.schematic.internal.CacheSchematicDb;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.infinispan.schematic.internal.document.BsonReader;
import org.infinispan.schematic.internal.document.ImmutableField;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SimpleFileStoreReader {

    private final static Logger LOGGER = LoggerFactory.getLogger(SimpleFileStoreReader.class);

    private static CacheSchematicDb schematicDB;
    private static Cache<String, SchematicEntry> ispnCache;
    private static EmbeddedCacheManager cacheManager;

    private static final String ISPN_CACHE = "documents";
    private static final String KEY_ID = "key"; //TODO
    private static final String FCREPO_TEST_FILE_DIRECTORY = "target/test-classes/files";



    public static String readFolder(final String path) {
        return doReadFolder(path);
    }

    private static String doReadFolder(final String path) {
        init(FCREPO_TEST_FILE_DIRECTORY);
        SimpleFileStoreReader s = new SimpleFileStoreReader();
        return s.testFedoraRootNodeFile();
    }

    /**
     * Init once
     * @param FCREPO_TEST_FILE_DIRECTORY
     */
    public static void init(final String FCREPO_TEST_FILE_DIRECTORY) {

        LOGGER.debug("Init SimpleFileStoreReader");
        ConfigurationBuilder cfgBuilder = new ConfigurationBuilder();
        cfgBuilder.invocationBatching().enable().transaction()
                .transactionManagerLookup(new DummyTransactionManagerLookup());
        FileCacheStoreConfiguration cfg = cfgBuilder.loaders().addFileCacheStore().location(FCREPO_TEST_FILE_DIRECTORY).create();
        cacheManager = TestCacheManagerFactory.createCacheManager(cfgBuilder);
        ispnCache = cacheManager.getCache(ISPN_CACHE);
        schematicDB = new CacheSchematicDb(ispnCache);
    }

    public String testFedoraRootNodeFile() {

        LOGGER.debug("Reading file node");

        final String key = "87a0a8c7505d64/"; //Fedora root node
        final SchematicEntry schematicEntry = schematicDB.get(key);

        /* Test whether the document can be read in the first place */
        Document document = schematicEntry.getContentAsDocument();

        boolean SKIP = true; //TODO remove

        /* Validate root node */
        final Document entryMetadata = schematicEntry.getMetadata();

        if (SKIP)
            return Json.write(entryMetadata);


        /* Validate children */
        Document children = document.getDocument("children");

        /* Verify a particular child, i.e a child node of Fedora root node */
        final String childNodeKeyString = "87a0a8c7505d644205a567-ac5f-41a7-b16f-66258bb54a53";
        final String childNodeKeyName = "FedoraDatastreamsTest63";

        Iterator<Document.Field> fieldsIterator = children.fields().iterator();
        BasicDocument testDocument = new BasicDocument(KEY_ID, childNodeKeyString, "name", childNodeKeyName);
        List<Document.Field> fieldList = IteratorUtils.toList(fieldsIterator);


        boolean foundMatch = false;
        Document persistedFedoraDocument = null;
        for (int i = 0; i < fieldList.size(); i++) {
            if (fieldList.get(i).getValueAsDocument().containsAll(testDocument)) {
                persistedFedoraDocument = fieldList.get(i).getValueAsDocument();
                foundMatch = true;
            }
        }

        LOGGER.debug("\n\nOK. Done reading Fedora repo node(s).\n\n");
        return "N/A";
    }




}

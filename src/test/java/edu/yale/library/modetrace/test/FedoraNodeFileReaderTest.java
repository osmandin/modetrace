package edu.yale.library.modetrace.test;

import org.apache.commons.collections.IteratorUtils;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.FileCacheStoreConfiguration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.CacheSchematicDb;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.infinispan.schematic.internal.document.ImmutableField;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;


/**
 * Reads and validates fedora node data persisted using org.infinispan.loaders.file.FileCacheStore
 */
public class FedoraNodeFileReaderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FedoraNodeFileReaderTest.class);

    private static final String FCREPO_TEST_FILE_DIRECTORY = "target/test-classes/files";
    private static final String ISPN_CACHE = "documents";
    private static final String KEY_ID = "key";
    private static final int EXPECTED_CHILD_NODES = 45;

    private CacheSchematicDb schematicDB;
    private Cache<String, SchematicEntry> ispnCache;
    private EmbeddedCacheManager cacheManager;

    @Before
    public void init() {

        ConfigurationBuilder cfgBuilder = new ConfigurationBuilder();
        cfgBuilder.invocationBatching().enable().transaction()
                .transactionManagerLookup(new DummyTransactionManagerLookup());
        FileCacheStoreConfiguration cfg = cfgBuilder.loaders().addFileCacheStore().location(FCREPO_TEST_FILE_DIRECTORY).create();
        cacheManager = TestCacheManagerFactory.createCacheManager(cfgBuilder);
        ispnCache = cacheManager.getCache(ISPN_CACHE);

        /* sanity check(s) */
        assertFalse("Wrong value for store property=purge", cfg.purgeOnStartup());
        assertEquals("Wrong value for store property=location", cfg.location(), FCREPO_TEST_FILE_DIRECTORY);

        schematicDB = new CacheSchematicDb(ispnCache);
    }

    /**
     * Test contents against a file that we know contains root node.
     * <p/>
     * Specifically, verifies Fedora root node, children count, and a child node's properties.
     */
    @Test
    public void testFedoraRootNodeFile() {

        assertNotNull("Error getting store instance.", schematicDB);

        final String key = "87a0a8c7505d64/"; //Fedora root node
        final SchematicEntry schematicEntry = schematicDB.get(key);

        assertNotNull("Store file entry set null", schematicEntry);

        /* Test whether the document can be read in the first place */
        Document document = schematicEntry.getContentAsDocument();
        assertNotNull("FAILED at reading contents. Document null", document);
        assertEquals("Document elements missing", document.size(), 4);

        /* Validate root node */
        final Document entryMetadata = schematicEntry.getMetadata();
        assertEquals("Entry key identifier mismatch", entryMetadata.get("id"), key); //=87a0a8c7505d64/
        assertEquals("Entry content type mismatch", entryMetadata.get("contentType"), "application/json");
        assertEquals("Root node properties content mismatch",
                document.get("properties").toString(),
                "{ \"http://www.jcp.org/jcr/1.0\" : { \"primaryType\" : { \"$name\" : \"mode:root\" } ," +
                        " \"uuid\" : \"87a0a8c7505d64/\" } }");

        /* Validate children */
        Document children = document.getDocument("children");
        assertEquals("Children element missing", children.size(), EXPECTED_CHILD_NODES);

        /* Verify a particular child, i.e a child node of Fedora root node */
        final String childNodeKeyString = "87a0a8c7505d644205a567-ac5f-41a7-b16f-66258bb54a53";
        final String childNodeKeyName = "FedoraDatastreamsTest63";

        Iterator<Document.Field> fieldsIterator = children.fields().iterator();
        BasicDocument testDocument = new BasicDocument(KEY_ID, childNodeKeyString, "name", childNodeKeyName);
        List<Document.Field> fieldList = IteratorUtils.toList(fieldsIterator);

        assert (fieldList.size() == EXPECTED_CHILD_NODES);

        boolean foundMatch = false;
        Document persistedFedoraDocument = null;
        for (int i = 0; i < fieldList.size(); i++) {
            if (fieldList.get(i).getValueAsDocument().containsAll(testDocument)) {
                persistedFedoraDocument = fieldList.get(i).getValueAsDocument();
                foundMatch = true;
            }
        }

        assertTrue("Test entry does not match any actual/persisted entry. " +
                "This means that the expected node was not found in the persistence backend", foundMatch);

        /* Validate child property using a Document */
        assertEquals(persistedFedoraDocument.get(KEY_ID), testDocument.get(KEY_ID));

        /* Alternatively, check using a Document.Field */
        Document.Field testField = new ImmutableField(childNodeKeyString, childNodeKeyName);
        assertEquals(persistedFedoraDocument.get(KEY_ID), testField.getName());

        LOGGER.debug("\n\nOK. Done reading Fedora repo node(s).\n\n");
    }

    @After
    public void afterTest() {
        //ispnCache.stop();
        ispnCache = null;
        //schematicDB.stop();
        schematicDB = null;
    }

}

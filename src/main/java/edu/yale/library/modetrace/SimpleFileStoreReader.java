package edu.yale.library.modetrace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.infinispan.schematic.internal.CacheSchematicDb;
import org.infinispan.schematic.internal.document.BsonReader;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.ByteArrayInputStream;


public class SimpleFileStoreReader {

    private final static Logger LOGGER = LoggerFactory.getLogger(SimpleFileStoreReader.class);

    private static CacheSchematicDb schematicDB;
    private static Cache<String, SchematicEntry> ispnCache;
    private static EmbeddedCacheManager cacheManager;

    private static final String ISPN_CACHE = "documents"; //TODO should be fcrep4-data or something

    public static String readFolder(final String path) {
        List<String> keys = doReadFolderAsList(path);
        return doReadNodes(path, keys);
    }

    public static String readNodes(final String path, List<String> keys) {
        return doReadNodes(path, keys);
    }

    private static String doReadNodes(final String path, List<String> keys) {
        initCacheStore(path);
        SimpleFileStoreReader s = new SimpleFileStoreReader();
        return s.readNodeEntry(keys);
    }

    private static List<String> doReadFolderAsList(final String path) {
        return BsonReaderUtil.readNodeFolderAsList(path);
    }

    /**
     * For now reads only (id and content type), but can easily read the full document.
     * Perhaps also return file name.
     *
     * @param nodes
     * @return
     */
    public String readNodeEntry(final List<String> nodes) {

        long unableToReadCount = 0;
        StringBuffer sb = new StringBuffer();
        org.infinispan.Cache cache = schematicDB.getCache();
        for (String n : nodes) {
            cache.get(n);
            final SchematicEntry schematicEntry = schematicDB.get(n);
            if (schematicEntry == null) { //TODO
                unableToReadCount++;
                //LOGGER.debug("Unable to read:" + n );
                //problem with some escape chars e.g in http://fedora.info/definitions
                continue;  //FIXME
            }
            final Document entryMetadata = schematicEntry.getMetadata();
            sb.append(Json.write(entryMetadata) + System.getProperty("line.separator"));
        }
        LOGGER.debug("Cache size=" + cache.size());
        LOGGER.debug("Unable to read=" + unableToReadCount);
        return sb.toString();
    }

    /**
     * Reads file using org.infinispan.schematic.internal.document.BsonReader.
     * TODO Ends up reading only first few hundred bytes. merge files for filecachestore
     * TODO replace Commons FileUtils
     */
    private static final class BsonReaderUtil {

        private static String readNodeFile(final byte[] bytes) {
            BsonReader reader = new BsonReader();
            String documentString = "NOT READ";
            try {
                final Document document = reader.read(new ByteArrayInputStream(bytes));
                documentString = document.toString();
                return documentString;
            } catch (NullPointerException e) {
                throw e;
            } catch (IOException io) {
                //ignore for now
            }
            return documentString;
        }

        private static List<String> readNodeFolderAsList(final String path) {
            StringBuilder sb = new StringBuilder();
            List<String> data = new ArrayList<String>();
            final File dir = new File(path);
            final File[] files = dir.listFiles();
            for (final File f : files) {
                try {
                    final byte[] bytes = FileUtils.readFileToByteArray(f);
                    data.add(filterNodeDataWithoutFileId(f.getName(), readNodeFile(bytes)));
                } catch (NullPointerException e) {
                    throw e;
                } catch (Exception e) {
                    continue; //ignore for now
                }
            }
            return data;
        }

        /**
         * TODO temporary
         *
         * @param file
         * @param str
         * @return
         */
        private static String filterNodeDataWithoutFileId(final String file, final String str) {
            if (!str.contains(":")) { // no node data
                return "{" + file + ":" + str + "}" + "\n";
            }
            int startIndex = 8;
            int endIndex = str.length() - 12;
            String modifiedString = str.replace(": null }", "");
            modifiedString = str.substring(startIndex, endIndex);
            //LOGGER.debug("Str:" + modifiedString);
            return modifiedString;
        }
    }

    /**
     * TODO Init once, clean up etc
     *
     * @param FCREPO_DIRECTORY
     */
    public static void initCacheStore(final String FCREPO_DIRECTORY) {

        ConfigurationBuilder cfgBuilder = new ConfigurationBuilder();
        cfgBuilder.invocationBatching().enable().transaction()
                .transactionManagerLookup(new DummyTransactionManagerLookup());
        cfgBuilder.loaders().addFileCacheStore().location(FCREPO_DIRECTORY).create();
        cacheManager = TestCacheManagerFactory.createCacheManager(cfgBuilder);
        ispnCache = cacheManager.getCache(ISPN_CACHE);
        schematicDB = new CacheSchematicDb(ispnCache);
    }
}

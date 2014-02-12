package edu.yale.library.modetrace;

import java.io.File;
import java.io.FilenameFilter;

/**
 * FileCacheStore bases file names on a node's hash. This class can help calculate this data
 * and act as a java.io.FilenameFilter
 * @author Osman Din
 */
public final class FileLookupUtil implements FilenameFilter {

    private static final int MASK = 0xfffffc00;

    public static String getFileName(String node) {
        return FileCacheStoreObjectHash.getEntryHash(node).toString();
    }

    @Override
    public boolean accept(File dir, String fileName) {
        return fileName.matches("([1-9]|-)[0-9]{9,10}") ? true : false;
    }

    private static final class FileCacheStoreObjectHash {
        public static Integer getEntryHash(Object key) {
            return key.hashCode() & MASK;
        }
    }

}

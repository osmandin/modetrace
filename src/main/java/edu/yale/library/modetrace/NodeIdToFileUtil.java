package edu.yale.library.modetrace;

public class NodeIdToFileUtil {

    private final static int MASK = 0xfffffc00;

    public static String getFileName(String node) {
        return FileCacheStoreObjectHash.getEntryHash(node).toString();
    }

    private final static class FileCacheStoreObjectHash {
        public static Integer getEntryHash(Object key) {
            return key.hashCode() & MASK;
        }
    }

}

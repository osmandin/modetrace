package edu.yale.library.modetrace;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.document.BsonReader;

public class SimpleFileStoreReader {
    public static String readFolder(final String path) {
        return doReadFolder(path);
    }

    private static String doReadFolder(final String path) {
        return BsonReaderUtil.readNodeFolder(path);
    }

    /**
     * Reads file using org.infinispan.schematic.internal.document.BsonReader.
     * TODO Ends up reading only first few hundred bytes. merge files for filecachestore
     * TODO replace Commons FileUtils
     */
    private static final class BsonReaderUtil {
        private static String readNodeFolder(final String path) {
            StringBuilder sb = new StringBuilder();
            final File dir = new File(path);
            final File[] files = dir.listFiles();
            for (final File f : files) {
                try {
                    final byte[] bytes = FileUtils.readFileToByteArray(f);
                    sb.append(filterNodeData(f.getName(), readNodeFile(bytes)));
                } catch (NullPointerException e) {
                    throw e;
                } catch (Exception e) {
                    continue; //ignore for now
                }
            }
            return sb.toString();
        }

        private static String readNodeFile(byte[] bytes) {
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


        /**
         * TODO temporary
         * TODO print formatting.  Replace w/ JSON pretty print
         *
         * @param file
         * @param str
         * @return
         */
        private static String filterNodeData(final String file, final String str) {
            if (!str.contains(":")) { // no node data
                return "{" + file + ":" + str + "}" + "\n";
            }
            int startIndex = 8;
            int endIndex = str.length() - 12;
            String modifiedString = str.replace(": null }", "");
            modifiedString = "{" + file + ":" + str.substring(startIndex, endIndex) + "}" + "\n";
            return modifiedString;
        }
    }

}

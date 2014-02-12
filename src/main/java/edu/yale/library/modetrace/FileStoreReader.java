package edu.yale.library.modetrace;

import org.apache.commons.io.FileUtils;
import org.infinispan.schematic.internal.SchematicEntryLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.HashMap;


/**
 * @author Osman Din
 */
public class FileStoreReader extends AbstractReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStoreReader.class);

    public Map<String, SchematicEntryLiteral> readSerializedObjects(final String path) {
        long passCount = 0;
        long failCount = 0;
        final Map objectMap = new HashMap();
        final File dir = new File(path);
        final File[] files = dir.listFiles(new FileLookupUtil());

        for (final File f : files) {
            try {
                final byte[] fileBytes = FileUtils.readFileToByteArray(f);
                final SchematicEntryLiteral schematicEntry = convertBytes(fileBytes);
                assert (schematicEntry != null);
                //todo validate?
                objectMap.put(f.getName(), schematicEntry);
                passCount++;
            } catch (Exception e) {
                LOGGER.error("Could not read file={}", f.getName(), e);
                failCount++;
                break;
            }
        }
        LOGGER.debug("File count={}", files.length);
        LOGGER.debug("Passed reads={}", passCount);
        LOGGER.debug("Failed reads={}", failCount);
        return objectMap;
    }

}

package edu.yale.library.modetrace;


import org.infinispan.schematic.internal.SchematicEntryLiteral;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

/**
 * A reader that can read LevelDB contents.
 * @author Osman Din
 */
public class LevelDBReader extends AbstractReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(LevelDBReader.class);

    protected Map<String, SchematicEntryLiteral> readSerializedObjects(final String path) {
        long passCount = 0;
        long failCount = 0;
        final Map<String, SchematicEntryLiteral> objectMap = new HashMap();
        try {
            Options options = new Options();
            options.createIfMissing(false);
            final DB levelDB = factory.open(new File(path), options);
            assert (levelDB != null);
            DBIterator it = levelDB.iterator();
            assert (it != null);

            for (it.seekToFirst(); it.hasNext(); it.next()) {
                byte[] value = it.peekNext().getValue();
                assert (value != null);
                SchematicEntryLiteral o;
                try {
                    o = convertBytes(value);
                    assert (o != null);
                    objectMap.put(o.getMetadata().getString("id"), o);
                    passCount++;
                } catch (Exception e) {
                    e.printStackTrace();
                    failCount++;
                }
            }
            levelDB.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.debug("Passed reads={}", passCount);
        LOGGER.debug("Failed reads={}", failCount);
        return objectMap;
    }

}

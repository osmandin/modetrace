package edu.yale.library.modetrace.test;

import org.apache.commons.lang3.SystemUtils;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.SchematicEntryLiteral;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.fusesource.leveldbjni.JniDBFactory.factory;
import static org.junit.Assert.fail;

/**
 * Test against a pre-generated Fedora LevelDB file(s).
 */
public class LevelDBReaderTest extends AbstractTest {

    private static String getFolder() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "target/test-classes/fcrepo-leveldb-win/data";
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            return "target/test-classes/fcrepo-leveldb/data";
        } else
            throw new UnsupportedOperationException("ISPN store not supported on this platform.");
    }

    @Test
    public void testFedoraContentsInLevelDB() {
        List<SchematicEntryLiteral> list = new ArrayList();
        try {
            Options options = new Options();
            options.createIfMissing(false);
            final DB levelDB = factory.open(new File(getFolder()), options);
            assert (levelDB != null);
            DBIterator it = levelDB.iterator();
            assert (it != null);

            for (it.seekToFirst(); it.hasNext(); it.next()) {
                byte[] value = it.peekNext().getValue();
                assert (value != null);
                SchematicEntryLiteral o = convertBytes(value);
                assert (o != null);
                list.add(o);
            }
            levelDB.close();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        //match against a particular node created
        boolean foundMatch = false;
        for (SchematicEntryLiteral o : list) {
            Document d = o.asDocument();
            if (d.toString().contains("87a0a8c7505d64/")) {
                foundMatch = true;
            }
        }
        assert (foundMatch == true);
    }


}

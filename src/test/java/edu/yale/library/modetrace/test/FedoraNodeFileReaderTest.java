package edu.yale.library.modetrace.test;

import org.apache.commons.io.IOUtils;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.SchematicEntryLiteral;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.Assert.fail;

/**
 * Test against a pre-generated Fedora FileCacheStore file.
 */
public class FedoraNodeFileReaderTest extends AbstractTest {

    private static final String FILECACHESTORE_TEST_FILE = "target/test-classes/files/documents/-891938816";

    /**
     * Fedora reading test.
     */
    @Test
    public void testFileContents() {
        try {
            InputStream is = new FileInputStream(FILECACHESTORE_TEST_FILE);
            byte[] bytes = IOUtils.toByteArray(is);
            SchematicEntryLiteral o = convertBytes(bytes);
            assert (o != null);
            Document d = o.asDocument();
            if (!d.toString().contains("\"uuid\" : \"87a0a8c7505d64/\"")) {
                fail("Content not found");
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}

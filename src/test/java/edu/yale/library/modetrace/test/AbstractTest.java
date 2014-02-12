package edu.yale.library.modetrace.test;

import edu.yale.library.modetrace.SerializationUtil;
import org.infinispan.schematic.internal.SchematicEntryLiteral;
import org.jboss.marshalling.river.Protocol;

import java.nio.ByteBuffer;

public class AbstractTest {
    private static final int ID_PREDEFINED_OBJECT_INDEX = 0;
    private static final int ID_NEW_OBJECT_INDEX = 1;

    protected SchematicEntryLiteral convertBytes(byte[] fileBytes) {
        try {
            ByteBuffer srcBuffer = ByteBuffer.wrap(fileBytes);
            while (srcBuffer.get() != Protocol.ID_NEW_OBJECT)
                srcBuffer.mark();
            srcBuffer.reset();
            final byte[] subBytes = new byte[srcBuffer.remaining() + 1];
            subBytes[ID_PREDEFINED_OBJECT_INDEX] = Protocol.ID_PREDEFINED_OBJECT;
            srcBuffer.get(subBytes, ID_NEW_OBJECT_INDEX, srcBuffer.remaining());
            assert (subBytes != null);
            final SchematicEntryLiteral bsonDocument = (SchematicEntryLiteral) SerializationUtil.unmarshall(subBytes);
            assert (bsonDocument != null);
            return bsonDocument;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

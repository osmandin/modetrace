package edu.yale.library.modetrace;

import org.infinispan.schematic.internal.SchematicEntryLiteral;
import org.infinispan.schematic.internal.document.PrettyJsonWriter;
import org.jboss.marshalling.river.Protocol;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * An abstract reader defining interface for concrete implementations and for extracting and manipulating
 * org.infinispan.schematic.internal.SchematicEntryLiteral contents out of a JBoss Marshalling serialized binary
 *
 * @author Osman Din
 */
public abstract class AbstractReader {

    private static final PrettyJsonWriter prettyJsonWriter;

    static {
        prettyJsonWriter = new PrettyJsonWriter();
    }

    private static final int ID_PREDEFINED_OBJECT_INDEX = 0;
    private static final int ID_NEW_OBJECT_INDEX = 1;

    public Map<String, String> readFolder(final String path) {
        return new TreeMap(doReadFolder(path));
    }

    private Map<String, String> doReadFolder(final String path) {
        Map<String, String> stringMap = new HashMap();
        Map<String, SchematicEntryLiteral> objectMap = readSerializedObjects(path);
        for (Map.Entry<String, SchematicEntryLiteral> entry : objectMap.entrySet()) {
            stringMap.put(entry.getKey(), transform(entry.getValue()));
        }
        return stringMap;
    }

    private static String transform(final SchematicEntryLiteral entry) {
        return prettyJsonWriter.write(entry.getContentAsDocument());
    }

    public SchematicEntryLiteral convertBytes(final byte[] fileBytes) {
        try {
            ByteBuffer srcBuffer = ByteBuffer.wrap(fileBytes);
            while (srcBuffer.get() != Protocol.ID_NEW_OBJECT)
                srcBuffer.mark();
            srcBuffer.reset();
            final byte[] subBytes = new byte[srcBuffer.remaining() + 1];
            subBytes[ID_PREDEFINED_OBJECT_INDEX] = Protocol.ID_PREDEFINED_OBJECT;
            srcBuffer.get(subBytes, ID_NEW_OBJECT_INDEX, srcBuffer.remaining());
            assert (subBytes != null);
            final SchematicEntryLiteral schematicEntry = (SchematicEntryLiteral) SerializationUtil.unmarshall(subBytes);
            assert (schematicEntry != null);
            return schematicEntry;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    abstract Map<String, SchematicEntryLiteral> readSerializedObjects(final String path);
}

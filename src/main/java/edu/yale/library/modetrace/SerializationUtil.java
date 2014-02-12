package edu.yale.library.modetrace;

import org.infinispan.marshall.jboss.JBossExternalizerAdapter;

import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.document.Timestamp;
import org.infinispan.schematic.document.ObjectId;
import org.infinispan.schematic.document.MinKey;
import org.infinispan.schematic.document.MaxKey;
import org.infinispan.schematic.document.Null;
import org.infinispan.schematic.internal.SchematicEntryDelta;
import org.infinispan.schematic.internal.SchematicEntryLiteral;
import org.infinispan.schematic.internal.SchematicEntryWholeDelta;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.delta.AddValueIfAbsentOperation;
import org.infinispan.schematic.internal.delta.AddValueOperation;
import org.infinispan.schematic.internal.delta.PutIfAbsentOperation;
import org.infinispan.schematic.internal.delta.PutOperation;
import org.infinispan.schematic.internal.delta.ClearOperation;
import org.infinispan.schematic.internal.delta.RemoveOperation;
import org.infinispan.schematic.internal.delta.RemoveAllValuesOperation;
import org.infinispan.schematic.internal.delta.RemoveValueOperation;
import org.infinispan.schematic.internal.delta.RemoveAtIndexOperation;
import org.infinispan.schematic.internal.delta.RetainAllValuesOperation;
import org.infinispan.schematic.internal.delta.SetValueOperation;
import org.infinispan.schematic.internal.document.ArrayExternalizer;
import org.infinispan.schematic.internal.document.DocumentExternalizer;
import org.infinispan.schematic.internal.document.Paths;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.MappingClassExternalizerFactory;
import org.jboss.marshalling.ClassExternalizerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds ModeShape BSON data types to org.jboss.marshalling.MarshallingConfiguration. It's possible
 * that not all data types are currently in use in Fcrepo.
 * <p/>
 * @author Osman Din (Code based on ModeShape test org.infinispan.schematic.internal.document.AbstractExternalizerTest).
 */
public final class SerializationUtil {

    private static final String JBOSS_MARSHALLER = "river";
    private static final int VERSION = 3;
    private static final MarshallerFactory marshallerFactory;
    private static final MarshallingConfiguration configuration;
    private static final Map<Class<?>, Externalizer> externalizersByClass = new HashMap();

    static {
        marshallerFactory = Marshalling.getProvidedMarshallerFactory(JBOSS_MARSHALLER);
        configuration = new MarshallingConfiguration();
        configuration.setVersion(VERSION);
        add(new SchematicEntryLiteral.Externalizer());
        add(new DocumentExternalizer());
        add(new ArrayExternalizer());
        add(new PutOperation.Externalizer());
        add(new PutIfAbsentOperation.Externalizer());
        add(new RemoveOperation.Externalizer());
        add(new AddValueOperation.Externalizer());
        add(new AddValueIfAbsentOperation.Externalizer());
        add(new ClearOperation.Externalizer());
        add(new RemoveValueOperation.Externalizer());
        add(new RemoveAllValuesOperation.Externalizer());
        add(new RemoveAtIndexOperation.Externalizer());
        add(new RetainAllValuesOperation.Externalizer());
        add(new SetValueOperation.Externalizer());
        add(new SchematicEntryDelta.Externalizer());
        add(new SchematicEntryWholeDelta.Externalizer());
        add(new Paths.Externalizer());
        add(new Binary.Externalizer());
        add(new Code.Externalizer());
        add(new MaxKey.Externalizer());
        add(new MinKey.Externalizer());
        add(new Null.Externalizer());
        add(new ObjectId.Externalizer());
        add(new Symbol.Externalizer());
        add(new Timestamp.Externalizer());
        add(new AddValueIfAbsentOperation.Externalizer());
        ClassExternalizerFactory factory = new MappingClassExternalizerFactory(externalizersByClass);
        configuration.setClassExternalizerFactory(factory);
    }

    public static void add(SchematicExternalizer<?> externalizer) {
        Externalizer adapter = new JBossExternalizerAdapter(externalizer);
        for (Class<?> clazz : externalizer.getTypeClasses()) {
            externalizersByClass.put(clazz, adapter);
        }
    }

    public static Object unmarshall(final byte[] bytes) throws IOException, ClassNotFoundException {
        final Unmarshaller unmarshaller = marshallerFactory.createUnmarshaller(configuration);
        final ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        try {
            unmarshaller.start(Marshalling.createByteInput(is));
            final Object result = unmarshaller.readObject();
            unmarshaller.finish();
            is.close();
            return result;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

package lu.uni.serval.commons.runner.utils.configuration;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigurationParser {
    private static final Logger logger = LogManager.getLogger(ConfigurationParser.class);

    public static <T extends Configuration> T parse(String config, Class<T> type) throws IOException {
        File file = new File(config);

        if(!file.exists()){
            throw new IOException(String.format("Configuration file '%s' does not exist!", file.getAbsolutePath()));
        }

        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(new Jdk8Module(), new FolderModule(file.getParentFile()));

        final T mercatorConfiguration = mapper.readValue(file, type);

        logger.info("Configuration loaded from " + config);

        return mercatorConfiguration;
    }

    public static class FolderModule extends SimpleModule {
        private final File folder;

        FolderModule(File folder){
            this.folder = folder;
        }

        @Override
        public void setupModule(SetupContext context) {
            super.setupModule(context);
            context.addBeanDeserializerModifier(new BeanDeserializerModifier()
            {
                @Override public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer)
                {
                    if (Configuration.class.isAssignableFrom(beanDesc.getBeanClass())){
                        return new Modifier(beanDesc.getBeanClass(), deserializer, folder);
                    }

                    return deserializer;
                }
            });
        }
    }

    public static class Modifier<T extends Configuration> extends StdDeserializer<T> implements ResolvableDeserializer{
        private final JsonDeserializer<?> defaultDeserializer;
        private final File folder;

        public Modifier(Class<T> type, JsonDeserializer<?> defaultDeserializer, File folder) {
            super(type);

            this.defaultDeserializer = defaultDeserializer;
            this.folder = folder;
        }

        @Override
        public void resolve(DeserializationContext ctxt) throws JsonMappingException {
            ((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
        }

        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            T configuration = (T) defaultDeserializer.deserialize(p, ctxt);
            configuration.setFolder(this.folder);

            return configuration;
        }
    }

}

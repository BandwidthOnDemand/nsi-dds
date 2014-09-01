package net.es.nsi.pce.jersey;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;

/**
 *
 * @author hacksaw
 */
@Provider
public class JsonMoxyConfigurationContextResolver implements ContextResolver<MoxyJsonConfig> {
        private final MoxyJsonConfig config;

        public JsonMoxyConfigurationContextResolver() {
            config = new MoxyJsonConfig();
            config.setNamespacePrefixMapper(Utilities.getNameSpace());
            config.setNamespaceSeparator('.');
            config.setAttributePrefix("@");
            config.setFormattedOutput(true);
        }

        @Override
        public MoxyJsonConfig getContext(Class<?> objectType) {
            return config;
        }
}

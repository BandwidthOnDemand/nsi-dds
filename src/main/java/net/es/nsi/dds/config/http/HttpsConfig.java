package net.es.nsi.dds.config.http;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.net.ssl.SSLContext;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.jaxb.configuration.KeyStoreType;
import net.es.nsi.dds.jaxb.configuration.ObjectFactory;
import net.es.nsi.dds.jaxb.configuration.SecureType;
import org.glassfish.jersey.SslConfigurator;

public class HttpsConfig {
    private final ObjectFactory factory = new ObjectFactory();

    private String basedir;
    private SecureType config;

    public HttpsConfig(SecureType config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("HttpConfig: server configuration not provided");
        }

        // We will use the application basedir to fully qualify any relative paths.
        basedir = System.getProperty(Properties.SYSTEM_PROPERTY_BASEDIR);

        // Determine the keystore configuration.
        KeyStoreType keyStore = config.getKeyStore();
        if (keyStore == null) {
            // Check to see if the keystore was provided on the commandline.
            keyStore = factory.createKeyStoreType();
            keyStore.setFile(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE, Properties.DEFAULT_SSL_KEYSTORE));
            keyStore.setPassword(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD, Properties.DEFAULT_SSL_KEYSTORE_PASSWORD));
            keyStore.setType(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE, Properties.DEFAULT_SSL_KEYSTORE_TYPE));
        }

        keyStore.setFile(getAbsolutePath(keyStore.getFile()));

        KeyStoreType trustStore = config.getTrustStore();
        if (trustStore == null) {
            trustStore = factory.createKeyStoreType();
            trustStore.setFile(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE, Properties.DEFAULT_SSL_TRUSTSTORE));
            trustStore.setPassword(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_PASSWORD, Properties.DEFAULT_SSL_TRUSTSTORE_PASSWORD));
            trustStore.setType(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_TYPE, Properties.DEFAULT_SSL_TRUSTSTORE_TYPE));
        }

        trustStore.setFile(getAbsolutePath(trustStore.getFile()));

        this.config = config;
    }

    private String getAbsolutePath(String inPath) throws IOException {
        Path outPath = Paths.get(inPath);
        if (!outPath.isAbsolute()) {
            outPath = Paths.get(basedir, inPath);
        }

        return outPath.toRealPath().toString();
    }

    public SSLContext getSSLContext() {
        SslConfigurator sslConfig = SslConfigurator.newInstance()
            .trustStoreFile(config.getTrustStore().getFile())
            .trustStorePassword(config.getTrustStore().getPassword())
            .trustStoreType(config.getTrustStore().getType())
            .keyStoreFile(config.getKeyStore().getFile())
            .keyPassword(config.getKeyStore().getPassword())
            .keyStoreType(config.getKeyStore().getType())
            .securityProtocol("TLS");
        return sslConfig.createSSLContext();
    }

    public boolean isProduction() {
        return config.isProduction();
    }
}

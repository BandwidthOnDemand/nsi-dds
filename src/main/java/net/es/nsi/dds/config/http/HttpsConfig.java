package net.es.nsi.dds.config.http;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.net.ssl.SSLContext;
import net.es.nsi.dds.api.jaxb.KeyStoreType;
import net.es.nsi.dds.api.jaxb.ObjectFactory;
import net.es.nsi.dds.api.jaxb.SecureType;
import net.es.nsi.dds.config.Properties;
import org.glassfish.jersey.SslConfigurator;

public class HttpsConfig {
    private final static String SSL_KEYSTORE = "javax.net.ssl.keyStore";
    private final static String DEFAULT_SSL_KEYSTORE = "config/keystore.jks";
    private final static String SSL_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    private final static String DEFAULT_SSL_KEYSTORE_PASSWORD = "changeit";
    private final static String SSL_KEYSTORE_TYPE = "javax.net.ssl.keyStoreType";
    private final static String DEFAULT_SSL_KEYSTORE_TYPE = "JKS";
    private final static String SSL_TRUSTSTORE = "javax.net.ssl.trustStore";
    private final static String DEFAULT_SSL_TRUSTSTORE = "config/truststore.jks";
    private final static String SSL_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    private final static String DEFAULT_SSL_TRUSTSTORE_PASSWORD = "changeit";
    private final static String SSL_TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";
    private final static String DEFAULT_SSL_TRUSTSTORE_TYPE = "JKS";

    private final ObjectFactory factory = new ObjectFactory();

    private String basedir;
    private SecureType config;

    public HttpsConfig(SecureType config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("HttpConfig: server configuration not provided");
        }

        // We will use the application basedir to fully qualify any relative paths.
        basedir = System.getProperty(Properties.DDS_SYSTEM_PROPERTY_BASEDIR);

        // Determine the keystore configuration.
        KeyStoreType keyStore = config.getKeyStore();
        if (keyStore == null) {
            // Check to see if the keystore was provided on the commandline.
            keyStore = factory.createKeyStoreType();
            keyStore.setFile(System.getProperty(SSL_KEYSTORE, DEFAULT_SSL_KEYSTORE));
            keyStore.setPassword(System.getProperty(SSL_KEYSTORE_PASSWORD, DEFAULT_SSL_KEYSTORE_PASSWORD));
            keyStore.setType(System.getProperty(SSL_KEYSTORE_TYPE, DEFAULT_SSL_KEYSTORE_TYPE));
        }

        keyStore.setFile(getAbsolutePath(keyStore.getFile()));

        KeyStoreType trustStore = config.getTrustStore();
        if (trustStore == null) {
            trustStore = factory.createKeyStoreType();
            trustStore.setFile(System.getProperty(SSL_TRUSTSTORE, DEFAULT_SSL_TRUSTSTORE));
            trustStore.setPassword(System.getProperty(SSL_TRUSTSTORE_PASSWORD, DEFAULT_SSL_TRUSTSTORE_PASSWORD));
            trustStore.setType(System.getProperty(SSL_TRUSTSTORE_TYPE, DEFAULT_SSL_TRUSTSTORE_TYPE));
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

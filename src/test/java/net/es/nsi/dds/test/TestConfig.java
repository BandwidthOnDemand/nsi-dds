package net.es.nsi.dds.test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.client.RestClient;
import net.es.nsi.dds.config.ConfigurationManager;
import net.es.nsi.dds.config.Properties;
import org.glassfish.jersey.client.ClientConfig;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class TestConfig {
    private static final String CONFIG_PATH = "configPath";
    private final static String CONFIG_DIR = "src/test/resources/config/";
    public static final String DEFAULT_DDS_FILE = CONFIG_DIR + "dds.xml";
    private static final String DDS_CONFIG_FILE_ARGNAME = "ddsConfigFile";

    private final Client client;
    private final WebTarget target;

    public TestConfig() throws IllegalStateException, KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        System.setProperty(CONFIG_PATH, CONFIG_DIR);
        System.setProperty(DDS_CONFIG_FILE_ARGNAME, DEFAULT_DDS_FILE);
        try {
            if (ConfigurationManager.INSTANCE.isInitialized()) {
                log.info("TestConfig: ConfigurationManager already initialized so shutting down.");
                ConfigurationManager.INSTANCE.shutdown();
            }
            System.setProperty(Properties.SYSTEM_PROPERTY_CONFIGDIR, CONFIG_DIR);
            ConfigurationManager.INSTANCE.initialize();
        }
        catch (IOException | InterruptedException ex) {
            log.error("TestConfig: failed to initialize ConfigurationManager.");
            ex.printStackTrace();
        }

        ClientConfig clientConfig = RestClient.configureClient();
        client = ClientBuilder.newClient(clientConfig);

        target = client.target(ConfigurationManager.INSTANCE.getDdsServer().getUrl());
    }

    public void shutdown() throws InterruptedException {
        ConfigurationManager.INSTANCE.shutdown();
        client.close();
    }

    public Client getClient() {
        return client;
    }

    /**
     * @return the target
     */
    public WebTarget getTarget() {
        return target;
    }
}

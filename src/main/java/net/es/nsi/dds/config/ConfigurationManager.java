package net.es.nsi.dds.config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import net.es.nsi.dds.provider.DiscoveryProvider;
import net.es.nsi.dds.server.DdsServer;
import net.es.nsi.dds.spring.SpringContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

/**
 * The Document Discovery Service's Configuration Manager loads initial
 * configuration files and instantiates singletons.  Spring Beans are used
 * to drive initialization and created singletons for the key services.
 * @author hacksaw
 */
public enum ConfigurationManager {
    INSTANCE;

    private static DdsServer ddsServer;
    private static DiscoveryProvider discoveryProvider;
    private static ApplicationContext context;

    private boolean initialized = false;

    private final Logger log = LogManager.getLogger(getClass());

    /**
     * @return the initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @param aInitialized the initialized to set
     */
    public void setInitialized(boolean aInitialized) {
        initialized = aInitialized;
    }

    /**
     * This method initializes the DDS configuration found under the specified
     * configPath.
     *
     * @env configdir The path containing all the needed configuration files.
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws IOException If there is an error loading bean configuration file.
     */
    public synchronized void initialize() throws IOException, IllegalStateException, KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        String configPath = System.getProperty(Properties.SYSTEM_PROPERTY_CONFIGDIR);

        if (!isInitialized()) {
            Path path = Paths.get(configPath, "beans.xml");
            Path realPath;
            try {
                realPath = path.toRealPath();
            } catch (IOException ex) {
                log.error("ConfigurationManager: configuration file not found " + path.toString(), ex);
                throw ex;
            }

            // Initialize the Spring context to load our dependencies.
            SpringContext sc = SpringContext.getInstance();
            context = sc.initContext(realPath.toUri().toString());

            // Get references to the spring controlled beans.
            ddsServer = (DdsServer) context.getBean("ddsServer");

            try {
              ddsServer.start();
            } catch (IOException | IllegalStateException | KeyManagementException | NoSuchAlgorithmException | NoSuchProviderException | KeyStoreException | CertificateException | UnrecoverableKeyException ex) {
              log.error("[ConfigurationManager] initialize() failed to start DDS server", ex);
              throw ex;
            }

            // Start the discovery process.
            discoveryProvider = (DiscoveryProvider) context.getBean("discoveryProvider");
            discoveryProvider.start();

            setInitialized(true);
            log.info("Loaded configuration from: " + path.toString());
        }
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }

    /**
     * @return the ddsServer
     */
    public DdsServer getDdsServer() {
        return ddsServer;
    }

    /**
     * @return the discoveryProvider
     */
    public DiscoveryProvider getDiscoveryProvider() {
        return discoveryProvider;
    }

    public void shutdown() {
        if (discoveryProvider != null) {
            discoveryProvider.shutdown();
        }

        if (ddsServer != null) {
            ddsServer.shutdown();
        }
        initialized = false;
    }
}

package net.es.nsi.dds.config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.es.nsi.dds.spring.SpringContext;
import net.es.nsi.dds.provider.DiscoveryProvider;
import net.es.nsi.dds.server.DdsServer;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * The Path Computation Engine's Configuration Manager loads initial
 * configuration files and instantiates singletons.  Spring Beans are used
 * to drive initialization and created singletons for the key services.
 *
 * HttpConfigProvider - This provider contains configuration for the PCEServer
 * and AggServer.  The PCEServer drives the core logic for path computation and
 * exposes the web services interfaces as an external API.  The AggServer is a
 * test endpoint for receiving PCE results and is not utilized during normal
 * operation.
 *
 * ServiceInfoProvider - This provider loads NSA configuration and security
 * information from a configuration file.  This information is is not used
 * during pat computation, but is exposed through a web service interface for
 * use by the aggregator for peer communications.  ** This functionality should
 * not be part of the PCE and will be moved to a Discovery Service at a later
 * date. **
 *
 * TopologyProvider - This provider loads network topology information used
 * during the path computation process.  Topology is currently loaded from a
 * local configuration file but will be changed in the near future to use an
 * external topology service.
 *
 * PCEScheduler - The scheduling task for PCE functions such as configuration
 * monitoring and other maintenance tasks.
 *
 * @author hacksaw
 */
public enum ConfigurationManager {
    INSTANCE;

    private static DdsServer ddsServer;
    private static DiscoveryProvider discoveryProvider;
    private static ApplicationContext context;

    private boolean initialized = false;

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
     * @throws IOException If there is an error loading bean configuration file.
     */
    public synchronized void initialize() throws IOException {
        String configPath = System.getProperty(Properties.DDS_SYSTEM_PROPERTY_CONFIGDIR);

        if (!isInitialized()) {
            // Load and watch the log4j configuration file for changes.
            DOMConfigurator.configureAndWatch(getLog4jConfig(configPath), 45 * 1000);
            final org.slf4j.Logger log = LoggerFactory.getLogger(ConfigurationManager.class);

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
            ddsServer.start();

            // Start the discovery process.
            discoveryProvider = (DiscoveryProvider) context.getBean("discoveryProvider");
            discoveryProvider.start();

            setInitialized(true);
            log.info("Loaded configuration from: " + path.toString());
        }
    }

     private String getLog4jConfig(String configPath) throws IOException {
        String log4jConfig = System.getProperty("log4j.configuration");
        if (log4jConfig == null) {
            Path realPath = Paths.get(configPath, "log4j.xml").toRealPath();
            log4jConfig = realPath.toString();
        }
        return log4jConfig;
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

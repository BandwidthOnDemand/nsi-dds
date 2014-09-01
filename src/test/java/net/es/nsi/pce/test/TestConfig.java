/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.jersey.RestClient;
import org.glassfish.jersey.client.ClientConfig;

/**
 *
 * @author hacksaw
 */
public class TestConfig {
    private static final String CONFIG_PATH = "configPath";
    private final static String CONFIG_DIR = "src/test/resources/config/";
    private static final String DEFAULT_TOPOLOGY_FILE = CONFIG_DIR + "topology-dds.xml";
    public static final String DEFAULT_DDS_FILE = CONFIG_DIR + "dds.xml";
    private static final String TOPOLOGY_CONFIG_FILE_ARGNAME = "topologyConfigFile";
    private static final String DDS_CONFIG_FILE_ARGNAME = "ddsConfigFile";

    private Client client;
    private WebTarget target;

    public TestConfig() {
        System.setProperty(CONFIG_PATH, CONFIG_DIR);
        System.setProperty(DDS_CONFIG_FILE_ARGNAME, DEFAULT_DDS_FILE);
        System.setProperty(TOPOLOGY_CONFIG_FILE_ARGNAME, DEFAULT_TOPOLOGY_FILE);
        try {
            if (ConfigurationManager.INSTANCE.isInitialized()) {
                System.out.println("TestConfig: ConfigurationManager already initialized so shutting down.");
                ConfigurationManager.INSTANCE.shutdown();
            }
            ConfigurationManager.INSTANCE.initialize(CONFIG_DIR);
        }
        catch (Exception ex) {
            System.err.println("TestConfig: failed to initialize ConfigurationManager.");
            ex.printStackTrace();
        }

        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        client = ClientBuilder.newClient(clientConfig);

        target = client.target(ConfigurationManager.INSTANCE.getPceServer().getUrl());
    }

    public void shutdown() {
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

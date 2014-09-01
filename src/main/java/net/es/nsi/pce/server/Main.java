package net.es.nsi.pce.server;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

import net.es.nsi.pce.config.ConfigurationManager;

/**
 * This is the main execution thread for the PAth Computation Engine.  The
 * runtime configuration is loaded from disk, the HTTP server is started, and
 * a shutdown hook is added to monitor for termination conditions and clean up
 * services.
 *
 * @author hacksaw
 */
public class Main {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

    private static final String CONFIG_PATH = "configPath";
    private static final String CONFIG_DEFAULT_PATH = "config/";
    private static final String DEFAULT_TOPOLOGY_FILE = CONFIG_DEFAULT_PATH + "topology-dds.xml";
    private static final String DEFAULT_DDS_FILE = CONFIG_DEFAULT_PATH + "dds.xml";

    public static final String TOPOLOGY_CONFIG_FILE_ARGNAME = "topologyConfigFile";
    public static final String DDS_CONFIG_FILE_ARGNAME = "ddsConfigFile";

    public static final String PCE_SERVER_CONFIG_NAME = "pce";

    // Keep running PCE while true.
    private static boolean keepRunning = true;

    /**
     * Returns a boolean indicating whether the PCE should continue running
     * (true) or should terminate (false).
     *
     * @return true if the PCE should be running, false otherwise.
     */
    public static boolean isKeepRunning() {
        return keepRunning;
    }

    /**
     * Set whether the PCE should be running (true) or terminated (false).
     *
     * @param keepRunning true if the PCE should be running, false otherwise.
     */
    public static void setKeepRunning(boolean keepRunning) {
        Main.keepRunning = keepRunning;
    }

    /**
     * Main initialization and execution thread for the NSI Path Computation
     * Engine.  Method will loop until a signal is received to shutdown.
     *
     * @param args No arguments are accepted.
     * @throws Exception If anything fails during initialization.
     */
    public static void main(String[] args) throws Exception {
        // Create Options object to hold our command line options.
        Options options = new Options();

        // Configuration directory option.
        Option topologyOption = new Option(TOPOLOGY_CONFIG_FILE_ARGNAME, true, "Path to your topology configuration file");
        topologyOption.setRequired(false);
        options.addOption(topologyOption);

        Option ddsOption = new Option(DDS_CONFIG_FILE_ARGNAME, true, "Path to your DDS configuration file");
        ddsOption.setRequired(false);
        options.addOption(ddsOption);

        Option configOption = new Option("c", true, "Where you keep your other configfiles (defaults to ./config)");
        configOption.setRequired(false);
        options.addOption(configOption);

        // Parse the command line options.
        CommandLineParser parser = new GnuParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("You did not provide the correct arguments, see usage below.");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar pce.jar  [-c <configDir>] [-topologyConfigFile <filename>] [-ddsConfigFile <filename>]", options);
            System.exit(1);
            return;
        }

        String configPath = cmd.getOptionValue("c");
        if(configPath == null) {
            configPath = CONFIG_DEFAULT_PATH;
        }
        else {
            configPath = configPath.trim();

            if (!configPath.endsWith("/")) {
                configPath = configPath + "/";
            }
        }

        System.setProperty(CONFIG_PATH, configPath);

        String topologyFile = cmd.getOptionValue(TOPOLOGY_CONFIG_FILE_ARGNAME);
        if (topologyFile == null || topologyFile.isEmpty()) {
            topologyFile = DEFAULT_TOPOLOGY_FILE;
        }

        System.setProperty(TOPOLOGY_CONFIG_FILE_ARGNAME, topologyFile);

        String ddsFile = cmd.getOptionValue(DDS_CONFIG_FILE_ARGNAME);
        if (ddsFile == null || ddsFile.isEmpty()) {
            ddsFile = DEFAULT_DDS_FILE;
        }

        System.setProperty(DDS_CONFIG_FILE_ARGNAME, ddsFile);

        // Load PCE configuration from disk.
        ConfigurationManager.INSTANCE.initialize(configPath);

        // Listen for a shutdown event so we can clean up.
        Runtime.getRuntime().addShutdownHook(
            new Thread() {
                @Override
                public void run() {
                    log.info("Shutting down PCE...");
                    PCEServer.getInstance().shutdown();
                    log.info("...Shutdown complete.");
                    Main.setKeepRunning(false);
                }
            }
        );

        // Loop until we are told to shutdown.
        while (keepRunning) {
            Thread.sleep(1000);
        }
    }
}

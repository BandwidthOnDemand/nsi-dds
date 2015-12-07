package net.es.nsi.dds.server;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

import net.es.nsi.dds.config.ConfigurationManager;
import net.es.nsi.dds.config.Properties;

/**
 * This is the main execution thread for the NSI Document Distribution Service.
 * The runtime configuration is loaded from disk, the HTTP server is started, and
 * a shutdown hook is added to monitor for termination conditions and clean up
 * services.
 *
 * @author hacksaw
 */
public class Main {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

    // Command line arguments.
    public static final String DDS_ARGNAME_BASEDIR = "base";
    public static final String DDS_ARGNAME_CONFIGDIR = "config";
    public static final String DDS_ARGNAME_CONFIGFILE = "ddsConfigFile";

    // Default properties.
    private static final String DEFAULT_CONFIGDIR = "config/";
    private static final String DEFAULT_DDS_FILE = "dds.xml";

    public static final String PCE_SERVER_CONFIG_NAME = "dds";

    // Keep running PCE while true.
    private static boolean keepRunning = true;

    /**
     * Main initialization and execution thread for the NSI Document
     * Distribution Service.  Method will loop until a signal is received to
     * shutdown.
     *
     * @param args Runtime configuration parameters can be passed on the command
     * line or via system properties.
     * @throws Exception If anything fails during initialization.
     */
    public static void main(String[] args) throws Exception {
        // Load the command line options into appropriate system properties.
        try {
            processOptions(args);
        }
        catch (ParseException ex) {
            System.err.println("Failed to load command line options: " + ex.getMessage());
            System.exit(1);
            return;
        }

        // Initialize DDS runtime.
        ConfigurationManager.INSTANCE.initialize();

        // Listen for a shutdown event so we can clean up.
        Runtime.getRuntime().addShutdownHook(
            new Thread() {
                @Override
                public void run() {
                    log.info("Shutting down PCE...");
                    DdsServer.getInstance().shutdown();
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

    /**
     * Process any command line arguments and set up associated system
     * properties for runtime components.
     *
     * @param args
     * @throws ParseException
     * @throws IOException
     */
    private static void processOptions(String[] args) throws ParseException, IOException {
        // Parse the command line options.
        CommandLineParser parser = new GnuParser();

        Options options = getOptions();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("You did not provide the correct arguments, see usage below.");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar dds.jar [-base <application directory>] [-config <configDir>] [-ddsConfigFile <filename>]", options);
            throw e;
        }

        // Get the application base directory.
        String basedir = getBasedir(cmd);
        System.setProperty(Properties.SYSTEM_PROPERTY_BASEDIR, basedir);

        // Now for the configuration directory path.
        String configdir = getConfigdir(cmd, basedir);
        System.setProperty(Properties.SYSTEM_PROPERTY_CONFIGDIR, configdir);

        // See if the user overrode the default location of the dds configuration file.
        String ddsFile = getConfigFile(cmd, configdir);
        System.setProperty(Properties.SYSTEM_PROPERTY_CONFIGFILE, ddsFile);

    }

    /**
     * Build supported command line options for parsing of parameter input.
     *
     * @return List of supported command line options.
     */
    private static Options getOptions() {
        // Create Options object to hold our command line options.
        Options options = new Options();

        Option basedirOption = new Option(DDS_ARGNAME_BASEDIR, true, "The runtime home directory for the application (defaults to \"user.dir\")");
        basedirOption.setRequired(false);
        options.addOption(basedirOption);

        Option ddsOption = new Option(DDS_ARGNAME_CONFIGFILE, true, "Path to your DDS configuration file");
        ddsOption.setRequired(false);
        options.addOption(ddsOption);

        Option configOption = new Option(DDS_ARGNAME_CONFIGDIR, true, "DDS configuration files (defaults to ./config)");
        configOption.setRequired(false);
        options.addOption(configOption);
        return options;
    }

    /**
     * Processes the "basedir" command line and system property option.
     *
     * @param cmd Commands entered by the user.
     * @return The configured basedir.
     * @throws IOException
     */
    private static String getBasedir(CommandLine cmd) throws IOException {
        // Get the application base directory.
        String basedir = System.getProperty(Properties.SYSTEM_PROPERTY_BASEDIR);
        basedir = cmd.getOptionValue(DDS_ARGNAME_BASEDIR, basedir);
        if(basedir == null || basedir.isEmpty()) {
            basedir = System.getProperty("user.dir");
        }

        try {
            basedir = Paths.get(basedir).toRealPath().toString();
        } catch (IOException ex) {
            log.error("Base directory not found " + basedir, ex);
            throw ex;
        }

        return basedir;
    }

    /**
     * Processes the "configdir" command line and system property option.
     * @param cmd Commands entered by the user.
     * @param basedir The base directory for the application (install directory).
     * @return The configured configdir.
     * @throws IOException
     */
    private static String getConfigdir(CommandLine cmd, String basedir) throws IOException {
        String configdir = System.getProperty(Properties.SYSTEM_PROPERTY_CONFIGDIR);
        configdir = cmd.getOptionValue(DDS_ARGNAME_CONFIGDIR, configdir);
        Path configPath;
        if(configdir == null || configdir.isEmpty()) {
            configPath = Paths.get(basedir, DEFAULT_CONFIGDIR);
        }
        else {
            configPath = Paths.get(configdir);
        }

        try {
            configdir = configPath.toRealPath().toString();
        } catch (IOException ex) {
            log.error("Configuration directory not found " + configdir, ex);
            throw ex;
        }

        return configdir;
    }

    /**
     * Processes the "ddsConfigFile" command line and system property option.
     * @param cmd Commands entered by the user.
     * @param configdir The application configuration directory.
     * @return The configuration file path.
     * @throws IOException
     */
    private static String getConfigFile(CommandLine cmd, String configdir) throws IOException {
        String ddsFile = System.getProperty(Properties.SYSTEM_PROPERTY_CONFIGFILE);
        ddsFile = cmd.getOptionValue(DDS_ARGNAME_CONFIGFILE, ddsFile);
        Path ddsPath;
        if (ddsFile == null || ddsFile.isEmpty()) {
            ddsPath = Paths.get(configdir, DEFAULT_DDS_FILE);
        }
        else {
            ddsPath = Paths.get(ddsFile);
        }

        try {
            ddsFile = ddsPath.toRealPath().toString();
        } catch (IOException ex) {
            log.error("DDS configuration file not found " + ddsFile, ex);
            throw ex;
        }

        return ddsFile;
    }

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
}

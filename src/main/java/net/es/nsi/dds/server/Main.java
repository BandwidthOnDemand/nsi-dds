package net.es.nsi.dds.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.config.ConfigurationManager;
import net.es.nsi.dds.config.Properties;
import org.apache.commons.cli.*;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * This is the main execution thread for the NSI Document Distribution Service. The runtime configuration is loaded from
 * disk, the HTTP server is started, and a shutdown hook is added to monitor for termination conditions and clean up
 * services.
 *
 * @author hacksaw
 */
public class Main extends ResourceConfig {
  // Command line arguments.

  public static final String DDS_ARGNAME_BASEDIR = "base";
  public static final String DDS_ARGNAME_CONFIGDIR = "config";
  public static final String DDS_ARGNAME_CONFIGFILE = "ddsConfigFile";
  public static final String DDS_ARGNAME_PIDFILE = "pidFile";

  // Default properties.
  private static final String DEFAULT_CONFIGDIR = "config/";
  private static final String DEFAULT_DDS_FILE = "dds.xml";

  // Keep running DDS while true.
  private static boolean keepRunning = true;

  /**
   * Main initialization and execution thread for the NSI Document Distribution Service. Method will loop until a signal
   * is received to shutdown.
   *
   * @param args Runtime configuration parameters can be passed on the command line or via system properties.
   * @throws Exception If anything fails during initialization.
   */
  public static void main(String[] args) throws Exception {
    // Load the command line options into appropriate system properties.
    try {
      processOptions(args);
    } catch (ParseException ex) {
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
        System.out.println("Shutting down DDS...");
        DdsServer.getInstance().shutdown();
        System.out.println("...Shutdown complete.");
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
   * Process any command line arguments and set up associated system properties for runtime components.
   *
   * @param args
   * @throws ParseException
   * @throws IOException
   */
  private static void processOptions(String[] args) throws ParseException, IOException {
    // Parse the command line options.
    CommandLineParser parser = new DefaultParser();

    Options options = getOptions();
    CommandLine cmd;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.err.println("Error: You did not provide the correct arguments, see usage below.");
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java -jar dds.jar [-base <application directory>] [-config <configDir>] [-ddsConfigFile <filename>] [-pidFile <filename>]", options);
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

    // Write the process id out to file if specified.
    processPidFile(cmd);
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

    Option pidFileOption = new Option(DDS_ARGNAME_PIDFILE, true, "The file in which to write the process pid");
    pidFileOption.setRequired(false);
    options.addOption(pidFileOption);
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
    if (basedir == null || basedir.isEmpty()) {
      basedir = System.getProperty("user.dir");
    }

    try {
      basedir = Paths.get(basedir).toRealPath().toString();
    } catch (IOException ex) {
      System.err.printf("Error: Base directory not found %s, ex = %s\n", basedir, ex);
      throw ex;
    }

    return basedir;
  }

  /**
   * Processes the "configdir" command line and system property option.
   *
   * @param cmd Commands entered by the user.
   * @param basedir The base directory for the application (install directory).
   * @return The configured configdir.
   * @throws IOException
   */
  private static String getConfigdir(CommandLine cmd, String basedir) throws IOException {
    String configdir = System.getProperty(Properties.SYSTEM_PROPERTY_CONFIGDIR);
    configdir = cmd.getOptionValue(DDS_ARGNAME_CONFIGDIR, configdir);
    Path configPath;
    if (configdir == null || configdir.isEmpty()) {
      configPath = Paths.get(basedir, DEFAULT_CONFIGDIR);
    } else {
      configPath = Paths.get(configdir);
    }

    try {
      configdir = configPath.toRealPath().toString();
    } catch (IOException ex) {
      System.err.printf("Error: Configuration directory not found %s, ex = %s\n", configdir, ex);
      throw ex;
    }

    return configdir;
  }

  /**
   * Processes the "ddsConfigFile" command line and system property option.
   *
   * @param cmd Commands entered by the user.
   * @param configdir The application configuration directory.
   * @return The configuration file path.
   * @throws IOException
   */
  private static String getConfigFile(CommandLine cmd, String configdir) throws IOException {
    String ddsFile = cmd.getOptionValue(DDS_ARGNAME_CONFIGFILE,
        System.getProperty(Properties.SYSTEM_PROPERTY_CONFIGFILE));
    Path ddsPath;
    if (ddsFile == null || ddsFile.isEmpty()) {
      ddsPath = Paths.get(configdir, DEFAULT_DDS_FILE);
    } else {
      ddsPath = Paths.get(ddsFile);
    }

    String result;
    try {
      result = ddsPath.toRealPath().toString();
    } catch (IOException ex) {
      System.err.printf("Error: DDS configuration file not found %s, ex = %s\n", ddsFile, ex);
      throw ex;
    }

    return result;
  }

  /**
   * Processes the "pidFile" command line and system property option.
   *
   * @param cmd Commands entered by the user.
   * @return The specified pidFile.
   * @throws IOException
   */
  private static void processPidFile(CommandLine cmd) throws IOException {
    // Get the application base directory.
    String pidFile = cmd.getOptionValue(DDS_ARGNAME_PIDFILE, System.getProperty(Properties.SYSTEM_PROPERTY_PIDFILE));
    pidFile = cmd.getOptionValue(DDS_ARGNAME_PIDFILE, pidFile);
    long pid = ProcessHandle.current().pid();
    if (pidFile == null || pidFile.isEmpty() || pid == -1) {
      return;
    }

    BufferedWriter out = null;
    try {
      FileWriter fstream = new FileWriter(pidFile, false);
      out = new BufferedWriter(fstream);
      out.write(String.valueOf(pid));
    } catch (IOException e) {
      System.err.printf("Error: %s\n", e.getMessage());
    } finally {
      if (out != null) {
        out.close();
      }
    }

    return;
  }

  /**
   * Returns a boolean indicating whether the DDS should continue running (true) or should terminate (false).
   *
   * @return true if the DDS should be running, false otherwise.
   */
  public static boolean isKeepRunning() {
    return keepRunning;
  }

  /**
   * Set whether the DDS should be running (true) or terminated (false).
   *
   * @param keepRunning true if the DDS should be running, false otherwise.
   */
  public static void setKeepRunning(boolean keepRunning) {
    Main.keepRunning = keepRunning;
  }
}

package net.es.nsi.dds.gangofthree;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import net.es.nsi.dds.client.TestServer;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.discovery.FileUtilities;
import net.es.nsi.dds.test.TestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

public class StressTest {

  private final static HttpConfig testServer = new HttpConfig("localhost", "8402", "net.es.nsi.dds.client");

  private final static String DDS_CONFIGURATION = "src/test/resources/config/dds.xml";
  private final static String DOCUMENT_DIR = "src/test/resources/documents/";
  private static DdsConfiguration ddsConfig;
  private static TestConfig testConfig;
  private static WebTarget target;
  private static WebTarget discovery;
  private static String callbackURL;
  private static Logger log;

  @BeforeClass
  public static void oneTimeSetUp() {
    System.setProperty(Properties.SYSTEM_PROPERTY_LOG4J, "src/test/resources/config/log4j.xml");
    log = LogManager.getLogger(StressTest.class);

    log.debug("*************************************** StressTest oneTimeSetUp ***********************************");

    try {
      // Load a copy of the test DDS configuration and clear the document
      // repository for this test.
      ddsConfig = new DdsConfiguration();
      ddsConfig.setFilename(DDS_CONFIGURATION);
      ddsConfig.load();
      File directory = new File(ddsConfig.getRepository());
      FileUtilities.deleteDirectory(directory);

      // Configure the local test client callback server.
      TestServer.INSTANCE.start(testServer);

      callbackURL = new URL(testServer.getURL(), "dds/callback").toString();
    } catch (IllegalArgumentException | JAXBException | IOException | IllegalStateException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
      log.error("oneTimeSetUp: failed to start HTTP server " + ex.getLocalizedMessage());
      fail();
    }

    testConfig = new TestConfig();
    target = testConfig.getTarget();
    discovery = target.path("dds");
    log.debug("*************************************** StressTest oneTimeSetUp done ***********************************");
  }

  @AfterClass
  public static void oneTimeTearDown() {
    log.debug("*************************************** StressTest oneTimeTearDown ***********************************");
    testConfig.shutdown();
    try {
      TestServer.INSTANCE.shutdown();
    } catch (Exception ex) {
      log.error("oneTimeTearDown: test server shutdown failed." + ex.getLocalizedMessage());
      fail();
    }
    log.debug("*************************************** StressTest oneTimeTearDown done ***********************************");
  }

  @Test
  public void errorTest() {
    log.debug("*************************************** errorTest ***********************************");
    // Simple ping to determine if interface is available.
    for (int i = 0; i < 2000; i++) {
      Response response = discovery.path("error").request(MediaType.APPLICATION_XML).get();
      //assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
  }
}

package net.es.nsi.dds.jaxb;

import java.util.List;
import net.es.nsi.dds.jaxb.configuration.AccessControlType;
import net.es.nsi.dds.jaxb.configuration.ClientType;
import net.es.nsi.dds.jaxb.configuration.DdsConfigurationType;
import net.es.nsi.dds.jaxb.configuration.PeerURLType;
import net.es.nsi.dds.jaxb.configuration.SecureType;
import net.es.nsi.dds.jaxb.configuration.ServerType;
import net.es.nsi.dds.jaxb.configuration.StaticType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class ConfigurationParserTest {
  private static final Logger log = LogManager.getLogger("ConfigurationParserTest");

  public ConfigurationParserTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of getInstance method, of class ConfigurationParser.
   */
  @Test
  public void testGetInstance() {
    log.debug("ConfigurationParserTest: getInstance()");
    ConfigurationParser result = ConfigurationParser.getInstance();
    assertNotNull(result);
  }

  /**
   * Test of readConfiguration method, of class ConfigurationParser.
   */
  @Test
  public void testReadConfiguration() throws Exception {
    log.debug("ConfigurationParserTest: readConfiguration start");

    String filename = "src/test/resources/config/ConfigurationParserTest.xml";
    ConfigurationParser parser = ConfigurationParser.getInstance();
    assertNotNull(parser);
    DdsConfigurationType result = parser.readConfiguration(filename);
    assertNotNull(result);

    // Verify some of the expected fields.

    // <nsaId>urn:ogf:network:netherlight.net:2013:nsa:bod</nsaId>
    // <documents>src/test/resources/config/documents</documents>
    // <cache>src/test/resources/config/cache</cache>
    // <repository>src/test/resources/config/repository</repository>
    // <expiryInterval>600</expiryInterval>
    // <baseURL>http://localhost:8801/dds</baseURL>
    assertEquals("urn:ogf:network:netherlight.net:2013:nsa:bod", result.getNsaId());
    assertEquals("src/test/resources/config/documents", result.getDocuments());
    assertEquals("src/test/resources/config/repository", result.getRepository());
    assertEquals(600L, result.getExpiryInterval());
    assertEquals("http://localhost:8801/dds", result.getBaseURL());

    // <server address="localhost" port="8801" packageName="net.es.nsi.dds" secure="true">
    //    <static>
    //        <path>src/test/resources/config/www</path>
    //        <relative>/www</relative>
    //    </static>
    // </server>
    ServerType server = result.getServer();
    assertNotNull(server);
    assertEquals("localhost", server.getAddress());
    assertEquals("8801", server.getPort());
    assertTrue(server.isSecure());
    StaticType st = server.getStatic();
    assertNotNull(st);
    assertEquals("src/test/resources/config/www", st.getPath());
    assertEquals("/www", st.getRelative());

    // <client maxConnPerRoute="10" maxConnTotal="60" secure="true" />
    ClientType client = result.getClient();
    assertNotNull(client);
    assertEquals(10, client.getMaxConnPerRoute());
    assertEquals(60, client.getMaxConnTotal());
    assertTrue(client.isSecure());

    // <secure production="true">
    //  <keyStore type="JKS">
    //    <file>src/test/resources/config/server.jks</file>
    //    <password>changeit</password>
    //  </keyStore>
    //  <trustStore type="JKS">
    //    <file>src/test/resources/config/truststore.jks</file>
    //    <password>changeit</password>
    //  </trustStore>
    // </secure>
    SecureType secure = result.getSecure();
    assertNotNull(secure);
    assertTrue(secure.isProduction());

    assertNotNull(secure.getKeyStore());
    assertEquals("JKS", secure.getKeyStore().getType());
    assertEquals("src/test/resources/config/server.jks", secure.getKeyStore().getFile());
    assertEquals("changeit", secure.getKeyStore().getPassword());

    assertNotNull(secure.getTrustStore());
    assertEquals("JKS", secure.getTrustStore().getType());
    assertEquals("src/test/resources/config/truststore.jks", secure.getTrustStore().getFile());
    assertEquals("changeit", secure.getTrustStore().getPassword());

    //<accessControl enabled="true"> ... </accessControl>
    AccessControlType ac = result.getAccessControl();
    assertNotNull(ac);
    assertTrue(ac.isEnabled());

    // <peerURL type="application/vnd.ogf.nsi.dds.v1+xml">http://localhost:8801/dds</peerURL>
    List<PeerURLType> peerURL = result.getPeerURL();
    assertNotNull(peerURL);
    boolean found = false;
    for (PeerURLType url : peerURL) {
      if ("application/vnd.ogf.nsi.dds.v1+xml".equals(url.getType()) &&
              "http://localhost:8801/dds".equals(url.getValue())) {
        found = true;
      }
    }
    assertTrue(found);
    log.debug("ConfigurationParserTest: readConfiguration done");
  }

  /**
   * Test of writeConfiguration method, of class ConfigurationParser.
   */
  @Test
  public void testWriteConfiguration() throws Exception {
    log.debug("ConfigurationParserTest: writeConfiguration start");

    // Read our original test data file.
    ConfigurationParser parser = ConfigurationParser.getInstance();
    assertNotNull(parser);
    DdsConfigurationType result = parser.readConfiguration("src/test/resources/config/ConfigurationParserTest.xml");
    assertNotNull(result);

    // Modify some content.
    result.setNsaId("new NSA id");
    result.getSecure().setProduction(Boolean.FALSE);

    // Now write this out to disk.
    parser.writeConfiguration("/tmp/ConfigurationParserTest.xml", result);

    // Now read it back in.
    DdsConfigurationType newResult = parser.readConfiguration("/tmp/ConfigurationParserTest.xml");
    assertNotNull(newResult);

    assertEquals("new NSA id", newResult.getNsaId());
    assertFalse(newResult.getSecure().isProduction());

    log.debug("ConfigurationParserTest: writeConfiguration start");
  }

}

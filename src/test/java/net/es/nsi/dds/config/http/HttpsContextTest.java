package net.es.nsi.dds.config.http;

import javax.net.ssl.SSLContext;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.jaxb.configuration.KeyStoreType;
import net.es.nsi.dds.jaxb.configuration.SecureType;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 *
 * @author hacksaw
 */
@Slf4j
public class HttpsContextTest {

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  public SecureType getConfigJKS() {
    SecureType config = new SecureType();

    KeyStoreType keystore = new KeyStoreType();
    keystore.setFile("src/test/resources/config/server.jks");
    keystore.setPassword("changeit");
    keystore.setType("JKS");

    KeyStoreType truststore = new KeyStoreType();
    truststore.setFile("src/test/resources/config/truststore.jks");
    truststore.setPassword("changeit");
    truststore.setType("JKS");

    config.setProduction(Boolean.TRUE);
    config.setKeyStore(keystore);
    config.setTrustStore(truststore);

    return config;
  }

  public SecureType getConfigP12() {
    SecureType config = new SecureType();

    KeyStoreType keystore = new KeyStoreType();
    keystore.setFile("src/test/resources/config/server.p12");
    keystore.setPassword("changeit");
    keystore.setType("PKCS12");

    KeyStoreType truststore = new KeyStoreType();
    truststore.setFile("src/test/resources/config/truststore.p12");
    truststore.setPassword("changeit");
    truststore.setType("PKCS12");

    config.setProduction(Boolean.TRUE);
    config.setKeyStore(keystore);
    config.setTrustStore(truststore);

    return config;
  }

  @Test
  public void testJKS() throws Exception {
    log.debug("HttpsContextTest: testJKS start");

    SecureType config = getConfigJKS();
    HttpsContext https = HttpsContext.getInstance();
    assertNotNull(https);
    https.load(config);
    assertTrue(https.isProduction());
    SSLContext sslContext = https.getSSLContext();
    assertNotNull(sslContext);
    assertEquals(sslContext.getProtocol(), "TLS");

    log.debug("HttpsContextTest: testJKS done");
  }

  @Test
  public void testP12() throws Exception {
    log.debug("HttpsContextTest: testP12 start");

    SecureType config = getConfigP12();
    HttpsContext https = HttpsContext.getInstance();
    assertNotNull(https);
    https.load(config);
    assertTrue(https.isProduction());
    SSLContext sslContext = https.getSSLContext();
    assertNotNull(sslContext);
    assertEquals(sslContext.getProtocol(), "TLS");

    log.debug("HttpsContextTest: testP12 done");
  }
}

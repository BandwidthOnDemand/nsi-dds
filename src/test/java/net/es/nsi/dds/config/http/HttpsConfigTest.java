package net.es.nsi.dds.config.http;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import net.es.nsi.dds.jaxb.configuration.KeyStoreType;
import net.es.nsi.dds.jaxb.configuration.SecureType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 *
 * @author hacksaw
 */
public class HttpsConfigTest {
  private static final Logger log = LogManager.getLogger(HttpsConfigTest.class);

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
    config.setMaxConnPerRoute(10);
    config.setMaxConnTotal(100);
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
    config.setMaxConnPerRoute(10);
    config.setMaxConnTotal(100);
    config.setKeyStore(keystore);
    config.setTrustStore(truststore);

    return config;
  }

  @Test
  public void testJKS() throws KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {
    log.debug("******* Running testJKS *******");

    SecureType config = getConfigJKS();
    HttpsConfig https = new HttpsConfig(config);
    assertNotNull(https);
    https.getSSLContext();
    log.debug("******* Done testJKS *******");
  }

  @Test
  public void testP12() throws KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {
    log.debug("******* Running testP12 *******");

    SecureType config = getConfigP12();
    HttpsConfig https = new HttpsConfig(config);
    assertNotNull(https);
    https.getSSLContext();

    log.debug("******* Done testP12 *******");
  }
}

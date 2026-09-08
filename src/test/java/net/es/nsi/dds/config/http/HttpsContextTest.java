package net.es.nsi.dds.config.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.jaxb.configuration.KeyStoreType;
import net.es.nsi.dds.jaxb.configuration.SecureType;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

  /**
   * The BouncyCastle crypto provider ("BC") is required for certificate and DN handling in the
   * authorization code.  The BouncyCastle JSSE provider ("BCJSSE") was a 2021 workaround for a
   * CentOS SecureRandom failure and has been removed along with the bctls dependency.
   */
  @Test
  public void testSecurityProviders() throws Exception {
    HttpsContext.getInstance().load(getConfigJKS());

    assertNotNull("BouncyCastle crypto provider must stay registered", Security.getProvider("BC"));
    assertNull("BCJSSE must not be registered", Security.getProvider("BCJSSE"));
  }

  /**
   * TLS must be served by the JDK provider now that BCJSSE is gone.
   */
  @Test
  public void testSslContextServedByJdk() throws Exception {
    HttpsContext https = HttpsContext.getInstance();
    https.load(getConfigJKS());

    String provider = https.getSSLContext().getProvider().getName();
    log.debug("HttpsContextTest: SSLContext provider {}", provider);
    assertFalse("SSLContext must not come from BouncyCastle", provider.startsWith("BC"));
  }

  /**
   * Complete a real handshake against the configured context to prove the keystore still works
   * without BCJSSE.  The client trusts everything because the test truststore holds only the
   * client certificate; the server side is what is under test here.
   */
  @Test
  public void testHandshake() throws Exception {
    log.debug("HttpsContextTest: testHandshake start");

    HttpsContext https = HttpsContext.getInstance();
    https.load(getConfigJKS());

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (SSLServerSocket server = (SSLServerSocket) https.getSSLContext().getServerSocketFactory()
            .createServerSocket(0, 1, InetAddress.getLoopbackAddress())) {

      Future<Integer> served = executor.submit((Callable<Integer>) () -> {
        try (SSLSocket accepted = (SSLSocket) server.accept();
                InputStream in = accepted.getInputStream();
                OutputStream out = accepted.getOutputStream()) {
          int received = in.read();
          out.write(received);
          out.flush();
          return received;
        }
      });

      try (SSLSocket client = (SSLSocket) trustAllContext().getSocketFactory()
              .createSocket(server.getInetAddress(), server.getLocalPort());
              OutputStream out = client.getOutputStream();
              InputStream in = client.getInputStream()) {
        client.startHandshake();
        assertNotNull(client.getSession().getCipherSuite());

        out.write(42);
        out.flush();
        assertEquals("echoed byte", 42, in.read());
      }

      assertEquals("server received byte", Integer.valueOf(42), served.get(10, TimeUnit.SECONDS));
    } finally {
      executor.shutdownNow();
    }

    log.debug("HttpsContextTest: testHandshake done");
  }

  /**
   * Client-side context that accepts any server certificate.
   */
  private static SSLContext trustAllContext() throws Exception {
    TrustManager trustAll = new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    };

    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(null, new TrustManager[] {trustAll}, null);
    return ctx;
  }
}

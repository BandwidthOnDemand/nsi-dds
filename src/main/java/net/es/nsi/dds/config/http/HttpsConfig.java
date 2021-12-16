package net.es.nsi.dds.config.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.jaxb.configuration.KeyStoreType;
import net.es.nsi.dds.jaxb.configuration.ObjectFactory;
import net.es.nsi.dds.jaxb.configuration.SecureType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.glassfish.jersey.SslConfigurator;

/**
 * Convert security configuration from jaxb configuration file type to something usable by system.
 *
 * @author hacksaw
 */
public class HttpsConfig {

  private final Logger log = LogManager.getLogger(getClass());
  private final ObjectFactory factory = new ObjectFactory();

  private String basedir;
  private SecureType config;

  /**
   * Convert provided jaxb security configuration object into an HttpConfig object.
   *
   * @param config
   * @throws IOException
   */
  public HttpsConfig(SecureType config) throws IOException {
    if (config == null) {
      throw new IllegalArgumentException("HttpConfig: server configuration not provided");
    }

    Security.addProvider(new BouncyCastleJsseProvider());

    // We will use the application basedir to fully qualify any relative paths.
    basedir = System.getProperty(Properties.SYSTEM_PROPERTY_BASEDIR);

    // Determine the keystore configuration.
    KeyStoreType keyStore = config.getKeyStore();
    if (keyStore == null) {
      // Check to see if the keystore was provided on the commandline.
      keyStore = factory.createKeyStoreType();
      keyStore.setFile(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE, Properties.DEFAULT_SSL_KEYSTORE));
      keyStore.setPassword(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD, Properties.DEFAULT_SSL_KEYSTORE_PASSWORD));
      keyStore.setType(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE, Properties.DEFAULT_SSL_KEYSTORE_TYPE));
    }

    keyStore.setFile(getAbsolutePath(keyStore.getFile()));

    KeyStoreType trustStore = config.getTrustStore();
    if (trustStore == null) {
      trustStore = factory.createKeyStoreType();
      trustStore.setFile(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE, Properties.DEFAULT_SSL_TRUSTSTORE));
      trustStore.setPassword(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_PASSWORD, Properties.DEFAULT_SSL_TRUSTSTORE_PASSWORD));
      trustStore.setType(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_TYPE, Properties.DEFAULT_SSL_TRUSTSTORE_TYPE));
    }

    trustStore.setFile(getAbsolutePath(trustStore.getFile()));

    this.config = config;
  }

  /**
   * Get the absolute path for inPath.
   *
   * @param inPath
   * @return
   * @throws IOException
   */
  private String getAbsolutePath(String inPath) throws IOException {
    Path outPath = Paths.get(inPath);
    if (!outPath.isAbsolute()) {
      outPath = Paths.get(basedir, inPath);
    }

    return outPath.toRealPath().toString();
  }

  /**
   * Get the default SSL context and add our specific configuration.Question: Do we really need this? Should we not let JVM parameters control this? If so we could remove all SSL
 configuration from the application.
   *
   *
   * @return New SSLContext for HTTP client.
   * @throws java.security.KeyManagementException
   * @throws java.security.NoSuchAlgorithmException
   * @throws java.security.NoSuchProviderException
   * @throws java.security.KeyStoreException
   * @throws java.io.IOException
   * @throws java.security.cert.CertificateException
   * @throws java.security.UnrecoverableKeyException
   */
  public SSLContext getSSLContext() throws KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException,
          KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {

    // Log what security providers are available to us.
    for (Provider provider : Security.getProviders()) {
      log.debug("getSSLContext: Provider - {}, {}", provider.getName(), provider.getInfo());
    }

    dumpSSLContext("getSSLContext: defaultContext", SslConfigurator.getDefaultContext());

    try {
      SSLContext sslContext = SSLContext.getInstance("TLS", "BCJSSE");

      KeyManagerFactory keyMgrFact = KeyManagerFactory.getInstance("PKIX", "BCJSSE");
      keyMgrFact.init(getKeyStore(config.getKeyStore()), config.getKeyStore().getPassword().toCharArray());

      TrustManagerFactory trustMgrFact = TrustManagerFactory.getInstance("PKIX", "BCJSSE");
      trustMgrFact.init(getKeyStore(config.getTrustStore()));

      sslContext.init(keyMgrFact.getKeyManagers(), trustMgrFact.getTrustManagers(), SecureRandom.getInstanceStrong());

      dumpSSLContext("getSSLContext: BCJSSE", sslContext);
      return sslContext;
    } catch (KeyManagementException | NoSuchAlgorithmException | NoSuchProviderException | KeyStoreException | IOException | CertificateException | UnrecoverableKeyException ex) {
      log.error("getSSLContext: could not find SSL provider", ex);
      throw ex;
    }

    /*
    SslConfigurator sslConfig = SslConfigurator.newInstance(true)
            .trustStoreFile(config.getTrustStore().getFile())
            .trustStorePassword(config.getTrustStore().getPassword())
            .trustStoreType(config.getTrustStore().getType())
            .trustManagerFactoryProvider(defaultContext.getProvider().getName())
            .keyStoreFile(config.getKeyStore().getFile())
            .keyPassword(config.getKeyStore().getPassword())
            .keyStoreType(config.getKeyStore().getType())
            .keyManagerFactoryProvider(defaultContext.getProvider().getName())
            .securityProtocol("TLS");
    SSLContext newContext = sslConfig.createSSLContext();
    dumpSSLContext("newContext", newContext);
    return newContext;
    return SslConfigurator.getDefaultContext();*/
  }

  public KeyStore getKeyStore(KeyStoreType ks) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    // Open specified keystore.
    File file = new File(ks.getFile());
    InputStream stream = new FileInputStream(file);
    KeyStore keyStore = KeyStore.getInstance(ks.getType());
    keyStore.load(stream, ks.getPassword().toCharArray());
    return keyStore;
  }

  public void dumpSSLContext(String prefix, SSLContext c) {
    log.debug("{} - Default provider = {}, {}", prefix, c.getProvider().getName(), c.getProvider().getInfo());

    SSLParameters supportedSSLParameters = c.getSupportedSSLParameters();
    String[] cipherSuites = supportedSSLParameters.getCipherSuites();
    for (String cipher : cipherSuites) {
      log.debug("{} - default cipher = {}", prefix, cipher);
    }

    for (String proto : supportedSSLParameters.getApplicationProtocols()) {
      log.debug("{} - application protocol = {}", prefix, proto);
    }
  }

  /**
   * Is this server configured for production?
   *
   * @return true if configured for production.
   */
  public boolean isProduction() {
    return config.isProduction();
  }

  /**
   * Get maximum connections per destination.
   *
   * @return
   */
  public int getMaxConnPerRoute() {
    return config.getMaxConnPerRoute();
  }

  /**
   * Get total number of connections across all destinations.
   *
   * @return
   */
  public int getMaxConnTotal() {
    return config.getMaxConnTotal();
  }
}

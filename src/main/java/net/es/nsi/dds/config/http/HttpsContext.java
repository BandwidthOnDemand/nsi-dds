package net.es.nsi.dds.config.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.glassfish.jersey.SslConfigurator;

/**
 * A singleton to manage the HTTPS security context.
 *
 * @author hacksaw
 */
public enum HttpsContext {
  INSTANCE;

  private final Logger log = LogManager.getLogger(getClass());
  private final ObjectFactory factory = new ObjectFactory();

  private SSLContext sslContext;
  private boolean isProduction;

  /**
   * Construct an HttpConfig object.
   */
  private HttpsContext() {
    log.debug("[HttpsContext]: constructor invoked");

    // If the BouncyCastle provider is not register add it in.
    if (Security.getProvider("BC") == null) {
      log.debug("Adding BouncyCastleProvider provider");
      Security.addProvider(new BouncyCastleProvider());
    }

    // If the BouncyCastle JSSE provider is not register add it in.
    if (Security.getProvider("BCJSSE") == null) {
      log.debug("Adding BouncyCastleJsseProvider provider");
      Security.addProvider(new BouncyCastleJsseProvider());
    }

    log.debug("[HttpsContext]: constructor complete");
  }

  /**
   *
   * @return
   */
  public static HttpsContext getInstance() {
    return INSTANCE;
  }

  /**
   * Convert security configuration from jaxb configuration file type to something usable by system.
   *
   * @param config
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchProviderException
   * @throws KeyStoreException
   * @throws IOException
   * @throws CertificateException
   * @throws UnrecoverableKeyException
   */
  public synchronized void load(SecureType config) throws KeyManagementException, NoSuchAlgorithmException,
          NoSuchProviderException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {

    log.debug("[HttpsContext].load invoked");

    if (config == null) {
      throw new IllegalArgumentException("[HttpsContext].load: server configuration not provided");
    }

    // We will use the application basedir to fully qualify any relative paths.
    String basedir = System.getProperty(Properties.SYSTEM_PROPERTY_BASEDIR);

    // Determine the keystore configuration.
    KeyStoreType keyStore = config.getKeyStore();
    if (keyStore == null) {
      // Check to see if the keystore was provided on the commandline.
      keyStore = factory.createKeyStoreType();
      keyStore.setFile(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE, Properties.DEFAULT_SSL_KEYSTORE));
      keyStore.setPassword(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD, Properties.DEFAULT_SSL_KEYSTORE_PASSWORD));
      keyStore.setType(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE, Properties.DEFAULT_SSL_KEYSTORE_TYPE));
    }

    keyStore.setFile(getAbsolutePath(basedir, keyStore.getFile()));

    KeyStoreType trustStore = config.getTrustStore();
    if (trustStore == null) {
      trustStore = factory.createKeyStoreType();
      trustStore.setFile(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE, Properties.DEFAULT_SSL_TRUSTSTORE));
      trustStore.setPassword(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_PASSWORD, Properties.DEFAULT_SSL_TRUSTSTORE_PASSWORD));
      trustStore.setType(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_TYPE, Properties.DEFAULT_SSL_TRUSTSTORE_TYPE));
    }

    trustStore.setFile(getAbsolutePath(basedir, trustStore.getFile()));

    sslContext = initContext(config);
    isProduction = config.isProduction();
    log.debug("[HttpsContext].load: done.");
  }

  /**
   * Get the absolute path for inPath.
   *
   * @param basedir
   * @param inPath
   * @return
   * @throws IOException
   */
  private String getAbsolutePath(String basedir, String inPath) throws IOException {
    Path outPath = Paths.get(inPath);
    if (!outPath.isAbsolute()) {
      outPath = Paths.get(basedir, inPath);
    }

    return outPath.toRealPath().toString();
  }

  /**
   * Get the default SSL context and add our specific configuration.
   *
   * Question: Do we really need this? Should we not let JVM parameters
   * control this? If so we could remove all SSL configuration from the
   * application.
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
  private SSLContext initContext(SecureType st) throws KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException,
          KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {

    // Log what security providers are available to us.
    for (Provider provider : Security.getProviders()) {
      log.debug("[HttpsContext].initContext: Provider - {}, {}", provider.getName(), provider.getInfo());
    }

    try {
      log.debug("get provider");
      // Configure the SSL context.
      SSLContext ctx = SSLContext.getInstance("TLS");
      log.debug("got provider");

      log.debug("Build keymanagers");
      KeyManagerFactory keyMgrFact = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyMgrFact.init(getKeyStore(st.getKeyStore()), st.getKeyStore().getPassword().toCharArray());

      TrustManagerFactory trustMgrFact = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustMgrFact.init(getKeyStore(st.getTrustStore()));
      log.debug("Done keymanagers");

      log.debug("Initialize provider");
      ctx.init(keyMgrFact.getKeyManagers(), trustMgrFact.getTrustManagers(), new SecureRandom());
      log.debug("Initialize provider done");


      // Now set BouncyCastle as the default provider.
      log.debug("set default provider");
      SSLContext.setDefault(ctx);
      log.debug("done setting default provider");

      // For giggles lets dump the default context.
      dumpSSLContext("[HttpsContext].initContext: defaultContext", SslConfigurator.getDefaultContext());
      return ctx;
    } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException
            | IOException | CertificateException | UnrecoverableKeyException ex) {
      log.error("[HttpsContext].initContext: could not find SSL provider", ex);
      throw ex;
    }
  }

  /**
   *
   * @param ks
   * @return
   * @throws KeyStoreException
   * @throws IOException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   */
  private KeyStore getKeyStore(KeyStoreType ks) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    // Open specified keystore.
    File file = new File(ks.getFile());
    if (!file.exists()) {
      log.error("[HttpsContext].getKeyStore: file {} does not exist.", ks.getFile());
      throw new FileNotFoundException(String.format("[HttpsContext].getKeyStore: file {} does not exist", ks.getFile()));
    }
    KeyStore keyStore;
    try (InputStream stream = new FileInputStream(file)) {
      keyStore = KeyStore.getInstance(ks.getType());
      keyStore.load(stream, ks.getPassword().toCharArray());
    }
    return keyStore;
  }

  /**
   *
   * @param prefix
   * @param c
   */
  private void dumpSSLContext(String prefix, SSLContext c) {
    log.debug("{} - Provider = {}, {}", prefix, c.getProvider().getName(), c.getProvider().getInfo());

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
   * Get the default SSL context.
   *
   * @return New SSLContext for HTTP client.
   */
  public SSLContext getSSLContext() {
    //return SslConfigurator.getDefaultContext();
    return sslContext;
  }

  /**
   * Is this server configured for production?
   *
   * @return true if configured for production.
   */
  public boolean isProduction() {
    return isProduction;
  }
}

package net.es.nsi.dds.dao;

import jakarta.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collection;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.provider.Document;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class TestDocumentCache {

  private DdsConfiguration config;
  private static Logger log;

  @BeforeClass
  public static void initialize() {
    System.setProperty(Properties.SYSTEM_PROPERTY_LOG4J, "src/test/resources/config/log4j.xml");
    log = LogManager.getLogger(TestDocumentCache.class);
  }

  @Before
  public void setUp() throws IllegalArgumentException, JAXBException, FileNotFoundException, NullPointerException,
          IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException,
          NoSuchProviderException, UnrecoverableKeyException {
    config = new DdsConfiguration();
    config.setFilename("src/test/resources/config/dds.xml");
    config.load();
    log.debug("Configured nsaId={}", config.getNsaId());
  }

  @Test
  public void testLoadCache() throws FileNotFoundException, UnsupportedEncodingException {
    log.debug("@Test - testLoadCache");
    DdsProfile ddsProfile = new CacheProfile(config);
    DocumentCache cache = new DocumentCache(ddsProfile);
    assertTrue(cache.isEnabled());
    cache.load();
    Collection<Document> values = cache.values();
    assertTrue(values.size() > 0);
    for (Document document : values) {
      log.debug("id=" + URLDecoder.decode(document.getId(), "UTF-8"));
    }
    int original = values.size();
    cache.expire();
    assertEquals(original, cache.values().size());
  }

  @Test
  public void testLoadRepository() throws FileNotFoundException, UnsupportedEncodingException {
    log.debug("@Test - testLoadRepository");
    DdsProfile ddsProfile = new RepositoryProfile(config);
    DocumentCache cache = new DocumentCache(ddsProfile);
    assertTrue(cache.isEnabled());
    cache.load();
    Collection<Document> values = cache.values();
    assertTrue(values.isEmpty());
  }
}

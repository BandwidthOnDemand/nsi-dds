package net.es.nsi.dds.discovery;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import net.es.nsi.dds.client.TestServer;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.test.TestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class TestDocumentRepository {

  private final static HttpConfig testServer = new HttpConfig("localhost", "8402", "net.es.nsi.dds.client");
  private final static String DOCUMENT_DIR = "src/test/resources/documents/";
  private final static ObjectFactory factory = new ObjectFactory();
  private static DdsConfiguration ddsConfig;
  private static TestConfig testConfig;
  private static WebTarget target;
  private static WebTarget discovery;
  private static Logger log;

  @BeforeClass
  public static void initialize() {
    System.setProperty(Properties.SYSTEM_PROPERTY_LOG4J, "src/test/resources/config/log4j.xml");
    log = LogManager.getLogger(TestDocumentRepository.class);
  }

  @Before
  public void oneTimeSetUp() {
    log.debug("*************************************** TestDocumentRepository oneTimeSetUp ***********************************");

    try {
      testConfig = new TestConfig();
      target = testConfig.getTarget();
      discovery = target.path("dds");
      // Load a copy of the test DDS configuration and clear the document
      // repository for this test.
      ddsConfig = new DdsConfiguration();
      ddsConfig.setFilename(TestConfig.DEFAULT_DDS_FILE);
      ddsConfig.load();
      File directory = new File(ddsConfig.getRepository());
      FileUtilities.deleteDirectory(directory);

      // Configure the local test client callback server.
      TestServer.INSTANCE.start(testServer);
    } catch (IllegalArgumentException | JAXBException | IOException | IllegalStateException | KeyStoreException
            | NoSuchAlgorithmException | CertificateException | KeyManagementException | NoSuchProviderException
            | UnrecoverableKeyException ex) {
      log.error("oneTimeSetUp: failed to start HTTP server", ex);
      fail();
    }

    log.debug("*************************************** TestDocumentRepository oneTimeSetUp done ***********************************");
  }

  @After
  public void oneTimeTearDown() {
    log.debug("*************************************** TestDocumentRepository oneTimeTearDown ***********************************");
    testConfig.shutdown();
    try {
      File directory = new File(ddsConfig.getRepository());
      FileUtilities.deleteDirectory(directory);
      TestServer.INSTANCE.shutdown();
    } catch (Exception ex) {
      log.error("oneTimeTearDown: test server shutdown failed - {}", ex.getLocalizedMessage());
      fail();
    }
    log.debug("*************************************** TestDocumentRepository oneTimeTearDown done ***********************************");
  }

  /**
   * Load the Discovery Service with a default set of documents.
   *
   * @throws Exception
   */
  @Test
  public void addDocuments() throws Exception {
    log.debug("*************************************** TestDocumentRepository addDocuments ***********************************");
    // For each document file in the document directory load into discovery service.
    List<String> identifiers = new ArrayList<>();
    for (String file : FileUtilities.getXmlFileList(DOCUMENT_DIR)) {
      log.debug("File: {}", file);
      DocumentType document = DdsParser.getInstance().readDocument(file);
      JAXBElement<DocumentType> jaxbRequest = factory.createDocument(document);
      Response response = discovery.path("documents").request(MediaType.APPLICATION_XML).post(Entity.entity(new GenericEntity<JAXBElement<DocumentType>>(jaxbRequest) {
      }, MediaType.APPLICATION_XML));
      if (Response.Status.CREATED.getStatusCode() != response.getStatus()
              && Response.Status.CONFLICT.getStatusCode() != response.getStatus()) {
        fail();
      }
      response.close();

      identifiers.add(document.getId());
    }

    // Now verify the local repository contains the target files.
    assertTrue(ddsConfig.isRepositoryConfigured());
    assertTrue(verifyAddResults(identifiers));

    log.debug("*************************************** TestDocumentRepository addDocuments done ***********************************");
  }

  private boolean verifyAddResults(List<String> identifiers) throws JAXBException, IOException, NullPointerException {
    log.debug("Repo Dir=" + ddsConfig.getRepository());
    List<String> repository = new ArrayList<>();
    for (String file : FileUtilities.getXmlFileList(ddsConfig.getRepository())) {
      DocumentType document = DdsParser.getInstance().readDocument(file);
      repository.add(document.getId());
      log.debug("verifyAddResults: repo=" + document.getId());
    }

    for (String id : identifiers) {
      log.debug("verifyAddResults: id=" + id);
    }
    if (identifiers.containsAll(repository) && repository.containsAll(identifiers)) {
      return true;
    }

    return false;
  }
}

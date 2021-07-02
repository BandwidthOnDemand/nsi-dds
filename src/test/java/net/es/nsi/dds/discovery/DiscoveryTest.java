package net.es.nsi.dds.discovery;

import com.google.common.base.Strings;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.client.TestServer;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.DocumentEventType;
import net.es.nsi.dds.jaxb.dds.DocumentListType;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.ErrorType;
import net.es.nsi.dds.jaxb.dds.FilterCriteriaType;
import net.es.nsi.dds.jaxb.dds.FilterType;
import net.es.nsi.dds.jaxb.dds.NotificationListType;
import net.es.nsi.dds.jaxb.dds.NotificationType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.jaxb.dds.SubscriptionRequestType;
import net.es.nsi.dds.jaxb.dds.SubscriptionType;
import net.es.nsi.dds.jaxb.nsa.NsaType;
import net.es.nsi.dds.test.TestConfig;
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.util.XmlUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ChunkedInput;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DiscoveryTest {

  private final static HttpConfig testServer = new HttpConfig("localhost", "8802", "net.es.nsi.dds.client");

  private final static String DDS_CONFIGURATION = "src/test/resources/config/dds.xml";
  private final static String DOCUMENT_DIR = "src/test/resources/documents/";
  private final static ObjectFactory factory = new ObjectFactory();
  private static DdsConfiguration ddsConfig;
  private static TestConfig testConfig;
  private static WebTarget target;
  private static WebTarget discovery;
  private static String callbackURL;
  private static Logger log;

  @BeforeClass
  public static void oneTimeSetUp() {
    System.setProperty(Properties.SYSTEM_PROPERTY_LOG4J, "src/test/resources/config/log4j.xml");
    log = LogManager.getLogger(DiscoveryTest.class);

    log.debug("*************************************** DiscoveryTest oneTimeSetUp ***********************************");

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
    log.debug("*************************************** DiscoveryTest oneTimeSetUp done ***********************************");
  }

  @AfterClass
  public static void oneTimeTearDown() {
    log.debug("*************************************** DiscoveryTest oneTimeTearDown ***********************************");
    testConfig.shutdown();
    try {
      TestServer.INSTANCE.shutdown();
    } catch (IllegalStateException ex) {
      log.error("oneTimeTearDown: test server shutdown failed." + ex.getLocalizedMessage());
      fail();
    }
    log.debug("*************************************** DiscoveryTest oneTimeTearDown done ***********************************");
  }

  /**
   * A simple get on the ping URL.
   */
  @Test
  public void aPing() throws InterruptedException, JAXBException {
    log.debug("******* Running aPing test");

    // Simple ping to determine if interface is available.
    WebTarget path = discovery.path("ping");

    log.debug("Path: " + path.toString());

    try (Response response = path.request(MediaType.APPLICATION_XML).get()) {
      if (Response.Status.OK.getStatusCode() != response.getStatus() && response.hasEntity()) {
        // We propably have an error message returned so dump it for debugging.
        String entity = response.readEntity(String.class);
        log.debug("Entity: \n" + entity);

        // Read the error into an ErrorType strucutre.
        ErrorType error = DdsParser.getInstance().xml2Error(entity);
        System.out.printf("errorÇode: %d, description: %s\n", error.getCode(), error.getDescription());

      }
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    } finally {
      log.debug("******* Done aPing test");
    }
  }

  /**
   * Load the Discovery Service with a default set of documents.
   *
   * @throws Exception
   */
  @Test
  public void bLoadDocuments() throws Exception {
    log.debug("******* Running aLoadDocuments test");
    // For each document file in the document directory load into discovery service.
    for (String file : FileUtilities.getXmlFileList(DOCUMENT_DIR)) {
      DocumentType document = DdsParser.getInstance().readDocument(file);
      JAXBElement<DocumentType> jaxbRequest = factory.createDocument(document);
      try ( Response response = discovery.path("documents").request(NsiConstants.NSI_DDS_V1_XML).post(Entity.entity(new GenericEntity<JAXBElement<DocumentType>>(jaxbRequest) {
      }, NsiConstants.NSI_DDS_V1_XML))) {
        if (Response.Status.CREATED.getStatusCode() != response.getStatus()
                && Response.Status.CONFLICT.getStatusCode() != response.getStatus()) {
          System.out.printf("aLoadDocuments: failed with status %d\n", response.getStatus());
          fail();
        }
      }
    }
    log.debug("******* Done aLoadDocuments test");
  }

  /**
   * Queries the default set of documents.
   *
   * @throws Exception
   */
  @Test
  public void cDocumentsFull() throws Exception {
    log.debug("************************** Running cDocumentsFull test ********************************");
    // Get a list of all documents with full contents.
    Response response = discovery.path("documents").request(NsiConstants.NSI_DDS_V1_XML).get();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    final ChunkedInput<DocumentListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentListType>>() {
    });
    DocumentListType chunk;
    DocumentListType documents = null;
    while ((chunk = chunkedInput.read()) != null) {
      log.debug("Chunk received...");
      documents = chunk;
    }
    response.close();

    assertNotNull(documents);

    for (DocumentType document : documents.getDocument()) {
      log.debug("cDocumentsFull: " + document.getNsa() + ", " + document.getType() + ", " + document.getId() + ", href=" + document.getHref());
      assertFalse(document.getContent().getValue().isEmpty());

      response = testConfig.getClient().target(document.getHref()).request(NsiConstants.NSI_DDS_V1_XML).get();
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      DocumentType doc = response.readEntity(DocumentType.class);
      response.close();

      assertNotNull(doc);
      assertFalse(doc.getContent().getValue().isEmpty());

      // Do a search using the NSA and Type from previous result.
      response = discovery.path("documents")
              .path(URLEncoder.encode(document.getNsa().trim(), "UTF-8"))
              .path(URLEncoder.encode(document.getType().trim(), "UTF-8"))
              .request(NsiConstants.NSI_DDS_V1_XML).get();
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

      DocumentListType docList = response.readEntity(DocumentListType.class);
      response.close();

      boolean found = false;
      for (DocumentType docListItem : docList.getDocument()) {
        if (document.getNsa().equalsIgnoreCase(docListItem.getNsa())
                && document.getType().equalsIgnoreCase(docListItem.getType())
                && document.getId().equalsIgnoreCase(docListItem.getId())) {
          found = true;
        }
      }

      assertTrue(found);
    }
    log.debug("************************** Running cDocumentsFull test ********************************");
  }

  /**
   * Queries the default set of documents.
   *
   * @throws Exception
   */
  @Test
  public void dDocumentsSummary() throws Exception {
    log.debug("************************** Running dDocumentsSummary test ********************************");
    // Get a list of all documents with summary contents.
    Response response = discovery.path("documents").queryParam("summary", "true").request(NsiConstants.NSI_DDS_V1_XML).get();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    DocumentListType documents = response.readEntity(DocumentListType.class);
    response.close();
    assertNotNull(documents);

    for (DocumentType document : documents.getDocument()) {
      log.debug("dDocumentsSummary: " + document.getNsa() + ", " + document.getType() + ", " + document.getId() + ", href=" + document.getHref());
      assertTrue(document.getContent() == null || Strings.isNullOrEmpty(document.getContent().getValue()));

      // Read the direct href and get summary contents.
      response = testConfig.getClient().target(document.getHref()).queryParam("summary", "true").request(NsiConstants.NSI_DDS_V1_XML).get();
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      DocumentType doc = response.readEntity(DocumentType.class);
      response.close();

      assertNotNull(doc);
      assertTrue(doc.getContent() == null || Strings.isNullOrEmpty(doc.getContent().getValue()));
    }
  }

  @Test
  public void eDocumentNotFound() throws Exception {
    log.debug("************************** Running eDocumentNotFound test ********************************");
    // We want a NOT_FOUND for a nonexistent nsa resource on document path.
    Response response = discovery.path("documents").path("invalidNsaValue").request(NsiConstants.NSI_DDS_V1_XML).get();
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    response.close();

    // We want an empty result set for an invalid type on document path.
    response = discovery.path("documents").path("urn:ogf:network:czechlight.cesnet.cz:2013:nsa").path("invalidDocumentType").request(NsiConstants.NSI_DDS_V1_XML).get();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    DocumentListType documents = response.readEntity(DocumentListType.class);
    response.close();

    assertNotNull(documents);
    assertTrue(documents.getDocument().isEmpty());
    log.debug("************************** Done eDocumentNotFound test ********************************");
  }

  @Test
  public void fLocalDocuments() throws Exception {
    log.debug("************************** Running fLocalDocuments test ********************************");
    // Get a list of all documents with full contents.
    Response response = discovery.path("local").request(NsiConstants.NSI_DDS_V1_XML).get();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    DocumentListType documents;
    try ( ChunkedInput<DocumentListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentListType>>() {
    })) {
      DocumentListType chunk;
      documents = null;
      while ((chunk = chunkedInput.read()) != null) {
        log.debug("Chunk received...");
        documents = chunk;
      }
    }
    response.close();
    assertNotNull(documents);

    for (DocumentType document : documents.getDocument()) {
      log.debug("Local NSA Id compare: localId=" + DdsConfiguration.getInstance().getNsaId() + ", document=" + document.getNsa());
      assertEquals(document.getNsa(), DdsConfiguration.getInstance().getNsaId());

      response = discovery.path("local").path(URLEncoder.encode(document.getType(), "UTF-8")).request(NsiConstants.NSI_DDS_V1_XML).get();
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      DocumentListType docs = response.readEntity(DocumentListType.class);
      response.close();

      assertNotNull(docs);
      response.close();

      for (DocumentType d : docs.getDocument()) {
        assertEquals(document.getType(), d.getType());
      }
    }
    log.debug("************************** Done fLocalDocuments test ********************************");
  }

  @Test
  public void fUpdateDocuments() throws Exception {
    log.debug("************************** Running fUpdateDocuments test ********************************");
    // For each document file in the document directory load into discovery service.
    for (String file : FileUtilities.getXmlFileList(DOCUMENT_DIR)) {
      DocumentType document = DdsParser.getInstance().readDocument(file);
      XMLGregorianCalendar currentTime = XmlUtilities.xmlGregorianCalendar();
      XMLGregorianCalendar future = XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + 360000);
      document.setExpires(future);
      document.setVersion(currentTime);
      for (Object obj : document.getAny()) {
        if (obj instanceof JAXBElement<?>) {
          JAXBElement<?> jaxb = (JAXBElement<?>) obj;
          if (jaxb.getValue() instanceof NsaType) {
            NsaType nsa = (NsaType) jaxb.getValue();
            nsa.setVersion(currentTime);
            nsa.setExpires(future);
          }
        }
      }

      log.debug("fUpdateDocuments: updating document " + document.getId());

      JAXBElement<DocumentType> jaxbRequest = factory.createDocument(document);
      Response response = discovery.path("documents")
              .path(URLEncoder.encode(document.getNsa().trim(), "UTF-8"))
              .path(URLEncoder.encode(document.getType().trim(), "UTF-8"))
              .path(URLEncoder.encode(document.getId().trim(), "UTF-8"))
              .request(NsiConstants.NSI_DDS_V1_XML)
              .put(Entity.entity(new GenericEntity<JAXBElement<DocumentType>>(jaxbRequest) {
              }, NsiConstants.NSI_DDS_V1_XML));
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      response.close();
    }
    log.debug("************************** Done fUpdateDocuments test ********************************");
  }

  @Test
  public void gAddNotification() throws Exception {
    log.debug("************************** Running gAddNotification test ********************************");
    // Register for ALL document event types.
    SubscriptionRequestType subscription = factory.createSubscriptionRequestType();
    subscription.setRequesterId("urn:ogf:network:es.net:2013:nsa");
    subscription.setCallback(callbackURL);
    FilterCriteriaType criteria = factory.createFilterCriteriaType();
    criteria.getEvent().add(DocumentEventType.ALL);
    FilterType filter = factory.createFilterType();
    filter.getInclude().add(criteria);
    subscription.setFilter(filter);
    JAXBElement<SubscriptionRequestType> jaxbRequest = factory.createSubscriptionRequest(subscription);
    Response response = discovery.path("subscriptions").request(NsiConstants.NSI_DDS_V1_XML).post(Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxbRequest) {
    }, NsiConstants.NSI_DDS_V1_XML));
    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

    SubscriptionType result = response.readEntity(SubscriptionType.class);
    String id = result.getId();
    response.close();

    response = testConfig.getClient().target(result.getHref()).request(NsiConstants.NSI_DDS_V1_XML).get();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    result = response.readEntity(SubscriptionType.class);
    response.close();
    assertEquals(id, result.getId());

    response = testConfig.getClient().target(result.getHref()).request(NsiConstants.NSI_DDS_V1_XML).delete();
    response.close();
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

    log.debug("************************** Done gAddNotification test ********************************");
  }

  @Test
  public void hNotification() throws Exception {
    log.debug("************************** Running hNotification test ********************************");
    // Register for ALL document event types.
    SubscriptionRequestType subscription = factory.createSubscriptionRequestType();
    subscription.setRequesterId("urn:ogf:network:es.net:2013:nsa");
    subscription.setCallback(callbackURL);
    FilterCriteriaType criteria = factory.createFilterCriteriaType();
    criteria.getEvent().add(DocumentEventType.ALL);
    FilterType filter = factory.createFilterType();
    filter.getInclude().add(criteria);
    subscription.setFilter(filter);
    JAXBElement<SubscriptionRequestType> jaxbRequest = factory.createSubscriptionRequest(subscription);

    Response response = discovery.path("subscriptions").request(NsiConstants.NSI_DDS_V1_XML).post(Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxbRequest) {
    }, NsiConstants.NSI_DDS_V1_XML));
    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

    SubscriptionType result = response.readEntity(SubscriptionType.class);
    response.close();

    response = testConfig.getClient().target(result.getHref()).request(NsiConstants.NSI_DDS_V1_XML).get();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    response.close();

    // Now we wait for the initial notifications to arrive.
    int count = 0;
    NotificationListType notifications = TestServer.INSTANCE.peekDdsNotification();
    while (notifications == null && count < 30) {
      count++;
      Thread.sleep(1000);
      notifications = TestServer.INSTANCE.peekDdsNotification();
    }

    assertNotNull(notifications);
    notifications = TestServer.INSTANCE.pollDdsNotification();
    while (notifications != null) {
      log.debug("hNotification: providerId=" + notifications.getProviderId() + ", subscriptionId=" + notifications.getId());
      for (NotificationType notification : notifications.getNotification()) {
        log.debug("hNotification: event=" + notification.getEvent() + ", documentId=" + notification.getDocument().getId());
      }
      notifications = TestServer.INSTANCE.pollDdsNotification();
    }

    // Now send a document update.
    fUpdateDocuments();

    // Now we wait for the update notifications to arrive.
    count = 0;
    notifications = TestServer.INSTANCE.peekDdsNotification();
    while (notifications == null && count < 30) {
      count++;
      Thread.sleep(1000);
      notifications = TestServer.INSTANCE.peekDdsNotification();
    }

    assertNotNull(notifications);
    notifications = TestServer.INSTANCE.pollDdsNotification();
    while (notifications != null) {
      log.debug("hNotification: providerId=" + notifications.getProviderId() + ", subscriptionId=" + notifications.getId());
      for (NotificationType notification : notifications.getNotification()) {
        log.debug("hNotification: event=" + notification.getEvent() + ", documentId=" + notification.getDocument().getId());
      }
      notifications = TestServer.INSTANCE.pollDdsNotification();
    }

    response = testConfig.getClient().target(result.getHref()).request(NsiConstants.NSI_DDS_V1_XML).delete();
    response.close();
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    log.debug("************************** Done hNotification test ********************************");
  }

  @Test
  public void iReadEachDocumentType() throws Exception {
    readDocumentType(NsiConstants.NSI_DOC_TYPE_NSA_V1);
    readDocumentType(NsiConstants.NSI_DOC_TYPE_TOPOLOGY_V2);
    readDocumentType("vnd.ogf.nsi.nsa.status.v1+xml");
  }

  public void readDocumentType(String type) throws Exception {
    String encode = URLEncoder.encode(type, "UTF-8");
    Response response = discovery.path("documents").queryParam("type", encode).queryParam("summary", true).request(NsiConstants.NSI_DDS_V1_XML).get();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    DocumentListType documents = null;
    try (final ChunkedInput<DocumentListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentListType>>() {})) {
      DocumentListType chunk;
      while ((chunk = chunkedInput.read()) != null) {
        documents = chunk;
      }
    }

    response.close();

    assertNotNull(documents);
    assertNotNull(documents.getDocument());
    assertFalse(documents.getDocument().isEmpty());

    for (DocumentType document : documents.getDocument()) {
      log.debug("readDocumentType: reading document " + document.getId());
      response = testConfig.getClient().target(document.getHref()).request(NsiConstants.NSI_DDS_V1_XML).get();
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      response.close();
    }
  }
}

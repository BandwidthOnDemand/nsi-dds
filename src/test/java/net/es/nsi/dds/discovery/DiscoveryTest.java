package net.es.nsi.dds.discovery;

import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.client.TestServer;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.DocumentEventType;
import net.es.nsi.dds.jaxb.dds.DocumentListType;
import net.es.nsi.dds.jaxb.dds.DocumentType;
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

    private final static HttpConfig testServer = new HttpConfig("localhost", "8402", "net.es.nsi.dds.client");

    private final static String DDS_CONFIGURATION = "src/test/resources/config/dds.xml";
    private final static String DOCUMENT_DIR = "src/test/resources/documents/";
    private final static ObjectFactory factory = new ObjectFactory();
    private static DdsConfiguration ddsConfig;
    private static TestConfig testConfig;
    private static WebTarget target;
    private static WebTarget discovery;
    private static String callbackURL;

    @BeforeClass
    public static void oneTimeSetUp() {
        System.out.println("*************************************** DiscoveryTest oneTimeSetUp ***********************************");

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
        }
        catch (IllegalArgumentException | JAXBException | IOException | IllegalStateException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            System.err.println("oneTimeSetUp: failed to start HTTP server " + ex.getLocalizedMessage());
            fail();
        }

        testConfig = new TestConfig();
        target = testConfig.getTarget();
        discovery = target.path("dds");
        System.out.println("*************************************** DiscoveryTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        System.out.println("*************************************** DiscoveryTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        try {
            TestServer.INSTANCE.shutdown();
        }
        catch (Exception ex) {
            System.err.println("oneTimeTearDown: test server shutdown failed." + ex.getLocalizedMessage());
            fail();
        }
        System.out.println("*************************************** DiscoveryTest oneTimeTearDown done ***********************************");
    }

    /**
     * Load the Discovery Service with a default set of documents.
     *
     * @throws Exception
     */
    @Test
    public void aLoadDocuments() throws Exception {
        System.out.println("************************** Running aLoadDocuments test ********************************");
        // For each document file in the document directory load into discovery service.
        for (String file : FileUtilities.getXmlFileList(DOCUMENT_DIR)) {
            DocumentType document = DdsParser.getInstance().readDocument(file);
            JAXBElement<DocumentType> jaxbRequest = factory.createDocument(document);
            Response response = discovery.path("documents").request(NsiConstants.NSI_DDS_V1_XML).post(Entity.entity(new GenericEntity<JAXBElement<DocumentType>>(jaxbRequest) {}, NsiConstants.NSI_DDS_V1_XML));
            if (Response.Status.CREATED.getStatusCode() != response.getStatus() &&
                    Response.Status.CONFLICT.getStatusCode() != response.getStatus()) {
                fail();
            }
            response.close();
        }
        System.out.println("************************** Done aLoadDocuments test ********************************");
    }

    /**
     * A simple get on the ping URL.
     */
    @Test
    public void bPing() {
        // Simple ping to determine if interface is available.
        Response response = discovery.path("ping").request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * Queries the default set of documents.
     *
     * @throws Exception
     */
    @Test
    public void cDocumentsFull() throws Exception {
        System.out.println("************************** Running cDocumentsFull test ********************************");
        // Get a list of all documents with full contents.
        Response response = discovery.path("documents").request(NsiConstants.NSI_DDS_V1_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final ChunkedInput<DocumentListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentListType>>() {});
        DocumentListType chunk;
        DocumentListType documents = null;
        while ((chunk = chunkedInput.read()) != null) {
            System.out.println("Chunk received...");
            documents = chunk;
        }
        response.close();

        assertNotNull(documents);

        for (DocumentType document : documents.getDocument()) {
            System.out.println("cDocumentsFull: " + document.getNsa() + ", " + document.getType() + ", " + document.getId() + ", href=" + document.getHref());
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
                if (document.getNsa().equalsIgnoreCase(docListItem.getNsa()) &&
                        document.getType().equalsIgnoreCase(docListItem.getType()) &&
                        document.getId().equalsIgnoreCase(docListItem.getId())) {
                    found = true;
                }
            }

            assertTrue(found);
        }
        System.out.println("************************** Running cDocumentsFull test ********************************");
    }

    /**
     * Queries the default set of documents.
     *
     * @throws Exception
     */
    @Test
    public void dDocumentsSummary() throws Exception {
        System.out.println("************************** Running dDocumentsSummary test ********************************");
        // Get a list of all documents with summary contents.
        Response response = discovery.path("documents").queryParam("summary", "true").request(NsiConstants.NSI_DDS_V1_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        DocumentListType documents = response.readEntity(DocumentListType.class);
        response.close();
        assertNotNull(documents);

        for (DocumentType document : documents.getDocument()) {
            System.out.println("dDocumentsSummary: " + document.getNsa() + ", " + document.getType() + ", " + document.getId() + ", href=" + document.getHref());
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
        System.out.println("************************** Running eDocumentNotFound test ********************************");
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
        System.out.println("************************** Done eDocumentNotFound test ********************************");
    }

    @Test
    public void fLocalDocuments() throws Exception {
        System.out.println("************************** Running fLocalDocuments test ********************************");
        // Get a list of all documents with full contents.
        Response response = discovery.path("local").request(NsiConstants.NSI_DDS_V1_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        DocumentListType documents;
        try (ChunkedInput<DocumentListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentListType>>() {})) {
            DocumentListType chunk;
            documents = null;
            while ((chunk = chunkedInput.read()) != null) {
                System.out.println("Chunk received...");
                documents = chunk;
            }
        }
        response.close();
        assertNotNull(documents);

        for (DocumentType document : documents.getDocument()) {
            System.out.println("Local NSA Id compare: localId=" + DdsConfiguration.getInstance().getNsaId() + ", document="+ document.getNsa());
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
        System.out.println("************************** Done fLocalDocuments test ********************************");
    }

    @Test
    public void fUpdateDocuments() throws Exception {
        System.out.println("************************** Running fUpdateDocuments test ********************************");
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

            System.out.println("fUpdateDocuments: updating document " + document.getId());

            JAXBElement<DocumentType> jaxbRequest = factory.createDocument(document);
            Response response = discovery.path("documents")
                    .path(URLEncoder.encode(document.getNsa().trim(), "UTF-8"))
                    .path(URLEncoder.encode(document.getType().trim(), "UTF-8"))
                    .path(URLEncoder.encode(document.getId().trim(), "UTF-8"))
                    .request(NsiConstants.NSI_DDS_V1_XML)
                    .put(Entity.entity(new GenericEntity<JAXBElement<DocumentType>>(jaxbRequest) {}, NsiConstants.NSI_DDS_V1_XML));
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            response.close();
        }
        System.out.println("************************** Done fUpdateDocuments test ********************************");
    }

    @Test
    public void gAddNotification() throws Exception {
        System.out.println("************************** Running gAddNotification test ********************************");
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
        Response response = discovery.path("subscriptions").request(NsiConstants.NSI_DDS_V1_XML).post(Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxbRequest) {}, NsiConstants.NSI_DDS_V1_XML));
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

        System.out.println("************************** Done gAddNotification test ********************************");
    }

    @Test
    public void hNotification() throws Exception {
        System.out.println("************************** Running hNotification test ********************************");
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

        Response response = discovery.path("subscriptions").request(NsiConstants.NSI_DDS_V1_XML).post(Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxbRequest) {}, NsiConstants.NSI_DDS_V1_XML));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        SubscriptionType result = response.readEntity(SubscriptionType.class);
        response.close();

        response = testConfig.getClient().target(result.getHref()).request(NsiConstants.NSI_DDS_V1_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        response.close();

        // Now we wait for the initial notifications to arrive.
        int count = 0;
        NotificationListType notifications = TestServer.INSTANCE.peekDdsNotification();
        while(notifications == null && count < 30) {
            count++;
            Thread.sleep(1000);
            notifications = TestServer.INSTANCE.peekDdsNotification();
        }

        assertNotNull(notifications);
        notifications = TestServer.INSTANCE.pollDdsNotification();
        while (notifications != null) {
            System.out.println("hNotification: providerId=" + notifications.getProviderId() + ", subscriptionId=" + notifications.getId() );
            for (NotificationType notification : notifications.getNotification()) {
                System.out.println("hNotification: event=" + notification.getEvent() + ", documentId=" + notification.getDocument().getId());
            }
            notifications = TestServer.INSTANCE.pollDdsNotification();
        }

        // Now send a document update.
        fUpdateDocuments();

        // Now we wait for the update notifications to arrive.
        count = 0;
        notifications = TestServer.INSTANCE.peekDdsNotification();
        while(notifications == null && count < 30) {
            count++;
            Thread.sleep(1000);
            notifications = TestServer.INSTANCE.peekDdsNotification();
        }

        assertNotNull(notifications);
        notifications = TestServer.INSTANCE.pollDdsNotification();
        while (notifications != null) {
            System.out.println("hNotification: providerId=" + notifications.getProviderId() + ", subscriptionId=" + notifications.getId() );
            for (NotificationType notification : notifications.getNotification()) {
                System.out.println("hNotification: event=" + notification.getEvent() + ", documentId=" + notification.getDocument().getId());
            }
            notifications = TestServer.INSTANCE.pollDdsNotification();
        }

        response = testConfig.getClient().target(result.getHref()).request(NsiConstants.NSI_DDS_V1_XML).delete();
        response.close();
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        System.out.println("************************** Done hNotification test ********************************");
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
            System.out.println("readDocumentType: reading document " + document.getId());
            response = testConfig.getClient().target(document.getHref()).request(NsiConstants.NSI_DDS_V1_XML).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            response.close();
        }
    }
}

package net.es.nsi.dds.test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.DocumentEventType;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.FilterCriteriaType;
import net.es.nsi.dds.jaxb.dds.FilterType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.jaxb.dds.SubscriptionRequestType;
import net.es.nsi.dds.jaxb.dds.SubscriptionType;
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.util.XmlUtilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Fixtures for tests that drive the DDS REST API.
 */
public final class DdsTestSupport {
    public static final String VIXEN_NSA = "urn:ogf:network:example.com:2013:nsa:vixen";

    private static final String DOCUMENT_DIR = "src/test/resources/documents/";
    private static final ObjectFactory factory = new ObjectFactory();

    private DdsTestSupport() {
    }

    /**
     * Reads a document from src/test/resources/documents with a current version and an expiry one hour out.
     */
    public static DocumentType freshDocument(String file) throws Exception {
        DocumentType document = DdsParser.getInstance().readDocument(DOCUMENT_DIR + file);
        document.setVersion(XmlUtilities.xmlGregorianCalendar());
        document.setExpires(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + 3_600_000));
        return document;
    }

    /**
     * POSTs a document; a 409 from an earlier test class in the same JVM is fine.
     */
    public static void post(WebTarget target, DocumentType document) {
        JAXBElement<DocumentType> jaxb = factory.createDocument(document);
        try (Response response = target.request(NsiConstants.NSI_DDS_V1_XML)
                .post(Entity.entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb) {}, NsiConstants.NSI_DDS_V1_XML))) {
            int status = response.getStatus();
            assertTrue("POST " + target.getUri() + " returned " + status,
                    status == Response.Status.CREATED.getStatusCode() || status == Response.Status.CONFLICT.getStatusCode());
        }
    }

    /**
     * DELETEs a document. Tests must do this for what they POST: the server persists documents to
     * config/cache, and later test classes reload any that expire after the expiry interval.
     */
    public static void delete(WebTarget dds, DocumentType document) {
        try (Response response = dds.path("documents").path(encode(document.getNsa())).path(encode(document.getType()))
                .path(encode(document.getId())).request(NsiConstants.NSI_DDS_V1_XML).delete()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }

    public static SubscriptionRequestType subscriptionRequest() {
        SubscriptionRequestType request = factory.createSubscriptionRequestType();
        request.setRequesterId("urn:ogf:network:es.net:2013:nsa");
        request.setCallback("http://localhost:1/unused");
        FilterCriteriaType criteria = factory.createFilterCriteriaType();
        criteria.getEvent().add(DocumentEventType.ALL);
        FilterType filter = factory.createFilterType();
        filter.getInclude().add(criteria);
        request.setFilter(filter);
        return request;
    }

    public static Entity<?> entity(SubscriptionRequestType request) {
        JAXBElement<SubscriptionRequestType> jaxb = factory.createSubscriptionRequest(request);
        return Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxb) {}, NsiConstants.NSI_DDS_V1_XML);
    }

    /**
     * Creates a subscription and returns its id.
     */
    public static String subscribe(WebTarget dds) {
        try (Response response = dds.path("subscriptions").request(NsiConstants.NSI_DDS_V1_XML)
                .post(entity(subscriptionRequest()))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            return response.readEntity(SubscriptionType.class).getId();
        }
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

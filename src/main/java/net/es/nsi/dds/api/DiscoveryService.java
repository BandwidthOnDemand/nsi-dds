package net.es.nsi.dds.api;

import com.google.common.base.Strings;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.actors.RegistrationRouter;
import net.es.nsi.dds.config.ConfigurationManager;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.CollectionType;
import net.es.nsi.dds.jaxb.dds.DocumentListType;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.NotificationListType;
import net.es.nsi.dds.jaxb.dds.NotificationType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.jaxb.dds.SubscriptionListType;
import net.es.nsi.dds.jaxb.dds.SubscriptionRequestType;
import net.es.nsi.dds.management.logs.DdsErrors;
import net.es.nsi.dds.management.logs.DdsLogger;
import net.es.nsi.dds.provider.DiscoveryProvider;
import net.es.nsi.dds.provider.Document;
import net.es.nsi.dds.provider.Source;
import net.es.nsi.dds.provider.Subscription;
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.util.XmlUtilities;
import org.apache.http.client.utils.DateUtils;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Path("/dds")
@Consumes(MediaType.APPLICATION_XML)
public class DiscoveryService {
    private final ObjectFactory factory = new ObjectFactory();

    //@Context SecurityContext securityContext;

    /**
     * Ping to see if the DDS service is operational.
     *
     * @param sc
     * @return
     * @throws Exception
     */
    @GET
    @Path("/ping")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response ping(@Context SecurityContext sc) throws Exception {
        log.debug("ping: PING!");
        if (sc == null) {
            log.debug("ping: Security Context is null.");
        }
        else {
            log.debug("ping: authentication scheme = " + sc.getAuthenticationScheme());
            if (sc.getUserPrincipal() != null) {
                log.debug("ping: User principle=" + sc.getUserPrincipal().getName());
            }
        }
        return Response.ok().build();
    }

    @GET
    @Path("/error")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response error() throws Exception {
        return Response.serverError().build();
    }

    /**
     * Get all resources associated with this DDS instance.
     *
     * @param summary
     * @param ifModifiedSince
     * @return
     * @throws Exception
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response getAll(
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        log.debug("getAll: summary={}, If-Modified-Since={}", summary, ifModifiedSince);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        // Get all the applicable documents.
        Collection<Document> documents = discoveryProvider.getDocuments(null, null, null, lastDiscovered);

        Date discovered = new Date(0);
        DocumentListType documentResults = factory.createDocumentListType();
        if (documents.size() > 0) {
            // Only the document meta data is required and not the document
            // contents.
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    documentResults.getDocument().add(document.getDocumentSummary());
                }
                else {
                    documentResults.getDocument().add(document.getDocument());
                }
            }
        }

        // Get the local documents.  There may be duplicates with the full
        // document list.
        Collection<Document> local;
        try {
            local = discoveryProvider.getLocalDocuments(null, null, lastDiscovered);
        }
        catch (WebApplicationException we) {
            if (we.getResponse().getStatusInfo() == Status.NOT_FOUND) {
                local = new ArrayList<>();
            }
            else {
                throw we;
            }
        }

        DocumentListType localResults = factory.createDocumentListType();
        if (local.size() > 0) {
            // Only the document meta data is required and not the document
            // contents.
            for (Document document : local) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    localResults.getDocument().add(document.getDocumentSummary());
                }
                else {
                    localResults.getDocument().add(document.getDocument());
                }
            }
        }

        Collection<Subscription> subscriptions = discoveryProvider.getSubscriptions(null, lastDiscovered);

        SubscriptionListType subscriptionsResults = factory.createSubscriptionListType();
        if (subscriptions.size() > 0) {
            // Only the document meta data is required and not the document
            // contents.
            for (Subscription subscription : subscriptions) {
                if (discovered.before(subscription.getLastModified())) {
                    discovered = subscription.getLastModified();
                }

                subscriptionsResults.getSubscription().add(subscription.getSubscription());
            }
        }

        if (documentResults.getDocument().isEmpty() &&
                localResults.getDocument().isEmpty() &&
                subscriptionsResults.getSubscription().isEmpty()) {
            return Response.notModified().build();
        }

        CollectionType all = factory.createCollectionType();
        all.setDocuments(documentResults);
        all.setLocal(localResults);
        all.setSubscriptions(subscriptionsResults);

        try {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            String encoded = DdsParser.getInstance().collection2Xml(all);
            return Response.ok().header("Last-Modified", date).entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/", "Unable to format XML response " + ex.getMessage());
            log.error("getAll: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Get all documents associated with this DDS instance filtered by nsa, type, or id.
     *
     * @param id
     * @param nsa
     * @param type
     * @param summary
     * @param ifModifiedSince
     * @return
     */
    @GET
    @Path("/documents")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocuments(
            @QueryParam("nsa") String nsa,
            @QueryParam("type") String type,
            @QueryParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) {

        log.debug("getDocuments: nsa={}, type{}, id={}, summary={}, If-Modified-Since={}", nsa, type, id, summary, ifModifiedSince);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents = discoveryProvider.getDocuments(nsa, type, id, lastDiscovered);

        Date discovered = new Date(0);
        DocumentListType results = factory.createDocumentListType();
        if (documents.size() > 0) {
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                // Check if only the document meta data is required and not
                // the document contents.
                if (summary) {
                    results.getDocument().add(document.getDocumentSummary());
                }
                else {
                    results.getDocument().add(document.getDocument());
                }
            }
        }
        else {
            log.debug("getDocuments: zero results to query nsa={}, type{}, id={}, summary={}, If-Modified-Since={}", nsa, type, id, summary, ifModifiedSince);
        }

        // Now we need to determine what "Last-Modified" date we send back.
        Response.ResponseBuilder header;
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            header = Response.ok().header("Last-Modified", date);
        }
        else {
            header = Response.ok();
        }

        try {
            String encoded = DdsParser.getInstance().documents2Xml(results);
            return header.entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents", "Unable to format XML response " + ex.getMessage());
            log.error("getDocuments: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Get a list of all document associated with the specified nsa, and
     * filtered by type and/or id.
     *
     * @param nsa
     * @param type
     * @param id
     * @param summary
     * @param ifModifiedSince
     * @return
     * @throws WebApplicationException
     */
    @GET
    @Path("/documents/{nsa}")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocumentsByNsa(
            @PathParam("nsa") String nsa,
            @QueryParam("type") String type,
            @QueryParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        log.debug("getDocumentsByNsa: nsa={}, type{}, id={}, summary={}, If-Modified-Since={}", nsa, type, id, summary, ifModifiedSince);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents = discoveryProvider.getDocumentsByNsa(nsa.trim(), type, id, lastDiscovered);

        Date discovered = new Date(0);
        DocumentListType results = factory.createDocumentListType();
        if (documents.size() > 0) {
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    results.getDocument().add(document.getDocumentSummary());
                }
                else {
                    results.getDocument().add(document.getDocument());
                }
            }
        }

        // Now we need to determine what "Last-Modified" date we send back.
        Response.ResponseBuilder header;
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            header = Response.ok().header("Last-Modified", date);
        }
        else {
            header = Response.ok();
        }

        try {
            String encoded = DdsParser.getInstance().documents2Xml(results);
            return header.entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents", "Unable to format XML response " + ex.getMessage());
            log.error("getDocuments: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Get a list of all documents of the specified type associated with the
     * specified nsa, and filtered by id.
     *
     * @param nsa
     * @param type
     * @param id
     * @param summary
     * @param ifModifiedSince
     * @return
     * @throws WebApplicationException
     */
    @GET
    @Path("/documents/{nsa}/{type}")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocumentsByNsaAndType(
            @PathParam("nsa") String nsa,
            @PathParam("type") String type,
            @QueryParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        log.debug("getDocumentsByNsaAndType: nsa={}, type{}, id={}, summary={}, If-Modified-Since={}", nsa, type, id, summary, ifModifiedSince);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents;
        documents = discoveryProvider.getDocumentsByNsaAndType(nsa.trim(), type.trim(), id, lastDiscovered);

        Date discovered = new Date(0);
        DocumentListType results = factory.createDocumentListType();
        if (documents.size() > 0) {
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    results.getDocument().add(document.getDocumentSummary());
                }
                else {
                    results.getDocument().add(document.getDocument());
                }
            }
        }

        // Now we need to determine what "Last-Modified" date we send back.
        Response.ResponseBuilder header;
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            header = Response.ok().header("Last-Modified", date);
        }
        else {
            header = Response.ok();
        }

        try {
            String encoded = DdsParser.getInstance().documents2Xml(results);
            return header.entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents", "Unable to format XML response " + ex.getMessage());
            log.error("getDocuments: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Add a document to the system.
     *
     * @param request
     * @return
     * @throws WebApplicationException
     */
    @POST
    @Path("/documents")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response addDocument(InputStream request) throws WebApplicationException {
        // Parse incoming XML into JAXB objects.
        DocumentType newDocument;
        try {
            newDocument = DdsParser.getInstance().xml2Document(request);
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents", "Unable to process XML " + ex.getMessage());
            log.error("addDocument: Failed to parse incoming request.", invalidXmlException);
            throw invalidXmlException;
        }

        log.debug("addDocument: nsa={}, type{}, id={}", newDocument.getNsa(), newDocument.getType(), newDocument.getId());

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        Document document = discoveryProvider.addDocument(newDocument, Source.LOCAL);

        try {
            String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
            String encoded = DdsParser.getInstance().document2Xml(document.getDocument());
            return Response.created(URI.create(document.getDocument().getHref())).header("Last-Modified", date).entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents", "Unable to format XML response " + ex.getMessage());
            log.error("addDocument: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Delete a document from the system.
     *
     * @param nsa
     * @param type
     * @param id
     * @return
     * @throws WebApplicationException
     */
    @DELETE
    @Path("/documents/{nsa}/{type}/{id}")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response deleteDocument(
            @PathParam("nsa") String nsa,
            @PathParam("type") String type,
            @PathParam("id") String id) throws WebApplicationException {

        log.debug("deleteDocument: nsa={}, type{}, id={}", nsa, type, id);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Document document;
        document = discoveryProvider.deleteDocument(nsa, type, id);

        try {
            String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
            String encoded = DdsParser.getInstance().document2Xml(document.getDocument());
            return Response.ok().header("Last-Modified", date).entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException(document.getDocument().getHref(), "Unable to format XML response " + ex.getMessage());
            log.error("deleteDocument: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Add a local document to the system.
     *
     * @param request
     * @return
     * @throws Exception
     */
    @POST
    @Path("/local")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response addLocalDocument(InputStream request) throws Exception {
        log.debug("addLocalDocument:");
        return addDocument(request);
    }

    /**
     * Return a list of local documents based on document identifier and type.
     *
     * @param id
     * @param type
     * @param summary
     * @param ifModifiedSince
     * @return
     * @throws WebApplicationException
     */
    @GET
    @Path("/local")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response getLocalDocuments(
            @QueryParam("type") String type,
            @QueryParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        log.debug("getLocalDocuments: type{}, id={}, summary={}, If-Modified-Since={}", type, id, summary, ifModifiedSince);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents = discoveryProvider.getLocalDocuments(type, id, lastDiscovered);

        Date discovered = new Date(0);
        DocumentListType results = factory.createDocumentListType();
        if (documents.size() > 0) {
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    results.getDocument().add(document.getDocumentSummary());
                }
                else {
                    results.getDocument().add(document.getDocument());
                }
            }
        }

        // Now we need to determine what "Last-Modified" date we send back.
        Response.ResponseBuilder header;
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            header = Response.ok().header("Last-Modified", date);
        }
        else {
            header = Response.ok();
        }

        try {
            String encoded = DdsParser.getInstance().documents2Xml(results);
            return header.entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/local", "Unable to format XML response " + ex.getMessage());
            log.error("getLocalDocuments: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Get all local documents of the specified type and id.
     *
     * @param type
     * @param id
     * @param summary
     * @param ifModifiedSince
     * @return
     * @throws WebApplicationException
     */
    @GET
    @Path("/local/{type}")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response getLocalDocumentsByType(
            @PathParam("type") String type,
            @QueryParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        log.debug("getLocalDocumentsByType: type={}, id={}, summary={}, If-Modified-Since={}", type, id, summary, ifModifiedSince);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents;
        documents = discoveryProvider.getLocalDocumentsByType(type.trim(), id, lastDiscovered);

        Date discovered = new Date(0);
        DocumentListType results = factory.createDocumentListType();
        if (documents.size() > 0) {
            // Only the document meta data is required and not the document
            // contents.
            for (Document document : documents) {
                if (discovered.before(document.getLastDiscovered())) {
                    discovered = document.getLastDiscovered();
                }

                if (summary) {
                    results.getDocument().add(document.getDocumentSummary());
                }
                else {
                    results.getDocument().add(document.getDocument());
                }
            }
        }

        // Now we need to determine what "Last-Modified" date we send back.
        Response.ResponseBuilder header;
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            header = Response.ok().header("Last-Modified", date);
        }
        else {
            header = Response.ok();
        }

        try {
            String encoded = DdsParser.getInstance().documents2Xml(results);
            return header.entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/local/" + type, "Unable to format XML response " + ex.getMessage());
            log.error("getLocalDocumentsByType: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Get the local document corresponding to the supplied type and id.
     *
     * @param type
     * @param id
     * @param summary
     * @param ifModifiedSince
     * @return
     * @throws WebApplicationException
     */
    @GET
    @Path("/local/{type}/{id}")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response getLocalDocument(
            @PathParam("type") String type,
            @PathParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        log.debug("getLocalDocument: type{}, id={}, summary={}, If-Modified-Since={}", type, id, summary, ifModifiedSince);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Document document;
        document = discoveryProvider.getLocalDocument(type, id, lastDiscovered);

        if (document == null) {
            // We found matching but it was not modified.
            return Response.notModified().build();
        }

        DocumentType result;
        if (summary) {
            result = document.getDocumentSummary();
        }
        else {
            result = document.getDocument();
        }

        try {
            String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
            String encoded = DdsParser.getInstance().document2Xml(result);
            return Response.ok().header("Last-Modified", date).entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/local/" + type, "Unable to format XML response " + ex.getMessage());
            log.error("getLocalDocumentsByType: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Get the document corresponding to the specified nsa, type, and id.
     *
     * @param nsa
     * @param type
     * @param id
     * @param summary
     * @param ifModifiedSince
     * @return
     * @throws WebApplicationException
     */
    @GET
    @Path("/documents/{nsa}/{type}/{id}")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocument(
            @PathParam("nsa") String nsa,
            @PathParam("type") String type,
            @PathParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        log.debug("getDocument: nsa={}, type={}, id={}, summary={}, If-Modified-Since={}", nsa, type, id, summary, ifModifiedSince);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Document document = discoveryProvider.getDocument(nsa, type, id, lastDiscovered);

        if (document == null) {
            // We found matching but it was not modified.
            return Response.notModified().build();
        }

        DocumentType result;
        if (summary) {
            result = document.getDocumentSummary();
        }
        else {
            result = document.getDocument();
        }

        try {
            String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
            String encoded = DdsParser.getInstance().document2Xml(result);
            return Response.ok().header("Last-Modified", date).entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents/" + nsa + "/" + type + "/" + id, "Unable to format XML response " + ex.getMessage());
            log.error("getDocument: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Update the specified document.
     *
     * @param nsa
     * @param type
     * @param id
     * @param request
     * @return
     * @throws WebApplicationException
     */
    @PUT
    @Path("/documents/{nsa}/{type}/{id}")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response updateDocument(
            @PathParam("nsa") String nsa,
            @PathParam("type") String type,
            @PathParam("id") String id,
            InputStream request) throws WebApplicationException {

        log.debug("updateDocument: nsa={}, type{}, id={}", nsa, type, id);

        // Parse incoming XML into JAXB objects.
        DocumentType updateRequest;
        try {
            Object object = XmlUtilities.xmlToJaxb(DocumentType.class, request);
            if (object instanceof DocumentType) {
                updateRequest = (DocumentType) object;
            }
            else {
                WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents/" + nsa + "/" + type + "/" + id, "Expected DocumentType but found " + object.getClass().getCanonicalName());
                log.error("updateDocument: Failed to parse incoming request.", invalidXmlException);
                throw invalidXmlException;
            }
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents/" + nsa + "/" + type + "/" + id, "Unable to process XML " + ex.getMessage());
            log.error("updateDocument: Failed to parse incoming request.", invalidXmlException);
            throw invalidXmlException;
        }

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        Document document = discoveryProvider.updateDocument(nsa, type, id, updateRequest, Source.LOCAL);

        try {
            String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
            String encoded = DdsParser.getInstance().document2Xml(document.getDocument());
            return Response.ok().header("Last-Modified", date).entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents/" + nsa + "/" + type + "/" + id, "Unable to format XML response " + ex.getMessage());
            log.error("updateDocument: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Get a list of all subscriptions filtered by optional requesterId.
     *
     * @param requesterId
     * @param ifModifiedSince
     * @return
     * @throws WebApplicationException
     */
    @GET
    @Path("/subscriptions")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response getSubscriptions(
            @QueryParam("requesterId") String requesterId,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        log.debug("getSubscriptions: requesterId={}", requesterId);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastModified = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastModified = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Subscription> subscriptions;
        subscriptions = discoveryProvider.getSubscriptions(requesterId, lastModified);

        Date modified = new Date(0);
        SubscriptionListType results = factory.createSubscriptionListType();
        if (subscriptions.size() > 0) {
            for (Subscription subscription : subscriptions) {
                if (modified.before(subscription.getLastModified())) {
                    modified = subscription.getLastModified();
                }

                results.getSubscription().add(subscription.getSubscription());
                log.debug("getSubscriptions: requesterId={}, subscriptionId={}", requesterId, subscription.getSubscription().getId());
            }
        }

        // Now we need to determine what "Last-Modified" date we send back.
        Response.ResponseBuilder header;
        if (results.getSubscription().size() > 0) {
            String date = DateUtils.formatDate(modified, DateUtils.PATTERN_RFC1123);
            header = Response.ok().header("Last-Modified", date);
        }
        else {
            header = Response.ok();
        }

        try {
            String encoded = DdsParser.getInstance().subscriptions2Xml(results);
            return header.entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/subscriptions", "Unable to format XML response " + ex.getMessage());
            log.error("getSubscriptions: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Add a new subscription to the system.
     *
     * @param accept
     * @param request
     * @return
     * @throws WebApplicationException
     */
    @POST
    @Path("/subscriptions")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response addSubscription(
            @HeaderParam("Accept") String accept,
            InputStream request) throws WebApplicationException {

        // Parse incoming XML into JAXB objects.
        SubscriptionRequestType subscriptionRequest;
        try {
            Object object = XmlUtilities.xmlToJaxb(SubscriptionRequestType.class, request);
            if (object instanceof SubscriptionRequestType) {
                subscriptionRequest = (SubscriptionRequestType) object;
            }
            else {
                WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/subscriptions", "Expected SubscriptionRequestType but found " + object.getClass().getCanonicalName());
                log.error("addSubscription: Failed to parse incoming request.", invalidXmlException);
                throw invalidXmlException;
            }
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/subscriptions", "Unable to process XML " + ex.getMessage());
            log.error("addSubscription: Failed to parse incoming request.", invalidXmlException);
            throw invalidXmlException;
        }

        log.debug("addSubscription: requesterId={}, callback={}", subscriptionRequest.getRequesterId(), subscriptionRequest.getCallback());

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        Subscription subscription = discoveryProvider.addSubscription(subscriptionRequest, accept);

        log.debug("addSubscription: requesterId={}, subscriptionId={}", subscriptionRequest.getRequesterId(), subscription.getId());

        try {
            String date = DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123);
            String encoded = DdsParser.getInstance().subscription2Xml(subscription.getSubscription());
            log.debug("addSubscription: response\n{}", encoded);
            return Response.created(URI.create(subscription.getSubscription().getHref())).header("Last-Modified", date).entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/subscriptions", "Unable to format XML response " + ex.getMessage());
            log.error("addSubscription: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Get a specific registered subscription by identifier.
     *
     * @param id
     * @param ifModifiedSince
     * @param sc
     * @return
     * @throws WebApplicationException
     */
    @GET
    @Path("/subscriptions/{id}")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response getSubscription(
            @PathParam("id") String id,
            @HeaderParam("If-Modified-Since") String ifModifiedSince,
            @Context SecurityContext sc) throws WebApplicationException {

        if (sc.getUserPrincipal() != null) {
          log.debug("getSubscription: User principle=" + sc.getUserPrincipal().getName());
        }



        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (!Strings.isNullOrEmpty(ifModifiedSince)) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Subscription subscription;
        subscription = discoveryProvider.getSubscription(id, lastDiscovered);

        if (subscription == null) {
            // We found matching but it was not modified.
            log.debug("getSubscriptions: found id={} but not modified", id);
            return Response.notModified().build();
        }

        log.debug("getSubscriptions: found id={}, requesterId={}", id, subscription.getSubscription().getRequesterId());

        try {
            String date = DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123);
            String encoded = DdsParser.getInstance().subscription2Xml(subscription.getSubscription());
            return Response.ok().header("Last-Modified", date).entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/subscriptions/" + id, "Unable to format XML response " + ex.getMessage());
            log.error("getSubscription: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }
    }

    /**
     * Edit an existing subscription.
     *
     * @param accept
     * @param id
     * @param request
     * @return
     * @throws WebApplicationException
     */
    @PUT
    @Path("/subscriptions/{id}")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response editSubscription(
            @HeaderParam("Accept") String accept,
            @PathParam("id") String id,
            InputStream request) throws WebApplicationException {

        log.debug("editSubscription: id={}", id);

        // Parse the XML into JAXB objects.
        SubscriptionRequestType subscriptionRequest;
        try {
            Object object = XmlUtilities.xmlToJaxb(SubscriptionRequestType.class, request);
            if (object instanceof SubscriptionRequestType) {
                subscriptionRequest = (SubscriptionRequestType) object;
            }
            else {
                WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/subscriptions/" + id, "Expected SubscriptionRequestType but found " + object.getClass().getCanonicalName());
                log.error("editSubscription: Failed to parse incoming request.", invalidXmlException);
                throw invalidXmlException;
            }
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/subscriptions/" + id, "Unable to process XML " + ex.getMessage());
            log.error("editSubscription: Failed to parse incoming request.", invalidXmlException);
            throw invalidXmlException;
        }

        // Process the subscription edit request.
        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        Subscription subscription = discoveryProvider.editSubscription(id, subscriptionRequest, accept);

        try {
            String date = DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123);
            String encoded = DdsParser.getInstance().subscription2Xml(subscription.getSubscription());
            return Response.ok(URI.create(subscription.getSubscription().getHref())).header("Last-Modified", date).entity(encoded).build();
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/subscriptions/" + id, "Unable to format XML response " + ex.getMessage());
            log.error("getSubscription: Failed to format outgoing response.", invalidXmlException);
            throw invalidXmlException;
        }

    }

    /**
     * Delete a registered subscription from the system.
     *
     * @param id
     * @return
     * @throws WebApplicationException
     */
    @DELETE
    @Path("/subscriptions/{id}")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response deleteSubscription(@PathParam("id") String id) throws WebApplicationException {
        log.debug("deleteSubscription: id={}", id);
        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        discoveryProvider.deleteSubscription(id);
        return Response.noContent().build();
    }

    /**
     * Endpoint for incoming DDS document notifications.  This endpoint is
     * registered against peer DDS servers.
     *
     * @param host
     * @param encoding
     * @param source
     * @param request
     * @return
     * @throws WebApplicationException
     */
    @POST
    @Path("/notifications")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response notifications(@HeaderParam("Host") String host, @HeaderParam("Accept") String encoding, @HeaderParam("X-Forwarded-For") String source, InputStream request) throws WebApplicationException {

        log.debug("notifications: Incoming notification from Host={}, X-Forwarded-For={}, Accept={}", host, source, encoding);

        // Parse the XML into JAXB objects.
        NotificationListType notifications;
        try {
            Object object = XmlUtilities.xmlToJaxb(NotificationListType.class, request);
            if (object instanceof NotificationListType) {
                notifications = (NotificationListType) object;
            }
            else {
                DdsLogger.getInstance().error(DdsErrors.DDS_NOTIFICATION_SUBSCRIPTION_PARSE_ERROR, source, encoding);
                WebApplicationException invalidXmlException = Exceptions.invalidXmlException("notifications", "Expected NotificationListType but found " + object.getClass().getCanonicalName());
                log.error("notifications: Failed to parse incoming notifications.", invalidXmlException);
                throw invalidXmlException;
            }
        } catch (JAXBException | IOException ex) {
            DdsLogger.getInstance().error(DdsErrors.DDS_NOTIFICATION_SUBSCRIPTION_PARSE_ERROR, source, encoding);
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("notifications", "Unable to process XML " + ex.getMessage());
            log.error("notifications: Failed to parse incoming notifications.", invalidXmlException);
            throw invalidXmlException;
        }

        // Process the notification request.
        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        // Make sure this is still a valid subscription otherwise we ignore it
        // and remove the subscriptions on the remote DDS instance via and
        // exception thrown by this lookup.
        if (!RegistrationRouter.getInstance().isSubscription(notifications.getHref())) {
            log.error("notifications: Notification does not exist - provider={}, subscriptionId={}, href={}", notifications.getProviderId(), notifications.getId(), notifications.getHref());
            DdsLogger.getInstance().error(DdsErrors.DDS_NOTIFICATION_SUBSCRIPTION_NOT_FOUND, "id", notifications.getId());
            throw Exceptions.doesNotExistException(DiscoveryError.SUBSCRIPTION_DOES_NOT_EXIST, "id", notifications.getId());
        }

        log.debug("notifications: provider={}, subscriptionId={}, href={}", notifications.getProviderId(), notifications.getId(), notifications.getHref());
        for (NotificationType notification : notifications.getNotification()) {
            log.debug("notifications: processing notification event=" + notification.getEvent() + ", documentId=" + notification.getDocument().getId());
            try {
                discoveryProvider.processNotification(notification);
            }
            catch (Exception ex) {
                DdsLogger.getInstance().error(DdsErrors.DDS_NOTIFICATION_PROCESSING_ERROR, "id", notifications.getId());
                WebApplicationException internalServerErrorException = Exceptions.internalServerErrorException("notifications", "failed to process notification for documentId=" + notification.getDocument().getId());
                log.error("notifications: failed to process notification for documentId={}", notification.getDocument().getId(), ex);
                throw internalServerErrorException;
            }
        }

        return Response.accepted().build();
    }
}

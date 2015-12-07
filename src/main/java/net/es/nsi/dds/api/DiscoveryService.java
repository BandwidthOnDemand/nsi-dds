/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.config.ConfigurationManager;
import net.es.nsi.dds.jaxb.dds.CollectionType;
import net.es.nsi.dds.jaxb.dds.DocumentListType;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.NotificationListType;
import net.es.nsi.dds.jaxb.dds.NotificationType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.jaxb.dds.SubscriptionListType;
import net.es.nsi.dds.jaxb.dds.SubscriptionRequestType;
import net.es.nsi.dds.jaxb.dds.SubscriptionType;
import net.es.nsi.dds.provider.DiscoveryProvider;
import net.es.nsi.dds.provider.Document;
import net.es.nsi.dds.provider.Source;
import net.es.nsi.dds.provider.Subscription;
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.util.XmlUtilities;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
@Path("/dds")
public class DiscoveryService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();

    /**
     * Ping to see if the DDS service is operational.
     *
     * @return
     * @throws Exception
     */
    @GET
    @Path("/ping")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response ping(@Context SecurityContext sc) throws Exception {
        log.debug("ping: PING!");
        if (sc == null) {
            log.debug("Security Context is null.");
        }
        else {
            log.debug("authentication scheme=" + sc.getAuthenticationScheme());
            if (sc.getUserPrincipal() != null) {
                log.debug("User principle=" + sc.getUserPrincipal().getName());
            }
        }
        return Response.ok().build();
    }

    @GET
    @Path("/error")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response error() throws Exception {
        log.debug("error: Bang!");
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
        String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<CollectionType>>(factory.createCollection(all)){}).build();
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
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(results);
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
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
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(results);
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
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
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(results);
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
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
            Object object = XmlUtilities.xmlToJaxb(DocumentType.class, request);
            if (object instanceof DocumentType) {
                newDocument = (DocumentType) object;
            }
            else {
                WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents", "Expected DocumentType but found " + object.getClass().getCanonicalName());
                log.error("addDocument: Failed to parse incoming request.", invalidXmlException);
                throw invalidXmlException;
            }
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents", "Unable to process XML " + ex.getMessage());
            log.error("addDocument: Failed to parse incoming request.", invalidXmlException);
            throw invalidXmlException;
        }

        log.debug("addDocument: nsa={}, type{}, id={}", newDocument.getNsa(), newDocument.getType(), newDocument.getId());

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Document document = discoveryProvider.addDocument(newDocument, Source.LOCAL);

        String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
        JAXBElement<DocumentType> jaxb = factory.createDocument(document.getDocument());
        return Response.created(URI.create(document.getDocument().getHref())).header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb){}).build();
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

        String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
        JAXBElement<DocumentType> jaxb = factory.createDocument(document.getDocument());
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb){}).build();
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
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(results);
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
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

        log.debug("getLocalDocumentsByType: type{}, id={}, summary={}, If-Modified-Since={}", type, id, summary, ifModifiedSince);

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
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(results);
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
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

        JAXBElement<DocumentType> jaxb;
        if (summary) {
            jaxb = factory.createDocument(document.getDocumentSummary());
        }
        else {
            jaxb = factory.createDocument(document.getDocument());
        }

        String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb){}).build();
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

        log.debug("getDocument: nsa={}, type{}, id={}, summary={}, If-Modified-Since={}", nsa, type, id, summary, ifModifiedSince);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Document document;
        document = discoveryProvider.getDocument(nsa, type, id, lastDiscovered);

        if (document == null) {
            // We found matching but it was not modified.
            return Response.notModified().build();
        }

        JAXBElement<DocumentType> jaxb;
        if (summary) {
            jaxb = factory.createDocument(document.getDocumentSummary());
        }
        else {
            jaxb = factory.createDocument(document.getDocument());
        }

        String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb){}).build();
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

        Document document;
        document = discoveryProvider.updateDocument(nsa, type, id, updateRequest, Source.LOCAL);

        String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
        JAXBElement<DocumentType> jaxb = factory.createDocument(document.getDocument());
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb){}).build();
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
                log.debug("getSubscriptions: {}", subscription.getSubscription().getId());
            }
        }

        // Now we need to determine what "Last-Modified" date we send back.
        JAXBElement<SubscriptionListType> jaxb = factory.createSubscriptions(results);
        if (results.getSubscription().size() > 0) {
            String date = DateUtils.formatDate(modified, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<SubscriptionListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<SubscriptionListType>>(jaxb){}).build();
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

        String date = DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123);
        JAXBElement<SubscriptionType> jaxb = factory.createSubscription(subscription.getSubscription());
        return Response.created(URI.create(subscription.getSubscription().getHref())).header("Last-Modified", date).entity(new GenericEntity<JAXBElement<SubscriptionType>>(jaxb){}).build();
    }

    /**
     * Get a specific registered subscription by identifier.
     *
     * @param id
     * @param ifModifiedSince
     * @return
     * @throws WebApplicationException
     */
    @GET
    @Path("/subscriptions/{id}")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response getSubscription(
            @PathParam("id") String id,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        log.debug("getSubscriptions: id={}", id);

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Subscription subscription;
        subscription = discoveryProvider.getSubscription(id, lastDiscovered);

        if (subscription == null) {
            // We found matching but it was not modified.
            return Response.notModified().build();
        }

        JAXBElement<SubscriptionType> jaxb = factory.createSubscription(subscription.getSubscription());

        String date = DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123);
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<SubscriptionType>>(jaxb){}).build();
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
        Subscription subscription;
        subscription = discoveryProvider.editSubscription(id, subscriptionRequest, accept);

        String date = DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123);
        JAXBElement<SubscriptionType> jaxb = factory.createSubscription(subscription.getSubscription());
        return Response.ok(URI.create(subscription.getSubscription().getHref())).header("Last-Modified", date).entity(new GenericEntity<JAXBElement<SubscriptionType>>(jaxb){}).build();
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
                WebApplicationException invalidXmlException = Exceptions.invalidXmlException("notifications", "Expected NotificationListType but found " + object.getClass().getCanonicalName());
                log.error("notifications: Failed to parse incoming notifications.", invalidXmlException);
                throw invalidXmlException;
            }
        } catch (JAXBException | IOException ex) {
            WebApplicationException invalidXmlException = Exceptions.invalidXmlException("notifications", "Unable to process XML " + ex.getMessage());
            log.error("notifications: Failed to parse incoming notifications.", invalidXmlException);
            throw invalidXmlException;
        }

        // Process the notification request.
        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        log.debug("notifications: provider={}, subscriptionId={}, href={}", notifications.getProviderId(), notifications.getId(), notifications.getHref());
        for (NotificationType notification : notifications.getNotification()) {
            log.debug("notifications: processing notification event={}, documentId={}" + notification.getEvent(), notification.getDocument().getId());
            try {
                discoveryProvider.processNotification(notification);
            }
            catch (Exception ex) {
                WebApplicationException internalServerErrorException = Exceptions.internalServerErrorException("notifications", "failed to process notification for documentId=" + notification.getDocument().getId());
                log.error("notifications: failed to process notification for documentId={}", notification.getDocument().getId(), ex);
                throw internalServerErrorException;
            }
        }

        return Response.accepted().build();
    }
}

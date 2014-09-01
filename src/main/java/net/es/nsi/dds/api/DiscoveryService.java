/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.api;

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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBElement;
import net.es.nsi.dds.config.ConfigurationManager;
import net.es.nsi.dds.api.jaxb.CollectionType;
import net.es.nsi.dds.api.jaxb.DocumentListType;
import net.es.nsi.dds.api.jaxb.DocumentType;
import net.es.nsi.dds.api.jaxb.NotificationListType;
import net.es.nsi.dds.api.jaxb.NotificationType;
import net.es.nsi.dds.api.jaxb.ObjectFactory;
import net.es.nsi.dds.api.jaxb.SubscriptionListType;
import net.es.nsi.dds.api.jaxb.SubscriptionRequestType;
import net.es.nsi.dds.api.jaxb.SubscriptionType;
import net.es.nsi.dds.provider.DiscoveryProvider;
import net.es.nsi.dds.provider.Document;
import net.es.nsi.dds.provider.Source;
import net.es.nsi.dds.provider.Subscription;
import net.es.nsi.dds.schema.NsiConstants;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
@Path("/discovery")
public class DiscoveryService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();

    @GET
    @Path("/ping")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response ping() throws Exception {
        log.debug("ping: PING!");
        return Response.ok().build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getAll(
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {

        log.debug("getAll: summary=" + summary);

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

    @GET
    @Path("/documents")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocuments(
            @QueryParam("id") String id,
            @QueryParam("nsa") String nsa,
            @QueryParam("type") String type,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents = discoveryProvider.getDocuments(nsa, type, id, lastDiscovered);

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
        else {
            log.debug("getDocuments: zero results to query nsa=" + nsa + ", type=" + type + ", id=" + id + ", summary=" + summary);
        }

        // Now we need to determine what "Last-Modified" date we send back.
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(results);
        if (results.getDocument().size() > 0) {
            String date = DateUtils.formatDate(discovered, DateUtils.PATTERN_RFC1123);
            return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<DocumentListType>>(jaxb){}).build();
    }

    @GET
    @Path("/documents/{nsa}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocumentsByNsa(
            @PathParam("nsa") String nsa,
            @QueryParam("type") String type,
            @QueryParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents;
        documents = discoveryProvider.getDocumentsByNsa(nsa.trim(), type, id, lastDiscovered);

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

    @GET
    @Path("/documents/{nsa}/{type}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocumentsByNsaAndType(
            @PathParam("nsa") String nsa,
            @PathParam("type") String type,
            @QueryParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        log.debug("getDocumentsByNsaAndType: " + nsa + ", " + type + ", " + id + ", " + summary + ", " + ifModifiedSince);

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

    @POST
    @Path("/documents")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response addDocument(DocumentType request) throws WebApplicationException {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Document document = discoveryProvider.addDocument(request, Source.LOCAL);

        String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
        JAXBElement<DocumentType> jaxb = factory.createDocument(document.getDocument());
        return Response.created(URI.create(document.getDocument().getHref())).header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb){}).build();
    }

    @POST
    @Path("/local")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response addLocalDocument(DocumentType document) throws Exception {

        return addDocument(document);
    }

    @GET
    @Path("/local")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getLocalDocuments(
            @QueryParam("id") String id,
            @QueryParam("type") String type,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Date lastDiscovered = null;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            lastDiscovered = DateUtils.parseDate(ifModifiedSince);
        }

        Collection<Document> documents;
        documents = discoveryProvider.getLocalDocuments(type, id, lastDiscovered);

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

    @GET
    @Path("/local/{type}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getLocalDocumentsByType(
            @PathParam("type") String type,
            @QueryParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

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

    @GET
    @Path("/local/{type}/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getLocalDocument(
            @PathParam("type") String type,
            @PathParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

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

    @GET
    @Path("/documents/{nsa}/{type}/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getDocument(
            @PathParam("nsa") String nsa,
            @PathParam("type") String type,
            @PathParam("id") String id,
            @DefaultValue("false") @QueryParam("summary") boolean summary,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

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

    @PUT
    @Path("/documents/{nsa}/{type}/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response updateDocument(
            @PathParam("nsa") String nsa,
            @PathParam("type") String type,
            @PathParam("id") String id,
            DocumentType request) throws WebApplicationException {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        Document document;
        document = discoveryProvider.updateDocument(nsa, type, id, request, Source.LOCAL);

        String date = DateUtils.formatDate(document.getLastDiscovered(), DateUtils.PATTERN_RFC1123);
        JAXBElement<DocumentType> jaxb = factory.createDocument(document.getDocument());
        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<DocumentType>>(jaxb){}).build();
    }

    @GET
    @Path("/subscriptions")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getSubscriptions(
            @QueryParam("requesterId") String requesterId,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

        log.debug("getSubscriptions: " + requesterId + ", " + ifModifiedSince);

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

    @POST
    @Path("/subscriptions")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response addSubscription(
            @HeaderParam("Accept") String accept,
            SubscriptionRequestType subscriptionRequest) throws WebApplicationException {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        Subscription subscription;
        subscription = discoveryProvider.addSubscription(subscriptionRequest, accept);

        String date = DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123);
        JAXBElement<SubscriptionType> jaxb = factory.createSubscription(subscription.getSubscription());
        return Response.created(URI.create(subscription.getSubscription().getHref())).header("Last-Modified", date).entity(new GenericEntity<JAXBElement<SubscriptionType>>(jaxb){}).build();
    }

    @GET
    @Path("/subscriptions/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response getSubscription(
            @PathParam("id") String id,
            @HeaderParam("If-Modified-Since") String ifModifiedSince) throws WebApplicationException {

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

    @PUT
    @Path("/subscriptions/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response editSubscription(
            @HeaderParam("Accept") String accept,
            @PathParam("id") String id,
            SubscriptionRequestType subscriptionRequest) throws WebApplicationException {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        Subscription subscription;
        subscription = discoveryProvider.editSubscription(id, subscriptionRequest, accept);

        String date = DateUtils.formatDate(subscription.getLastModified(), DateUtils.PATTERN_RFC1123);
        JAXBElement<SubscriptionType> jaxb = factory.createSubscription(subscription.getSubscription());
        return Response.ok(URI.create(subscription.getSubscription().getHref())).header("Last-Modified", date).entity(new GenericEntity<JAXBElement<SubscriptionType>>(jaxb){}).build();
    }

    @DELETE
    @Path("/subscriptions/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response deleteSubscription(@PathParam("id") String id) throws WebApplicationException {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        discoveryProvider.deleteSubscription(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/notifications")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response notifications(NotificationListType notifications) throws WebApplicationException {

        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        if (notifications != null) {
            log.debug("notifications: provider=" + notifications.getProviderId() + ", subscriptionId=" + notifications.getId() + ", href=" + notifications.getHref() + ", discovered=" + notifications.getDiscovered());
            for (NotificationType notification : notifications.getNotification()) {
                log.debug("notifications: processing notification event=" + notification.getEvent() + ", documentId=" + notification.getDocument().getId());
                try {
                    discoveryProvider.processNotification(notification);
                }
                catch (Exception ex) {
                    log.error("notifications: failed to process notification for documentId=" + notification.getDocument().getId(), ex);
                    return Response.serverError().build();
                }
            }
        }
        else {
            log.error("notifications: Received empty notification.");
        }

        return Response.accepted().build();
    }
}

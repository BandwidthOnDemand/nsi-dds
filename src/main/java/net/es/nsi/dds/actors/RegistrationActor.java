/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.actors;

import akka.actor.UntypedActor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.dds.api.jaxb.DocumentEventType;
import net.es.nsi.dds.api.jaxb.ErrorType;
import net.es.nsi.dds.api.jaxb.FilterCriteriaType;
import net.es.nsi.dds.api.jaxb.FilterType;
import net.es.nsi.dds.api.jaxb.ObjectFactory;
import net.es.nsi.dds.api.jaxb.SubscriptionListType;
import net.es.nsi.dds.api.jaxb.SubscriptionRequestType;
import net.es.nsi.dds.api.jaxb.SubscriptionType;
import net.es.nsi.dds.client.RestClient;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.dao.RemoteSubscription;
import net.es.nsi.dds.dao.RemoteSubscriptionCache;
import net.es.nsi.dds.management.logs.DdsErrors;
import net.es.nsi.dds.management.logs.DdsLogger;
import net.es.nsi.dds.management.logs.DdsLogs;
import net.es.nsi.dds.messages.RegistrationEvent;
import net.es.nsi.dds.messages.RegistrationEvent.Event;
import net.es.nsi.dds.schema.NsiConstants;
import net.es.nsi.dds.util.UrlHelper;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RegistrationActor extends UntypedActor {
    private static final String NOTIFICATIONS_URL = "notifications";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DdsLogger logger = DdsLogger.getLogger();

    private final ObjectFactory factory = new ObjectFactory();
    private final DdsConfiguration discoveryConfiguration;
    private final RemoteSubscriptionCache remoteSubscriptionCache;
    private final RestClient restClient;

    public RegistrationActor(DdsConfiguration discoveryConfiguration) {
        this.discoveryConfiguration = discoveryConfiguration;
        this.remoteSubscriptionCache = RemoteSubscriptionCache.getInstance();
        this.restClient = RestClient.getInstance();
    }

    @Override
    public void preStart() {
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof RegistrationEvent) {
            RegistrationEvent event = (RegistrationEvent) msg;
            log.debug("RegistrationActor: event=" + event.getEvent().name() + ", url=" + event.getUrl());

            switch (event.getEvent()) {
                case Register:
                    register(event);
                    break;
                case Update:
                    update(event);
                    break;
                case Delete:
                    delete(event);
                    break;
                default:
                    unhandled(msg);
                    break;
            }
        } else {
            unhandled(msg);
        }
    }

    private String getNotificationURL() throws MalformedURLException {
        String baseURL = discoveryConfiguration.getBaseURL();
        URL url;
        if (!baseURL.endsWith("/")) {
            baseURL = baseURL + "/";
        }
        url = new URL(baseURL);
        url = new URL(url, NOTIFICATIONS_URL);
        return url.toExternalForm();
    }

    /**
     * Create a new subscription on the specified remote DDS service.
     *
     * @param event
     */
    private void register(RegistrationEvent event) throws IllegalArgumentException {
        if (event.getEvent() != Event.Register) {
            throw new IllegalArgumentException("register: invalid event type " + event.getEvent());
        }

        final String remoteDdsURL = event.getUrl();

        // We will register for all events on all documents.
        FilterCriteriaType criteria = factory.createFilterCriteriaType();
        criteria.getEvent().add(DocumentEventType.ALL);
        FilterType filter = factory.createFilterType();
        filter.getInclude().add(criteria);
        SubscriptionRequestType request = factory.createSubscriptionRequestType();
        request.setFilter(filter);
        request.setRequesterId(discoveryConfiguration.getNsaId());

        try {
            request.setCallback(getNotificationURL());
        }
        catch (MalformedURLException mx) {
            log.error("RegistrationActor.register: failed to get my notification callback URL, failing registration for " + remoteDdsURL, mx);
            logger.error(DdsErrors.DDS_CONFIGURATION_INVALID_LOCAL_DDS_URL, remoteDdsURL);
            return;
        }

        Client client = restClient.get();
        WebTarget webTarget = client.target(remoteDdsURL).path("subscriptions");
        JAXBElement<SubscriptionRequestType> jaxb = factory.createSubscriptionRequest(request);

        Response response;
        try {
            log.debug("RegistrationActor: registering with remote DDS " + remoteDdsURL);
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML)
                    .header(HttpHeaders.CONTENT_ENCODING, "gzip")
                    .post(Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxb) {}, NsiConstants.NSI_DDS_V1_XML));
        }
        catch (Exception ex) {
            log.error("RegistrationActor.register: error on endpoint " + remoteDdsURL, ex);
            logger.error(DdsErrors.DDS_SUBSCRIPTION_ADD_FAILED, remoteDdsURL);
            return;
        }

        // If this failed then we log the issue and do not save the subscription information.
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            log.error("RegistrationActor.register: failed to create subscription " + remoteDdsURL + ", result = " + response.getStatusInfo().getReasonPhrase());

            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("RegistrationActor.register: Error id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
                logger.error(DdsErrors.DDS_SUBSCRIPTION_ADD_FAILED_DETAILED, remoteDdsURL, error.getId());
            }
            else {
                logger.error(DdsErrors.DDS_SUBSCRIPTION_ADD_FAILED_DETAILED, remoteDdsURL, response.getStatusInfo().getReasonPhrase());
            }

            response.close();
            return;
        }

        // Looks like we were successful so save the subscription information.
        SubscriptionType newSubscription = response.readEntity(SubscriptionType.class);
        logger.log(DdsLogs.DDS_SUBSCRIPTION_CREATED, remoteDdsURL, newSubscription.getHref());

        log.debug("RegistrationActor: registered with remote DDS " + remoteDdsURL + ", id=" + newSubscription.getId());

        RemoteSubscription remoteSubscription = new RemoteSubscription();
        remoteSubscription.setDdsURL(remoteDdsURL);
        remoteSubscription.setSubscription(newSubscription);
        if (response.getLastModified() == null) {
            // We should have gotten a valid lastModified date back.  Fake one
            // until we have worked out all the failure cases.  This will open
            // a small window of inaccuracy.
            log.error("RegistrationActor.register: invalid LastModified header for id=" + newSubscription.getId() + ", href=" + newSubscription.getHref());
            remoteSubscription.setCreated(new Date((System.currentTimeMillis() / 1000) * 1000 ));
        }
        else {
            remoteSubscription.setCreated(response.getLastModified());
        }

        remoteSubscriptionCache.add(remoteSubscription);
        response.close();

        // Now that we have registered a new subscription make sure we clean up
        // and old ones that may exist on the remote DDS.
        deleteOldSubscriptions(remoteDdsURL, newSubscription.getId());
    }

    private void deleteOldSubscriptions(String remoteDdsURL, String id) {
        Client client = restClient.get();

        WebTarget webTarget = client.target(remoteDdsURL).path("subscriptions").queryParam("requesterId", discoveryConfiguration.getNsaId());
        Response response;
        try {
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).get();
        }
        catch (Exception ex) {
            log.error("GET failed for " + webTarget.getUri().toASCIIString(), ex);
            logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED, webTarget.getUri().toASCIIString());
            return;
        }

        // If this failed then we log the issue hope for the best.  Duplicate
        // notification are not an issue.
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            log.error("Failed to retrieve list of subscriptions " + remoteDdsURL + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED_DETAILED, webTarget.getUri().toASCIIString(), error.getId());
            }
            else {
                logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED_DETAILED, webTarget.getUri().toASCIIString(), response.getStatusInfo().getReasonPhrase());
            }
            response.close();
            return;
        }

        // Looks like we were successful so save the subscription information.
        SubscriptionListType subscriptions = response.readEntity(SubscriptionListType.class);
        response.close();

        // For each subscription returned registered to our nsaId we check to
        // see if it is the one we just registered (current subscription).  If
        // it is not we delete the subscription.
        for (SubscriptionType subscription : subscriptions.getSubscription()) {
            if (!id.equalsIgnoreCase(subscription.getId())) {
                // Found one we need to remove.
                log.debug("deleteOldSubscriptions: found stale subscription " + subscription.getHref() + " on DDS " + webTarget.getUri().toASCIIString());
                deleteSubscription(remoteDdsURL, subscription.getHref());
            }
        }
    }

    private void update(RegistrationEvent event) throws IllegalArgumentException {
        if (event.getEvent() != Event.Update) {
            throw new IllegalArgumentException("update: invalid event type " + event.getEvent());
        }

        Client client = restClient.get();

        // First we retrieve the remote subscription to see if it is still
        // valid.  If it is not then we register again, otherwise we leave it
        // alone for now.
        RemoteSubscription remoteSubscription = remoteSubscriptionCache.get(event.getUrl());
        String remoteSubscriptionURL = remoteSubscription.getSubscription().getHref();

        // Check to see if the remote subscription URL is absolute or relative.
        WebTarget webTarget;
        if (UrlHelper.isAbsolute(remoteSubscriptionURL)) {
            webTarget = client.target(remoteSubscriptionURL);
        }
        else {
            webTarget = client.target(remoteSubscription.getDdsURL()).path(remoteSubscriptionURL);
        }

        String absoluteURL = webTarget.getUri().toASCIIString();

        // Read the remote subscription to determine existanxe and last update time.
        remoteSubscription.setLastAudit(new Date());
        Response response;
        try {
            log.debug("RegistrationActor.update: getting subscription " + absoluteURL + ", lastModified=" + remoteSubscription.getLastModified());
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).header("If-Modified-Since", DateUtils.formatDate(remoteSubscription.getLastModified(), DateUtils.PATTERN_RFC1123)).get();
        }
        catch (Exception ex) {
            log.error("GET failed for " + absoluteURL, ex);
            logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED, absoluteURL);
            return;
        }

        // We found the subscription and it was not updated.
        if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
            // The subscription exists and has not been modified.
            log.debug("RegistrationActor.update: subscription " + absoluteURL + " exists (not modified).");
            remoteSubscription.setLastSuccessfulAudit(new Date());
        }
        // We found the subscription and it was updated.
        else if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            // The subscription exists but was modified since our last query.
            // Save the new version even though we should have know about it.
            remoteSubscription.setLastModified(response.getLastModified());
            remoteSubscription.setLastSuccessfulAudit(new Date());
            SubscriptionType update = response.readEntity(SubscriptionType.class);
            remoteSubscription.setSubscription(update);
            logger.log(DdsLogs.DDS_SUBSCRIPTION_UPDATE_DETECTED, absoluteURL, response.getLastModified().toString());
        }
        // We did not find the subscription so will need to create a new one.
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            // Looks like our subscription was removed. We need to add it back in.
            logger.error(DdsErrors.DDS_SUBSCRIPTION_NOT_FOUND, absoluteURL);

            // Remove the stored subscription since a new one will be created.
            remoteSubscriptionCache.remove(remoteSubscription.getDdsURL());
            event.setEvent(RegistrationEvent.Event.Register);
            register(event);
        }
        // An unexpected error has occured.
        else {
            // Some other error we cannot handle at the moment.
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED_DETAILED, absoluteURL, error.getId());
            }
            else {
                logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED_DETAILED, absoluteURL, response.getStatusInfo().getReasonPhrase());
            }
        }

        response.close();
    }

    private void delete(RegistrationEvent event) throws IllegalArgumentException {
        if (event.getEvent() != Event.Delete) {
            throw new IllegalArgumentException("delete: invalid event type " + event.getEvent());
        }

        RemoteSubscription subscription = remoteSubscriptionCache.get(event.getUrl());
        if (deleteSubscription(subscription.getDdsURL(), subscription.getSubscription().getHref())) {
            remoteSubscriptionCache.remove(subscription.getDdsURL());
        }
    }

    private boolean deleteSubscription(String remoteDdsURL, String remoteSubscriptionURL) {
        Client client = restClient.get();

        // Check to see if the remote subscription URL is absolute or relative.
        WebTarget webTarget;
        if (UrlHelper.isAbsolute(remoteSubscriptionURL)) {
            webTarget = client.target(remoteSubscriptionURL);
        }
        else {
            webTarget = client.target(remoteDdsURL).path(remoteSubscriptionURL);
        }

        String absoluteURL = webTarget.getUri().toASCIIString();

        Response response;
        try {
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).delete();
        }
        catch (Exception ex) {
            log.error("Failed to delete subscription " + absoluteURL, ex);
            logger.error(DdsErrors.DDS_SUBSCRIPTION_DELETE_FAILED, absoluteURL);
            return false;
        }

        boolean result = true;
        if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            // Successfully deleted the subscription.
            logger.log(DdsLogs.DDS_SUBSCRIPTION_DELETED, absoluteURL);
        }
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            logger.error(DdsErrors.DDS_SUBSCRIPTION_DELETE_FAILED_DETAILED, absoluteURL, response.getStatusInfo().getReasonPhrase());
        }
        else {
            log.error("RegistrationActor.delete: failed to delete subscription " + absoluteURL + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                logger.error(DdsErrors.DDS_SUBSCRIPTION_DELETE_FAILED_DETAILED, absoluteURL, error.getId());
            }
            else {
                logger.error(DdsErrors.DDS_SUBSCRIPTION_DELETE_FAILED_DETAILED, absoluteURL, response.getStatusInfo().getReasonPhrase());
            }
            result = false;
        }

        response.close();
        return result;
    }
}
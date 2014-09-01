/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.discovery.actors;

import net.es.nsi.dds.discovery.dao.RemoteSubscriptionCache;
import net.es.nsi.dds.discovery.messages.RegistrationEvent;
import net.es.nsi.dds.discovery.messages.RemoteSubscription;
import akka.actor.UntypedActor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.dds.discovery.dao.DiscoveryConfiguration;
import net.es.nsi.dds.api.jaxb.DocumentEventType;
import net.es.nsi.dds.api.jaxb.ErrorType;
import net.es.nsi.dds.api.jaxb.FilterCriteriaType;
import net.es.nsi.dds.api.jaxb.FilterType;
import net.es.nsi.dds.api.jaxb.ObjectFactory;
import net.es.nsi.dds.api.jaxb.SubscriptionListType;
import net.es.nsi.dds.api.jaxb.SubscriptionRequestType;
import net.es.nsi.dds.api.jaxb.SubscriptionType;
import net.es.nsi.dds.discovery.util.UrlHelper;
import net.es.nsi.dds.jersey.RestClient;
import net.es.nsi.dds.schema.NsiConstants;
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
    private final ObjectFactory factory = new ObjectFactory();
    private DiscoveryConfiguration discoveryConfiguration;
    private RemoteSubscriptionCache remoteSubscriptionCache;
    private RestClient restClient;

    public RegistrationActor(DiscoveryConfiguration discoveryConfiguration) {
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
            log.debug("RegistrationActor: event=" + event.getEvent().name());

            if (event.getEvent() == RegistrationEvent.Event.Register) {
                register(event);
            }
            else if (event.getEvent() == RegistrationEvent.Event.Update) {
                update(event);
            }
            else if (event.getEvent() == RegistrationEvent.Event.Delete) {
                delete(event);
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

    private void register(RegistrationEvent event) {
        final RemoteSubscription remoteSubscription = event.getSubscription();
        final String remoteDdsURL = remoteSubscription.getDdsURL();

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
            return;
        }

        Client client = restClient.get();

        WebTarget webTarget = client.target(remoteDdsURL).path("subscriptions");
        JAXBElement<SubscriptionRequestType> jaxb = factory.createSubscriptionRequest(request);
        Response response;
        try {
            log.debug("RegistrationActor: registering with remote DDS " + remoteDdsURL);
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).post(Entity.entity(new GenericEntity<JAXBElement<SubscriptionRequestType>>(jaxb) {}, NsiConstants.NSI_DDS_V1_XML));
        }
        catch (Exception ex) {
            log.error("RegistrationActor.register: endpoint " + remoteDdsURL, ex);
            //client.close();
            return;
        }

        // If this failed then we log the issue and do not save the subscription information.
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            log.error("RegistrationActor.register: failed to create subscription " + remoteDdsURL + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("RegistrationActor.register: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }
            response.close();
            //client.close();
            return;
        }

        // Looks like we were successful so save the subscription information.
        SubscriptionType newSubscription = response.readEntity(SubscriptionType.class);

        log.debug("RegistrationActor.register: created subscription " + newSubscription.getId() + ", href=" + newSubscription.getHref() + ", lastModified=" + response.getLastModified());

        remoteSubscription.setSubscription(newSubscription);
        if (response.getLastModified() == null) {
            // We should have gotten a valid lastModified date back.  Fake one
            // until we have worked out all the failure cases.  This will open
            // a small window of inaccuracy.
            log.error("RegistrationActor.register: invalid LastModified header for id=" + newSubscription.getId() + ", href=" + newSubscription.getHref());
            remoteSubscription.setLastModified(new Date((System.currentTimeMillis() / 1000) * 1000 ));
        }
        else {
            remoteSubscription.setLastModified(response.getLastModified());
        }

        remoteSubscriptionCache.add(remoteSubscription);

        response.close();
        //client.close();

        // Now that we have registered a new subscription make sure we clean up
        // and old ones that may exist on the remote DDS.
        deleteOldSubscriptions(remoteDdsURL, newSubscription.getId());
    }

    private void deleteOldSubscriptions(String remoteDdsURL, String id) {
        Client client = restClient.get();

        WebTarget webTarget = client.target(remoteDdsURL).path("subscriptions").queryParam("requesterId", discoveryConfiguration.getNsaId());
        Response response;
        try {
            log.debug("deleteOldSubscriptions: querying subscriptions on remote DDS " + remoteDdsURL);
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).get();
        }
        catch (Exception ex) {
            log.error("deleteOldSubscriptions: failed for endpoint " + remoteDdsURL, ex);
            //client.close();
            return;
        }

        // If this failed then we log the issue hope for the best.  Duplicate
        // notification are not an issue.
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            log.error("deleteOldSubscriptions: failed to retrieve list of subscriptions " + remoteDdsURL + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("deleteOldSubscriptions: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }
            response.close();
            //client.close();
            return;
        }

        // Looks like we were successful so save the subscription information.
        SubscriptionListType subscriptions = response.readEntity(SubscriptionListType.class);
        response.close();
        //client.close();

        // For each subscription returned registered to our nsaId we check to
        // see if it is the one we just registered (current subscription).  If
        // it is not we delete the subscription.
        for (SubscriptionType subscription : subscriptions.getSubscription()) {
            if (!id.equalsIgnoreCase(subscription.getId())) {
                // Found one we need to remove.
                log.debug("deleteOldSubscriptions: found stale subscription " + subscription.getHref() + " on DDS " + remoteDdsURL);
                deleteSubscription(remoteDdsURL, subscription.getHref());
            }
        }
    }

    private void update(RegistrationEvent event) {
        Client client = restClient.get();

        // First we retrieve the remote subscription to see if it is still
        // valid.  If it is not then we register again, otherwise we leave it
        // alone for now.
        RemoteSubscription remoteSubscription = remoteSubscriptionCache.get(event.getSubscription().getDdsURL());
        String remoteSubscriptionURL = remoteSubscription.getSubscription().getHref();

        // Check to see if the remote subscription URL is absolute or relative.
        WebTarget webTarget;
        if (UrlHelper.isAbsolute(remoteSubscriptionURL)) {
            webTarget = client.target(remoteSubscriptionURL);
        }
        else {
            webTarget = client.target(remoteSubscription.getDdsURL()).path(remoteSubscriptionURL);
        }

        Response response = null;
        try {
            log.debug("RegistrationActor.update: getting subscription " + webTarget.getUri().toASCIIString() + ", lastModified=" + remoteSubscription.getLastModified());
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).header("If-Modified-Since", DateUtils.formatDate(remoteSubscription.getLastModified(), DateUtils.PATTERN_RFC1123)).get();
        }
        catch (Exception ex) {
            log.error("RegistrationActor.update: failed to update subscription " + remoteSubscriptionURL, ex);
            if (response != null) {
                response.close();
            }
            //client.close();
            return;
        }

        if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
            // The subscription exists and has not been modified.
            log.debug("RegistrationActor.update: subscription " + webTarget.getUri().toASCIIString() + " exists (not modified).");
        }
        else if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            // The subscription exists but was modified since our last query.
            // Save the new version even though we should have know about it.
            remoteSubscription.setLastModified(response.getLastModified());
            SubscriptionType update = response.readEntity(SubscriptionType.class);
            remoteSubscription.setSubscription(update);
            log.debug("RegistrationActor.update: subscription " + webTarget.getUri().toASCIIString() + " exists (modified).");
        }
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            // Looks like our subscription was removed. We need to add it back in.
            log.debug("RegistrationActor.update: subscription " + webTarget.getUri().toASCIIString() + " does not exists and will be recreated.");

            // Remove the stored subscription since a new one will be created.
            remoteSubscriptionCache.remove(remoteSubscription.getDdsURL());
            register(event);
        }
        else {
            // Some other error we cannot handle at the moment.
            log.error("RegistrationActor.update: failed to update subscription " + webTarget.getUri().toASCIIString() + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("RegistrationActor.update: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }
        }
        //client.close();
        response.close();
    }

    private void delete(RegistrationEvent event) {

        RemoteSubscription subscription = event.getSubscription();
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

        Response response = null;
        try {
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).delete();
        }
        catch (Exception ex) {
            log.error("RegistrationActor.delete: failed to delete subscription " + remoteDdsURL, ex);
            //client.close();
            if (response != null) {
                response.close();
            }
            return false;
        }

        if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            // Successfully deleted the subscription.
            log.debug("RegistrationActor.delete: deleted " + remoteSubscriptionURL);
        }
        else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            log.error("RegistrationActor.delete: subscription did not exist " + remoteSubscriptionURL);
        }
        else {
            log.error("RegistrationActor.delete: failed to delete subscription " + webTarget.getUri().toASCIIString() + ", result = " + response.getStatusInfo().getReasonPhrase());
            ErrorType error = response.readEntity(ErrorType.class);
            if (error != null) {
                log.error("RegistrationActor.delete: id=" + error.getId() + ", label=" + error.getLabel() + ", resource=" + error.getResource() + ", description=" + error.getDescription());
            }

            response.close();
            //client.close();
            return false;
        }

        response.close();
        //client.close();

        return true;
    }
}
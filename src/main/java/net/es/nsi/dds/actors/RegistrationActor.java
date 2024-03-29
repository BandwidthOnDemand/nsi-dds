package net.es.nsi.dds.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import net.es.nsi.dds.client.RestClient;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.dao.RemoteSubscription;
import net.es.nsi.dds.dao.RemoteSubscriptionCache;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.DocumentEventType;
import net.es.nsi.dds.jaxb.dds.ErrorType;
import net.es.nsi.dds.jaxb.dds.FilterCriteriaType;
import net.es.nsi.dds.jaxb.dds.FilterType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.jaxb.dds.SubscriptionListType;
import net.es.nsi.dds.jaxb.dds.SubscriptionRequestType;
import net.es.nsi.dds.jaxb.dds.SubscriptionType;
import net.es.nsi.dds.management.logs.DdsErrors;
import net.es.nsi.dds.management.logs.DdsLogger;
import net.es.nsi.dds.management.logs.DdsLogs;
import net.es.nsi.dds.messages.Message;
import net.es.nsi.dds.messages.RegistrationEvent;
import net.es.nsi.dds.messages.RegistrationEvent.Event;
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.util.UrlHelper;
import org.apache.http.client.utils.DateUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The Registration Actor handles local DDS subscription registrations to peer
 * DDS services.
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class RegistrationActor extends UntypedAbstractActor {
    private static final String NOTIFICATIONS_URL = "notifications";
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final DdsLogger logger = DdsLogger.getLogger();
    private final ObjectFactory factory = new ObjectFactory();
    private final DdsConfiguration discoveryConfiguration;
    private final RemoteSubscriptionCache remoteSubscriptionCache;
    private final RestClient restClient;

    /**
     * Default constructor called by Spring to initialize the actor.
     *
     * @param discoveryConfiguration
     * @param remoteSubscriptionCache
     */
    public RegistrationActor(DdsConfiguration discoveryConfiguration, RemoteSubscriptionCache remoteSubscriptionCache) {
        this.discoveryConfiguration = discoveryConfiguration;
        this.remoteSubscriptionCache = remoteSubscriptionCache;
        this.restClient = RestClient.getInstance();
    }

    /**
     * Nothing to do for initialization.
     */
    @Override
    public void preStart() {
    }

    /**
     * Process an incoming subscription message to the actor.
     *
     * @param msg
     */
    @Override
    public void onReceive(Object msg) {
        log.debug("[RegistrationActor] onReceive {}", Message.getDebug(msg));

        if (msg instanceof RegistrationEvent) {
            RegistrationEvent event = (RegistrationEvent) msg;
            log.debug("[RegistrationActor] event={}, url={}", event.getEvent().name(), event.getUrl());

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
                    log.error("[RegistrationActor] onReceive unhandled event {} {} {}",
                        event.getEvent(), this.getSender(), Message.getDebug(msg));
                    unhandled(msg);
                    break;
            }
        } else {
            log.error("[RegistrationActor] onReceive unhandled message {} {}", this.getSender(), Message.getDebug(msg));
            unhandled(msg);
        }

        log.debug("[RegistrationActor] onReceive done.");
    }

    /**
     * Build the base notification URL for this nsi-dds instance.
     *
     * @return
     * @throws MalformedURLException
     */
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
            log.error("RegistrationActor.register: failed to get my notification callback URL, failing registration for {}", remoteDdsURL, mx);
            logger.error(DdsErrors.DDS_CONFIGURATION_INVALID_LOCAL_DDS_URL, remoteDdsURL);
            return;
        }

        Client client = restClient.get();
        WebTarget webTarget = client.target(remoteDdsURL).path("subscriptions");

        Response response = null;
        try {
            log.debug("RegistrationActor.register: registering with remote DDS {}", remoteDdsURL);

            String encoded = DdsParser.getInstance().subscriptionRequest2Xml(request);
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML)
                    .header(HttpHeaders.CONTENT_ENCODING, "gzip")
                    .post(Entity.entity(encoded, NsiConstants.NSI_DDS_V1_XML));

            if (response.getStatusInfo().getStatusCode() == Response.Status.CREATED.getStatusCode()) {
                // Looks like we were successful so save the subscription information.
                SubscriptionType newSubscription = DdsParser.getInstance().xml2Subscription(response.readEntity(String.class));

                logger.log(DdsLogs.DDS_SUBSCRIPTION_CREATED, remoteDdsURL, newSubscription.getHref());

                log.debug("RegistrationActor.register: registered with remote DDS {}, id={}", remoteDdsURL, newSubscription.getId());

                RemoteSubscription remoteSubscription = new RemoteSubscription();
                remoteSubscription.setDdsURL(remoteDdsURL);
                remoteSubscription.setSubscription(newSubscription);
                if (response.getLastModified() == null) {
                    // We should have gotten a valid lastModified date back.  Fake one
                    // until we have worked out all the failure cases.  This will open
                    // a small window of inaccuracy.
                    log.error("RegistrationActor.register: invalid LastModified header for id={}, href={}", newSubscription.getId(), newSubscription.getHref());
                    remoteSubscription.setCreated(new Date((System.currentTimeMillis() / 1000) * 1000 ));
                }
                else {
                    remoteSubscription.setCreated(response.getLastModified());
                }

                remoteSubscriptionCache.add(remoteSubscription);

                // Now that we have registered a new subscription make sure we clean up
                // and old ones that may exist on the remote DDS.
                deleteOldSubscriptions(remoteDdsURL, newSubscription.getId());
            }
            else {
                log.error("RegistrationActor.register: failed to create subscription {}, result = {}", remoteDdsURL, response.getStatusInfo().getReasonPhrase());

                ErrorType error = DdsParser.getInstance().xml2Error(response.readEntity(String.class));
                if (error != null) {
                    log.error("RegistrationActor.register: Error id={}, label={}, resource={}, description={}",
                            error.getId(), error.getLabel(), error.getResource(),
                            error.getDescription());
                    logger.error(DdsErrors.DDS_SUBSCRIPTION_ADD_FAILED_DETAILED, remoteDdsURL, error.getId());
                }
                else {
                    logger.error(DdsErrors.DDS_SUBSCRIPTION_ADD_FAILED_DETAILED, remoteDdsURL, response.getStatusInfo().getReasonPhrase());
                }
            }
        }
        catch (Exception ex) {
            log.error(ex, "RegistrationActor.register: error on endpoint {}", remoteDdsURL);
            logger.error(DdsErrors.DDS_SUBSCRIPTION_ADD_FAILED, remoteDdsURL);
        }
        finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Delete a subscription on remote nsi-dds peer.
     *
     * @param remoteDdsURL
     * @param id
     */
    private void deleteOldSubscriptions(String remoteDdsURL, String id) {
        Client client = restClient.get();

        WebTarget webTarget = client.target(remoteDdsURL).path("subscriptions").queryParam("requesterId", discoveryConfiguration.getNsaId());
        Response response = null;
        try {
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).get();

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                // Looks like we were successful so save the subscription information.
                SubscriptionListType subscriptions = DdsParser.getInstance().xml2Subscriptions(response.readEntity(String.class));

                // For each subscription returned registered to our nsaId we check to
                // see if it is the one we just registered (current subscription).  If
                // it is not we delete the subscription.
                subscriptions.getSubscription()
                    .stream()
                    .filter((subscription) -> (!id.equalsIgnoreCase(subscription.getId())))
                    .map((subscription) -> {
                        // Found one we need to remove.
                        log.debug("deleteOldSubscriptions: found stale subscription {} on DDS {}", subscription.getHref(), webTarget.getUri().toASCIIString());
                        return subscription;
                    })
                    .forEach((subscription) -> {
                        deleteSubscription(remoteDdsURL, subscription.getHref());
                    });
            }
            else {
                log.error("Failed to retrieve list of subscriptions {}, result = {}", remoteDdsURL, response.getStatusInfo().getReasonPhrase());
                ErrorType error = DdsParser.getInstance().xml2Error(response.readEntity(String.class));
                if (error != null) {
                    logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED_DETAILED, webTarget.getUri().toASCIIString(), error.getId());
                }
                else {
                    logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED_DETAILED, webTarget.getUri().toASCIIString(), response.getStatusInfo().getReasonPhrase());
                }
            }
        }
        catch (Exception ex) {
            log.error(ex,"GET failed for {}", webTarget.getUri().toASCIIString());
            logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED, webTarget.getUri().toASCIIString());
        }
        finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Update a subscription on a remote nsi-dds peer.
     *
     * @param event
     * @throws IllegalArgumentException
     */
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
        Response response = null;
        try {
            log.debug("RegistrationActor.update: getting subscription {},lastModified = {}",
                absoluteURL, remoteSubscription.getLastModified());
            response = webTarget.request(NsiConstants.NSI_DDS_V1_XML)
                .header("If-Modified-Since",
                    DateUtils.formatDate(remoteSubscription.getLastModified(), DateUtils.PATTERN_RFC1123)).get();

            // We found the subscription and it was not updated.
            if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
                // The subscription exists and has not been modified.
                log.debug("RegistrationActor.update: subscription {} exists (not modified).", absoluteURL);
                remoteSubscription.setLastSuccessfulAudit(new Date());
            }
            // We found the subscription and it was updated.
            else if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                // The subscription exists but was modified since our last query.
                // Save the new version even though we should have know about it.
                remoteSubscription.setLastModified(response.getLastModified());
                remoteSubscription.setLastSuccessfulAudit(new Date());
                SubscriptionType update = DdsParser.getInstance().xml2Subscription(response.readEntity(String.class));
                remoteSubscription.setSubscription(update);
                logger.log(DdsLogs.DDS_SUBSCRIPTION_UPDATE_DETECTED, absoluteURL, response.getLastModified().toString());
            }
            // We did not find the subscription so will need to create a new one.
            else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                // Looks like our subscription was removed. We need to add it back in.
                logger.error(DdsErrors.DDS_SUBSCRIPTION_NOT_FOUND, absoluteURL);

                // Remove the stored subscription since a new one will be created.
                remoteSubscriptionCache.remove(remoteSubscription.getDdsURL());
                event.setEvent(Event.Register);
                register(event);
            }
            // An unexpected error has occured.
            else {
                // Some other error we cannot handle at the moment.
                ErrorType error = DdsParser.getInstance().xml2Error(response.readEntity(String.class));
                if (error != null) {
                    logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED_DETAILED, absoluteURL, error.getId());
                }
                else {
                    logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED_DETAILED, absoluteURL, response.getStatusInfo().getReasonPhrase());
                }
            }
        }
        catch (Exception ex) {
            log.error(ex,"GET failed for {}", absoluteURL);
            logger.error(DdsErrors.DDS_SUBSCRIPTION_GET_FAILED, absoluteURL);
        }
        finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Remove a peer subscription from the local subscription cache.
     *
     * @param event
     * @throws IllegalArgumentException
     */
    private void delete(RegistrationEvent event) throws IllegalArgumentException {
        if (event.getEvent() != Event.Delete) {
            throw new IllegalArgumentException("delete: invalid event type " + event.getEvent());
        }

        RemoteSubscription subscription = remoteSubscriptionCache.get(event.getUrl());
        if (deleteSubscription(subscription.getDdsURL(), subscription.getSubscription().getHref())) {
            remoteSubscriptionCache.remove(subscription.getDdsURL());
        }
    }

    /**
     * Delete a subscription on the remote nsi-dds peer.
     *
     * @param remoteDdsURL
     * @param remoteSubscriptionURL
     * @return
     */
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

        boolean result = true;
        try (Response response = webTarget.request(NsiConstants.NSI_DDS_V1_XML).delete()) {

            if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                // Successfully deleted the subscription.
                logger.log(DdsLogs.DDS_SUBSCRIPTION_DELETED, absoluteURL);
            }
            else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                logger.error(DdsErrors.DDS_SUBSCRIPTION_DELETE_FAILED_DETAILED, absoluteURL, response.getStatusInfo().getReasonPhrase());
            }
            else {
                log.error("RegistrationActor.delete: failed to delete subscription {}, result = {}", absoluteURL, response.getStatusInfo().getReasonPhrase());
                ErrorType error = DdsParser.getInstance().xml2Error(response.readEntity(String.class));
                if (error != null) {
                    logger.error(DdsErrors.DDS_SUBSCRIPTION_DELETE_FAILED_DETAILED, absoluteURL, error.getId());
                }
                else {
                    logger.error(DdsErrors.DDS_SUBSCRIPTION_DELETE_FAILED_DETAILED, absoluteURL, response.getStatusInfo().getReasonPhrase());
                }
                result = false;
            }

        }
        catch (Exception ex) {
            log.error(ex,"Failed to delete subscription {}", absoluteURL);
            logger.error(DdsErrors.DDS_SUBSCRIPTION_DELETE_FAILED, absoluteURL);
            result = false;
        }
        return result;
    }
}
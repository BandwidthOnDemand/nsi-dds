package net.es.nsi.dds.actors;

import akka.actor.UntypedActor;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.client.RestClient;
import net.es.nsi.dds.config.ConfigurationManager;
import net.es.nsi.dds.jaxb.dds.NotificationListType;
import net.es.nsi.dds.jaxb.dds.NotificationType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.messages.Notification;
import net.es.nsi.dds.provider.DiscoveryProvider;
import net.es.nsi.dds.util.XmlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Notification Actor delivers notifications to a specific DDS peer based
 * on a previously filtered list of documents.
 *
 * @author hacksaw
 */
public class NotificationActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();
    private final String providerId;
    private final RestClient restClient;

    public NotificationActor(String providerId, RestClient restClient) {
        this.providerId = providerId;
        this.restClient = restClient;
    }

    @Override
    public void preStart() {
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Notification) {
            Notification notification = (Notification) msg;
            log.debug("NotificationActor: notificationId={}, requesterId={}", notification.getSubscription().getId(), notification.getSubscription().getSubscription().getRequesterId());

            NotificationListType list = getNotificationList(notification);
            String callback = notification.getSubscription().getSubscription().getCallback();
            Client client = restClient.get();

            final WebTarget webTarget = client.target(callback);
            JAXBElement<NotificationListType> jaxb = factory.createNotifications(list);
            String mediaType = notification.getSubscription().getEncoding();

            log.debug("NotificationActor: sending mediaType={}, subscriptionId={}, callback={}", mediaType, notification.getSubscription().getId(), notification.getSubscription().getSubscription().getCallback());

            Response response = null;
            try {
                response = webTarget.request(mediaType).header(HttpHeaders.CONTENT_ENCODING, "gzip")
                    .post(Entity.entity(new GenericEntity<JAXBElement<NotificationListType>>(jaxb) {}, mediaType));

                if (response.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
                    log.debug("NotificationActor: sent notitifcation {} to client {}, result = {}", list.getId(), callback, response.getStatusInfo().getReasonPhrase());
                }
                else {
                    log.error("NotificationActor: failed notification {} to client {}, result = {}", list.getId(), callback, response.getStatusInfo().getReasonPhrase());
                    // TODO: Tell discovery provider...
                    DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
                    discoveryProvider.deleteSubscription(notification.getSubscription().getId());
                }
            }
            catch (Exception ex) {
                log.error("NotificationActor: failed notification {} to client {}" + list.getId(), callback, ex);
                DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
                discoveryProvider.deleteSubscription(notification.getSubscription().getId());
            }
            finally {
                if (response != null) {
                    response.close();
                }
            }
        } else {
            unhandled(msg);
        }
    }

    private NotificationListType getNotificationList(Notification notification) {
        NotificationListType list = factory.createNotificationListType();

        notification.getDocuments().stream().map((document) -> {
            log.debug("NotificationActor: documentId={}", document.getDocument().getId());
            return document;
        }).map((document) -> {
            NotificationType notify = factory.createNotificationType();
            notify.setEvent(notification.getEvent());
            notify.setDocument(document.getDocument());
            try {
                XMLGregorianCalendar discovered = XmlUtilities.longToXMLGregorianCalendar(document.getLastDiscovered().getTime());
                notify.setDiscovered(discovered);
            }
            catch (Exception ex) {
                log.error("NotificationActor: discovered date conversion failed", ex);
            }
            return notify;
        }).forEach((notify) -> {
            list.getNotification().add(notify);
        });

        list.setId(notification.getSubscription().getId());
        list.setHref(notification.getSubscription().getSubscription().getHref());
        list.setProviderId(providerId);

        return list;
    }
}

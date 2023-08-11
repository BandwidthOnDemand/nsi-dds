package net.es.nsi.dds.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.client.RestClient;
import net.es.nsi.dds.config.ConfigurationManager;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.NotificationListType;
import net.es.nsi.dds.jaxb.dds.NotificationType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.messages.Message;
import net.es.nsi.dds.messages.Notification;
import net.es.nsi.dds.provider.DiscoveryProvider;
import net.es.nsi.dds.util.XmlUtilities;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The Notification Actor delivers notifications to a specific DDS peer based
 * on a previously filtered list of documents.
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class NotificationActor extends UntypedAbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private final ObjectFactory factory = new ObjectFactory();
  private final String providerId;
  private final RestClient restClient;

  /**
   * Class constructor.  This class is a Spring managed bean.
   * @param providerId The provider URN of this DDS service.
   * @param restClient  The REST client to use for delivering notification messages.
   */
  public NotificationActor(String providerId, RestClient restClient) {
    this.providerId = providerId;
    this.restClient = restClient;
  }

  /**
   * A method invoked before the actor is started. We have nothing to do here
   * at the moment.
   */
  @Override
  public void preStart() {
    log.debug("NotificationActor: starting...");
  }

  /**
   * Deliver a list of changed documents to all registered subscriptions.
   *
   * @param msg
   */
  @Override
  public void onReceive(Object msg) {
    log.debug("[NotificationActor] onReceive {}", Message.getDebug(msg));

    // We only accept Notification messages.
    if (msg instanceof Notification) {
      Notification notification = (Notification) msg;
      String requesterId = notification.getSubscription().getSubscription().getRequesterId();
      String id = notification.getSubscription().getId();
      String callback = notification.getSubscription().getSubscription().getCallback();
      String mediaType = notification.getSubscription().getEncoding();

      log.debug("[NotificationActor] sending requesterId={}, id={}, mediaType={}, callback={}",
              requesterId, id, mediaType, callback);

      NotificationListType list = getNotificationList(notification);
      final WebTarget webTarget = restClient.get().target(callback);

      Response response = null;
      boolean error = false;
      try {
        String encoded = DdsParser.getInstance().notifications2Xml(list);
        response = webTarget.request(mediaType).header(HttpHeaders.CONTENT_ENCODING, "gzip")
                .post(Entity.entity(encoded, mediaType));

        if (response.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
          log.debug("[NotificationActor] sent notification = {} to client = {}, result = {}",
                  list.getId(), callback, response.getStatusInfo().getReasonPhrase());
        } else {
          log.error("[NotificationActor] failed notification = {} to client = {}, code = {}, result = {}",
                  list.getId(), callback, response.getStatusInfo().getStatusCode(),
                  response.getStatusInfo().getReasonPhrase());
          // TODO: Tell discovery provider...
          error = true;
        }
      } catch (Exception ex) {
        // Do not change this to specific Exceptions unless you keep the
        // generic catch due to underlying SSL exceptions from the SSL
        // provider.
        log.error(ex, "[NotificationActor] failed notification = {} to client = {}",
                list.getId(), callback);
        error = true;
      } finally {
        log.debug("[NotificationActor] finally - requesterId = {}, id = {}, callback = {}",
                requesterId, id, callback);

        if (response != null) {
          response.close();
        }

        if (error) {
          log.error("[NotificationActor] deleting requesterId={}, id={}, callback={}",
              requesterId, id, callback);
          DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
          discoveryProvider.deleteSubscription(notification.getSubscription().getId());
        }
      }

      log.debug("[NotificationActor] notification sent requesterId={}, id={}, callback={}",
              requesterId, id, callback);
    } else {
      log.error("[NotificationActor] onReceive unhandled message {} {}", this.getSender(), Message.getDebug(msg));
      unhandled(msg);
    }

    log.debug("[NotificationActor] onReceive done.");
  }

  /**
   * Return the list of notification targets.
   *
   * @param notification
   * @return
   */
  private NotificationListType getNotificationList(Notification notification) {
    NotificationListType list = factory.createNotificationListType();

    notification.getDocuments().stream().map((document) -> {
      log.debug("[NotificationActor] getNotificationList documentId={}", document.getDocument().getId());
      return document;
    }).map((document) -> {
      NotificationType notify = factory.createNotificationType();
      notify.setEvent(notification.getEvent());
      notify.setDocument(document.getDocument());
      try {
        XMLGregorianCalendar discovered = XmlUtilities.longToXMLGregorianCalendar(document.getLastDiscovered().getTime());
        notify.setDiscovered(discovered);
      } catch (Exception ex) {
        log.error("[NotificationActor] getNotificationList discovered date conversion failed", ex);
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

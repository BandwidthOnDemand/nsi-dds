package net.es.nsi.dds.actors;

import akka.actor.UntypedActor;
import java.io.IOException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.client.RestClient;
import net.es.nsi.dds.config.ConfigurationManager;
import net.es.nsi.dds.jaxb.DdsParser;
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
    // We only accept Notification messages.
    if (msg instanceof Notification) {
      Notification notification = (Notification) msg;
      log.debug("NotificationActor: subscriptionId = {}, requesterId = {}",
              notification.getSubscription().getId(),
              notification.getSubscription().getSubscription().getRequesterId());

      NotificationListType list = getNotificationList(notification);
      String callback = notification.getSubscription().getSubscription().getCallback();
      Client client = restClient.get();

      final WebTarget webTarget = client.target(callback);
      String mediaType = notification.getSubscription().getEncoding();

      log.debug("NotificationActor: sending mediaType={}, subscriptionId={}, callback={}",
              mediaType, notification.getSubscription().getId(),
              notification.getSubscription().getSubscription().getCallback());

      Response response = null;
      boolean error = false;
      try {
        String encoded = DdsParser.getInstance().notifications2Xml(list);
        response = webTarget.request(mediaType).header(HttpHeaders.CONTENT_ENCODING, "gzip")
                .post(Entity.entity(encoded, mediaType));

        if (response.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
          log.debug("NotificationActor: sent notitifcation = {} to client = {}, result = {}",
                  list.getId(), callback, response.getStatusInfo().getReasonPhrase());
        } else {
          log.error("NotificationActor: failed notification = {} to client = {}, code = {}, result = {}",
                  list.getId(), callback, response.getStatusInfo().getStatusCode(),
                  response.getStatusInfo().getReasonPhrase());
          // TODO: Tell discovery provider...
          error = true;
        }
      } catch (IOException | JAXBException | WebApplicationException | ProcessingException ex) {
        // Do not change this to specific Exceptions unless you keep the
        // generic catch due to underlying SSL exceptions from the SSL
        // provider.
        log.error("NotificationActor: failed notification = {} to client = {}, ex = {}",
                list.getId(), callback, ex);
        error = true;
      } finally {
        log.debug("NotificationActor: completed - subscriptionId = {}, requesterId = {}, callback = {}",
                notification.getSubscription().getId(),
                notification.getSubscription().getSubscription().getRequesterId(),
                notification.getSubscription().getSubscription().getCallback());

        if (response != null) {
          response.close();
        }

        if (error) {
          DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
          discoveryProvider.deleteSubscription(notification.getSubscription().getId());
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
      } catch (Exception ex) {
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

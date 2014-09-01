/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import net.es.nsi.pce.discovery.messages.Notification;
import akka.actor.UntypedActor;
import java.util.Date;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.config.ConfigurationManager;
import net.es.nsi.pce.discovery.dao.DiscoveryConfiguration;
import net.es.nsi.pce.discovery.dao.DiscoveryParser;
import net.es.nsi.dds.api.jaxb.NotificationListType;
import net.es.nsi.dds.api.jaxb.NotificationType;
import net.es.nsi.dds.api.jaxb.ObjectFactory;
import net.es.nsi.pce.discovery.provider.DiscoveryProvider;
import net.es.nsi.pce.discovery.provider.Document;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.schema.XmlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class NotificationActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();
    private final DiscoveryConfiguration discoveryConfiguration;
    private final RestClient restClient;

    public NotificationActor(DiscoveryConfiguration discoveryConfiguration, RestClient restClient) {
        this.discoveryConfiguration = discoveryConfiguration;
        this.restClient = restClient;
    }

    @Override
    public void preStart() {
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Notification) {
            Notification notification = (Notification) msg;
            log.debug("NotificationActor: notificationId=" + notification.getSubscription().getId());

            NotificationListType list = factory.createNotificationListType();

            Date lastDiscovered = new Date(0);
            for (Document document : notification.getDocuments()) {
                log.debug("NotificationActor: documentId=" + document.getDocument().getId());
                NotificationType notify = factory.createNotificationType();
                notify.setEvent(notification.getEvent());
                notify.setDocument(document.getDocument());
                try {
                    XMLGregorianCalendar discovered = XmlUtilities.longToXMLGregorianCalendar(document.getLastDiscovered().getTime());
                    notify.setDiscovered(discovered);

                    if (lastDiscovered.before(document.getLastDiscovered())) {
                        lastDiscovered = document.getLastDiscovered();
                    }
                }
                catch (Exception ex) {
                    log.error("NotificationActor: discovered date conversion failed", ex);
                }
                list.getNotification().add(notify);
            }

            list.setId(notification.getSubscription().getId());
            list.setHref(notification.getSubscription().getSubscription().getHref());
            list.setProviderId(discoveryConfiguration.getNsaId());
            try {
                XMLGregorianCalendar discovered = XmlUtilities.longToXMLGregorianCalendar(lastDiscovered.getTime());
                list.setDiscovered(discovered);
            }
            catch (Exception ex) {
                log.error("NotificationActor: discovered date conversion failed", ex);
            }

            String callback = notification.getSubscription().getSubscription().getCallback();
            Client client = restClient.get();

            final WebTarget webTarget = client.target(callback);
            JAXBElement<NotificationListType> jaxb = factory.createNotifications(list);
            String mediaType = notification.getSubscription().getEncoding();
            //String jaxbToString = DiscoveryParser.getInstance().jaxbToString(jaxb);
            //log.debug("Notification to send:\n" + jaxbToString);

            try {
                Response response = webTarget.request(mediaType)
                    .post(Entity.entity(new GenericEntity<JAXBElement<NotificationListType>>(jaxb) {}, mediaType));

                if (response.getStatus() != Response.Status.ACCEPTED.getStatusCode()) {
                    log.error("NotificationActor: failed notification " + list.getId() + " to client " + callback + ", result = " + response.getStatusInfo().getReasonPhrase());
                    // TODO: Tell discovery provider...
                    DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
                    discoveryProvider.deleteSubscription(notification.getSubscription().getId());
                }
                else if (log.isDebugEnabled()) {
                    log.debug("NotificationActor: sent notitifcation " + list.getId() + " to client " + callback + ", result = " + response.getStatusInfo().getReasonPhrase());
                }

                response.close();
            }
            catch (WebApplicationException ex) {
                log.error("NotificationActor: failed notification " + list.getId() + " to client " + callback, ex);
                DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
                discoveryProvider.deleteSubscription(notification.getSubscription().getId());
            }

            //client.close();
        } else {
            unhandled(msg);
        }
    }
}
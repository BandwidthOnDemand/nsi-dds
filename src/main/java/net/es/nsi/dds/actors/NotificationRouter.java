/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.actors;

import net.es.nsi.dds.messages.Notification;
import net.es.nsi.dds.messages.DocumentEvent;
import net.es.nsi.dds.messages.SubscriptionEvent;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.es.nsi.dds.dao.DiscoveryConfiguration;
import net.es.nsi.dds.api.jaxb.DocumentEventType;
import net.es.nsi.dds.provider.DiscoveryProvider;
import net.es.nsi.dds.provider.Document;
import net.es.nsi.dds.dao.DocumentCache;
import net.es.nsi.dds.provider.Subscription;
import net.es.nsi.dds.jersey.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class NotificationRouter extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsActorSystem ddsActorSystem;
    private DiscoveryConfiguration discoveryConfiguration;
    private DiscoveryProvider discoveryProvider;
    private DocumentCache documentCache;
    private RestClient restClient;
    private int poolSize;
    private Router router;

    public NotificationRouter(DdsActorSystem ddsActorSystem,
            DiscoveryConfiguration discoveryConfiguration,
            DiscoveryProvider discoveryProvider,
            DocumentCache documentCache,
            RestClient restClient) {
        this.ddsActorSystem = ddsActorSystem;
        this.discoveryConfiguration = discoveryConfiguration;
        this.discoveryProvider = discoveryProvider;
        this.documentCache = documentCache;
        this.restClient = restClient;
    }

    @Override
    public void preStart() {
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < getPoolSize(); i++) {
            ActorRef r = getContext().actorOf(Props.create(NotificationActor.class, discoveryConfiguration, restClient));
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof DocumentEvent) {
            // We have a document event.
            DocumentEvent de = (DocumentEvent) msg;
            log.debug("NotificationRouter: document event " + de.getEvent() + ", id="  + de.getDocument().getId());
            routeDocumentEvent(de);
        }
        else if (msg instanceof SubscriptionEvent) {
            // We have a subscription event.
            SubscriptionEvent se = (SubscriptionEvent) msg;
            log.debug("NotificationRouter: subscription event id=" + se.getSubscription().getId());
            routeSubscriptionEvent(se);
        }
        else if (msg instanceof Terminated) {
            log.debug("NotificationRouter: terminate event.");
            router = router.removeRoutee(((Terminated) msg).actor());
            ActorRef r = getContext().actorOf(Props.create(NotificationActor.class));
            getContext().watch(r);
            router = router.addRoutee(new ActorRefRoutee(r));
        }
        else {
            log.debug("NotificationRouter: unhandled event." + msg.getClass().getName());
            unhandled(msg);
        }
    }

    private void routeDocumentEvent(DocumentEvent de) {
        Collection<Subscription> subscriptions = discoveryProvider.getSubscriptions(de);

        log.debug("routeDocumentEvent: event=" + de.getEvent() + ", documentId=" + de.getDocument().getId());

        // We need to sent the list of matching documents to the callback
        // related to this subscription.  Only send if there is no pending
        // subscription event.
        for (Subscription subscription : subscriptions) {
            log.debug("routeDocumentEvent: subscription=" + subscription.getId() + ", endpoint=" + subscription.getSubscription().getCallback());
            if (subscription.getAction() == null) {
                Notification notification = new Notification();
                notification.setEvent(de.getEvent());
                notification.setSubscription(subscription);
                Collection<Document> documents = new ArrayList<>();
                documents.add(de.getDocument());
                notification.setDocuments(documents);
                router.route(notification, getSender());
            }
        }
    }

    private void routeSubscriptionEvent(SubscriptionEvent se) {

        // TODO: Apply subscription filter to these documents.
        Collection<Document> documents = documentCache.values();

        // Clean up our trigger event.
        log.debug("routeSubscriptionEvent: event=" + se.getEvent() + ", action=" + se.getSubscription().getAction().isCancelled());
        se.getSubscription().setAction(null);

        // We need to sent the list of matching documents to the callback
        // related to this subscription.
        Notification notification = new Notification();
        notification.setEvent(DocumentEventType.ALL);
        notification.setSubscription(se.getSubscription());
        notification.setDocuments(documents);
        router.route(notification, getSender());
    }

    /**
     * @return the poolSize
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * @param poolSize the poolSize to set
     */
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public Cancellable scheduleNotification(Object message, long delay) {
        Cancellable scheduleOnce = ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(delay, TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);
        return scheduleOnce;
    }

    public void sendNotification(Object message) {
        this.getSelf().tell(message, null);
    }
}

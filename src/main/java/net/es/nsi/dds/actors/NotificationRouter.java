package net.es.nsi.dds.actors;

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
import net.es.nsi.dds.client.RestClient;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.dao.DocumentCache;
import net.es.nsi.dds.jaxb.dds.DocumentEventType;
import net.es.nsi.dds.messages.DocumentEvent;
import net.es.nsi.dds.messages.Notification;
import net.es.nsi.dds.messages.StartMsg;
import net.es.nsi.dds.messages.SubscriptionEvent;
import net.es.nsi.dds.provider.DiscoveryProvider;
import net.es.nsi.dds.provider.Document;
import net.es.nsi.dds.provider.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 * This Notification Router will route notification messages to the target
 * actors based on notification type.
 *
 * @author hacksaw
 */
public class NotificationRouter extends UntypedActor {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DdsActorSystem ddsActorSystem;
    private final DdsConfiguration discoveryConfiguration;
    private final DiscoveryProvider discoveryProvider;
    private final DocumentCache documentCache;
    private final RestClient restClient;
    private int poolSize;
    private int notificationSize;
    private Router router;

    public NotificationRouter(DdsActorSystem ddsActorSystem,
            DdsConfiguration discoveryConfiguration,
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
            ActorRef r = getContext().actorOf(Props.create(NotificationActor.class, discoveryConfiguration.getNsaId(), restClient));
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
            log.debug("NotificationRouter: document event {}, id={}", de.getEvent(), de.getDocument().getId());
            routeDocumentEvent(de);
        }
        else if (msg instanceof SubscriptionEvent) {
            // We have a subscription event.
            SubscriptionEvent se = (SubscriptionEvent) msg;
            log.debug("NotificationRouter: subscription event id={}, requesterId={}", se.getSubscription().getId(), se.getSubscription().getSubscription().getRequesterId());
            routeSubscriptionEvent(se);
        }
        else if (msg instanceof Terminated) {
            log.debug("NotificationRouter: terminate event.");
            router = router.removeRoutee(((Terminated) msg).actor());
            ActorRef r = getContext().actorOf(Props.create(NotificationActor.class));
            getContext().watch(r);
            router = router.addRoutee(new ActorRefRoutee(r));
        }
        else if (msg instanceof StartMsg) {
            // We ignore these for now as we have no specific start task.
        }
        else {
            log.debug("NotificationRouter: unhandled event = {}", msg.getClass().getName());
            unhandled(msg);
        }
    }

    private void routeDocumentEvent(DocumentEvent de) {
        Collection<Subscription> subscriptions = discoveryProvider.getSubscriptions(de);

        log.debug("routeDocumentEvent: event={}, documentId={}", de.getEvent(), de.getDocument().getId());

        // We need to sent the list of matching documents to the callback
        // related to this subscription.  Only send if there is no pending
        // subscription event.
        subscriptions.stream().map((subscription) -> {
            log.debug("routeDocumentEvent: id={}, subscription={}, endpoint={}", de.getDocument().getId(), subscription.getId(), subscription.getSubscription().getCallback());
            return subscription;
        }).filter((subscription) -> (subscription.getAction() == null)).map((subscription) -> {
            Notification notification = new Notification();
            notification.setEvent(de.getEvent());
            notification.setSubscription(subscription);
            return notification;
        }).map((notification) -> {
            Collection<Document> documents = new ArrayList<>();
            documents.add(de.getDocument());
            notification.setDocuments(documents);
            return notification;
        }).forEach((notification) -> {
            router.route(notification, getSender());
        });
    }

    private void routeSubscriptionEvent(SubscriptionEvent se) {
        // TODO: Apply subscription filter to these documents.
        Collection<Document> documents = documentCache.values();

        // Clean up our trigger event.
        log.debug("routeSubscriptionEvent: requesterId={}, event={}, documents={}, postSize={}, action={}",
                se.getSubscription().getSubscription().getRequesterId(),
                se.getEvent(), documents.size(), notificationSize,
                se.getSubscription().getAction().isCancelled());
        se.getSubscription().setAction(null);

        // Send documents in chunks of 10.
        Object[] toArray = documents.toArray();
        int current = 0;
        while (current < toArray.length) {
            // We need to sent the list of matching documents to the callback
            // related to this subscription.
            Notification notification = new Notification();
            notification.setEvent(DocumentEventType.ALL);
            notification.setSubscription(se.getSubscription());
            ArrayList<Document> docs = new ArrayList<>();
            for (int i = 0; i < notificationSize
                    && current < toArray.length; i++) {
                docs.add((Document) toArray[current]);
                current++;
            }
            notification.setDocuments(docs);
            router.route(notification, getSender());
        }
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

    /**
     * @return the notificationSize
     */
    public int getNotificationSize() {
        return notificationSize;
    }

    /**
     * @param notificationSize the notificationSize to set
     */
    public void setNotificationSize(int notificationSize) {
        this.notificationSize = notificationSize;
    }
}

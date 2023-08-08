package net.es.nsi.dds.actors;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
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
import net.es.nsi.dds.messages.*;
import net.es.nsi.dds.provider.DiscoveryProvider;
import net.es.nsi.dds.provider.Document;
import net.es.nsi.dds.provider.Subscription;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 * This Notification Router will route notification messages to the target
 * actors based on notification type.
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class NotificationRouter extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final DdsActorSystem ddsActorSystem;
    private final DdsConfiguration discoveryConfiguration;
    private final DiscoveryProvider discoveryProvider;
    private final DocumentCache documentCache;
    private final RestClient restClient;
    private int poolSize;
    private int notificationSize;
    private Router router;

    /**
     * Constructor called by Spring to instantiate a singleton instance.
     *
     * @param ddsActorSystem
     * @param discoveryConfiguration
     * @param discoveryProvider
     * @param documentCache
     * @param restClient
     */
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

    /**
     * Initializes the router actor by creating a pool of NotificationActor to
     * process notification workload.
     */
    @Override
    public void preStart() {
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < getPoolSize(); i++) {
            ActorRef r = getContext().actorOf(Props.create(NotificationActor.class,
                discoveryConfiguration.getNsaId(), restClient));
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    /**
     * Process an incoming message to the actor.  This is typically a type
     * of notification event.
     *
     * @param msg
     */
    @Override
    public void onReceive(Object msg) {
        log.debug("[NotificationRouter] onReceive {}", Message.getDebug(msg));

        if (msg instanceof DocumentEvent) {
            // We have a document event.
            DocumentEvent de = (DocumentEvent) msg;
            log.debug("[NotificationRouter] document event {}, id={}", de.getEvent(), de.getDocument().getId());
            routeDocumentEvent(de);
        }
        else if (msg instanceof SubscriptionEvent) {
            // We have a subscription event.
            SubscriptionEvent se = (SubscriptionEvent) msg;
            log.debug("[NotificationRouter] subscription event id={}, requesterId={}",
                se.getSubscription().getId(), se.getSubscription().getSubscription().getRequesterId());
            routeSubscriptionEvent(se);
        }
        else if (msg instanceof Terminated) {
            Terminated terminated = ((Terminated) msg);
            log.debug("[NotificationRouter] terminate event for {}", terminated.actor().path());
            router = router.removeRoutee(terminated.actor());
            ActorRef r = getContext().actorOf(Props.create(NotificationActor.class));
            getContext().watch(r);
            router = router.addRoutee(new ActorRefRoutee(r));
        }
        else if (msg instanceof StartMsg) {
            // We ignore these for now as we have no specific start task.
            log.debug("[NotificationRouter] StartMsg event ignored, {}", Message.getDebug(msg));
        }
        else {
            log.error("[NotificationRouter] unhandled event = {}", Message.getDebug(msg));
            unhandled(msg);
        }

        log.debug("[NotificationRouter] onReceive done.");
    }

    /**
     * Provides the specific logic to route a document event to targets.
     *
     * @param de
     */
    private void routeDocumentEvent(DocumentEvent de) {
        Collection<Subscription> subscriptions = discoveryProvider.getSubscriptions(de);

        log.debug("routeDocumentEvent: event={}, documentId={}", de.getEvent(), de.getDocument().getId());

        // We need to sent the list of matching documents to the callback
        // related to this subscription.  Only send if there is no pending
        // subscription event.
        subscriptions.stream().map((subscription) -> {
            log.debug("routeDocumentEvent: id={}, subscription={}, endpoint={}",
                de.getDocument().getId(), subscription.getId(), subscription.getSubscription().getCallback());
            return subscription;
        }).filter((subscription) -> (subscription.getAction() == null)).map((subscription) -> {
            Notification notification = new Notification("routeDocumentEvent", this.getSelf().path());
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

    /**
     * Provides the specific logic to route a subscription event to targets.
     *
     * @param se
     */
    private void routeSubscriptionEvent(SubscriptionEvent se) {
        // TODO: Apply subscription filter to these documents.
        Collection<Document> documents = documentCache.values();

        // Clean up our trigger event.
        log.debug("routeSubscriptionEvent: requesterId={}, id={}, action={}",
                se.getSubscription().getSubscription().getRequesterId(),
                se.getSubscription().getSubscription().getId(),
                se.getSubscription().getAction().isCancelled());
        log.debug("routeSubscriptionEvent: event={}, documents={}, postSize={}",
                se.getSubscription().getSubscription().getId(),
                se.getEvent(), documents.size(), notificationSize);
        se.getSubscription().setAction(null);

        // Send documents in chunks of 10.
        Object[] toArray = documents.toArray();
        int current = 0;
        while (current < toArray.length) {
            // We need to sent the list of matching documents to the callback
            // related to this subscription.
            Notification notification = new Notification("routeSubscriptionEvent", this.getSelf().path());
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
     * Get the notification actor pool size.
     *
     * @return the poolSize
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Set the notification actor pool size.
     *
     * @param poolSize the poolSize to set
     */
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    /**
     * Schedule a notification message delivery to an actor.
     *
     * @param message
     * @param delay
     * @return
     */
    public Cancellable scheduleNotification(Object message, long delay) {
        return ddsActorSystem.getActorSystem().scheduler()
            .scheduleOnce(Duration.create(delay, TimeUnit.SECONDS), this.getSelf(), message,
                ddsActorSystem.getActorSystem().dispatcher(), null);
    }

    /**
     * Send a notification message to an actor.
     *
     * @param message
     */
    public void sendNotification(Object message) {
        this.getSelf().tell(message, null);
    }

    /**
     * Get the max notification message size.
     *
     * @return the notificationSize
     */
    public int getNotificationSize() {
        return notificationSize;
    }

    /**
     * Set the max notification message size.
     *
     * @param notificationSize the notificationSize to set
     */
    public void setNotificationSize(int notificationSize) {
        this.notificationSize = notificationSize;
    }
}

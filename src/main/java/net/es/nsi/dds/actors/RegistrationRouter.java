package net.es.nsi.dds.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.concurrent.TimeUnit;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.dao.RemoteSubscription;
import net.es.nsi.dds.dao.RemoteSubscriptionCache;
import net.es.nsi.dds.jaxb.configuration.PeerURLType;
import net.es.nsi.dds.messages.Message;
import net.es.nsi.dds.messages.RegistrationEvent;
import net.es.nsi.dds.messages.StartMsg;
import net.es.nsi.dds.spring.SpringApplicationContext;
import net.es.nsi.dds.util.NsiConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 * The Registration Router handles routing and distribution of DDS subscription
 * registrations to RegistrationActors.
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class RegistrationRouter extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final DdsActorSystem ddsActorSystem;
    private final DdsConfiguration discoveryConfiguration;
    private int poolSize = 5;
    private long interval = 600;
    private Router router;
    private final RemoteSubscriptionCache remoteSubscriptionCache;

    /**
     * Default constructor called by Spring to initialize the actor.
     *
     * @param ddsActorSystem
     * @param discoveryConfiguration
     * @param remoteSubscriptionCache
     */
    public RegistrationRouter(DdsActorSystem ddsActorSystem,
            DdsConfiguration discoveryConfiguration,
            RemoteSubscriptionCache remoteSubscriptionCache) {
        this.ddsActorSystem = ddsActorSystem;
        this.discoveryConfiguration = discoveryConfiguration;
        this.remoteSubscriptionCache = remoteSubscriptionCache;
    }

    /**
     * Return a reference to the singleton instance of the RegistrationRouter Agent.
     *
     * @return
     */
    public static RegistrationRouter getInstance() {
        return SpringApplicationContext.getBean("registrationRouter", RegistrationRouter.class);
    }

    /**
     * Initialize the actor by creating pool of RegistrationActors to do the work.
     */
    @Override
    public void preStart() {
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < getPoolSize(); i++) {
            ActorRef r = getContext()
                .actorOf(Props.create(RegistrationActor.class, discoveryConfiguration, remoteSubscriptionCache));
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    /**
     * Process an incoming registration messages and distribute to actor pool.
     *
     * @param msg
     */
    @Override
    public void onReceive(Object msg) {
        log.debug("[RegistrationRouter] onReceive {}", Message.getDebug(msg));

        // Check to see if we got the go ahead to start registering.
        if (msg instanceof StartMsg) {
            log.debug("[RegistrationRouter] start timer so convert to registration event.");
            StartMsg message = (StartMsg) msg;

            // Create a Register event to start us off.
            RegistrationEvent event = new RegistrationEvent(message.getInitiator(), message.getPath());
            event.setEvent(RegistrationEvent.Event.Register);
            msg = event;
        }

        // Process the registration event types.
        if (msg instanceof RegistrationEvent) {
            RegistrationEvent re = (RegistrationEvent) msg;
            if (re.getEvent() == RegistrationEvent.Event.Register) {
                // This is our first time through after initialization.
                log.debug("[RegistrationRouter] routeRegister");
                routeRegister();
            }
            else if (re.getEvent() == RegistrationEvent.Event.Audit) {
                // A regular audit event.
                log.debug("[RegistrationRouter] routeAudit");
                routeAudit();
            }
            else if (re.getEvent() == RegistrationEvent.Event.Delete) {
                // We are shutting down so clean up.
                log.debug("[RegistrationRouter] routeShutdown");
                routeShutdown();
            }
        }
        else if (msg instanceof Terminated) {
            Terminated terminated = ((Terminated) msg);
            log.error("[RegistrationRouter] terminate event for {}", terminated.actor().path());
            router = router.removeRoutee(terminated.actor());
            ActorRef r = getContext().actorOf(Props.create(RegistrationActor.class, discoveryConfiguration));
            getContext().watch(r);
            router = router.addRoutee(new ActorRefRoutee(r));
        }
        else {
            log.error("[RegistrationRouter] unhandled event {}", Message.getDebug(msg));
            unhandled(msg);
        }

        RegistrationEvent event = new RegistrationEvent("RegistrationRouter", this.getSelf().path());
        event.setEvent(RegistrationEvent.Event.Audit);
        ddsActorSystem.getActorSystem().scheduler()
            .scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), event,
                ddsActorSystem.getActorSystem().dispatcher(), null);

        log.debug("[RegistrationRouter] onReceive done.");
    }

    /**
     * Route an incoming registration message to an actor for processing.
     */
    private void routeRegister() {
        // We need to register invoke a registration actor for each remote DDS
        // we are peering with.
        discoveryConfiguration.getDiscoveryURL().stream()
                .filter((url) -> (url.getType().equalsIgnoreCase(NsiConstants.NSI_DDS_V1_XML)))
                .forEach((url) -> {
            RegistrationEvent regEvent = new RegistrationEvent("RegistrationRouter", this.getSelf().path());
            regEvent.setEvent(RegistrationEvent.Event.Register);
            regEvent.setUrl(url.getValue());
            log.debug("routeRegister: url={}", url.getValue());
            router.route(regEvent, this.getSelf());
        });
    }

    /**
     * Route an incoming registration audit message to an actor for processing.
     */
    private void routeAudit() {
        // Check the list of discovery URL against what we already have.
        Collection<PeerURLType> discoveryURL = discoveryConfiguration.getDiscoveryURL();
        Set<String> subscriptionURL = Sets.newHashSet(remoteSubscriptionCache.keySet());

        discoveryURL.stream()
                .filter((url) -> (url.getType().equalsIgnoreCase(NsiConstants.NSI_DDS_V1_XML)))
                .forEach((url) -> {
            // See if we already have seen this URL.  If we have not then
            // we need to create a new remote subscription.
            RemoteSubscription sub = remoteSubscriptionCache.get(url.getValue());
            if (sub == null) {
                // We have not seen this before.
                log.debug("routeAudit: new registration for url={}", url.getValue());

                RegistrationEvent regEvent = new RegistrationEvent("RegistrationRouter", this.getSelf().path());
                regEvent.setEvent(RegistrationEvent.Event.Register);
                regEvent.setUrl(url.getValue());
                router.route(regEvent, this.getSelf());
            }
            else {
                // We have seen this URL before.
                log.debug("routeAudit: auditing registration for url={}", url.getValue());
                RegistrationEvent regEvent = new RegistrationEvent("RegistrationRouter", this.getSelf().path());
                regEvent.setEvent(RegistrationEvent.Event.Update);
                regEvent.setUrl(url.getValue());
                router.route(regEvent, this.getSelf());

                // Remove from the existing list as processed.
                subscriptionURL.remove(url.getValue());
            }
        });
        // We only handle direct DDS peers here.

        // Now we see if there are any URL we missed from the old list and
        // unsubscribe them since we seem to no longer be interested.
        subscriptionURL.stream()
                .map(remoteSubscriptionCache::get)
                .filter(Objects::nonNull).map((sub) -> {
            // Should always be true unless modified while we are processing.
            RegistrationEvent regEvent = new RegistrationEvent("RegistrationRouter", this.getSelf().path());
            regEvent.setEvent(RegistrationEvent.Event.Delete);
            regEvent.setUrl(sub.getDdsURL());
            return regEvent;
        }).forEach((regEvent) -> {
            router.route(regEvent, getSelf());
        });
    }

    /**
     * Route an incoming shutdown message to an actor for processing.
     */
    private void routeShutdown() {
        for (String url : Sets.newHashSet(remoteSubscriptionCache.keySet())) {
            RemoteSubscription sub = remoteSubscriptionCache.get(url);
            if (sub != null) { // Should always be true unless modified while we are processing.
                RegistrationEvent regEvent = new RegistrationEvent("RegistrationRouter", this.getSelf().path());
                regEvent.setEvent(RegistrationEvent.Event.Delete);
                regEvent.setUrl(sub.getDdsURL());
                router.route(regEvent, getSelf());
            }
        }
    }

    /**
     * Get the actor pool size.
     *
     * @return the poolSize
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Set the actor pool size.
     *
     * @param poolSize the poolSize to set
     */
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    /**
     * Get the audit interval time.
     *
     * @return the interval
     */
    public long getInterval() {
        return interval;
    }

    /**
     * Set the audit interval time.
     *
     * @param interval the interval to set
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * Return true if the provided URL is a valid subscription.
     *
     * @param url
     * @return
     */
    public boolean isSubscription(String url) {
        return remoteSubscriptionCache.values().stream()
            .anyMatch((subscription) -> (subscription.getSubscription().getHref().contentEquals(url)));
    }
}

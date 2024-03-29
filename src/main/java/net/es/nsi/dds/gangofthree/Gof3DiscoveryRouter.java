/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.gangofthree;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.es.nsi.dds.actors.DdsActorSystem;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.jaxb.configuration.PeerURLType;
import net.es.nsi.dds.messages.Message;
import net.es.nsi.dds.messages.StartMsg;
import net.es.nsi.dds.messages.TimerMsg;
import net.es.nsi.dds.util.NsiConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class Gof3DiscoveryRouter extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final DdsActorSystem ddsActorSystem;
    private final DdsConfiguration discoveryConfiguration;
    private int poolSize;
    private long interval;
    private long refresh;
    private Router router;
    private final Map<String, Gof3DiscoveryMsg> discovery = new ConcurrentHashMap<>();

    public Gof3DiscoveryRouter(DdsActorSystem ddsActorSystem, DdsConfiguration discoveryConfiguration) {
        this.ddsActorSystem = ddsActorSystem;
        this.discoveryConfiguration = discoveryConfiguration;
    }

    @Override
    public void preStart() {
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < getPoolSize(); i++) {
            ActorRef r = getContext().actorOf(Props.create(Gof3DiscoveryActor.class));
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    @Override
    public void onReceive(Object msg) {
        log.debug("[Gof3DiscoveryRouter] onReceive {}", Message.getDebug(msg));

        // Check to see if we got the go ahead to start registering.
        if (msg instanceof StartMsg) {
            // Create a Register event to start us off.
            log.debug("[Gof3DiscoveryRouter] timer event.");
            StartMsg sm = (StartMsg) msg;
            msg = new TimerMsg(sm.getInitiator(), sm.getPath());
        }

        if (msg instanceof TimerMsg) {
            routeTimerEvent();

            TimerMsg message = new TimerMsg("Gof3DiscoveryRouter", this.self().path());
            ddsActorSystem.getActorSystem().scheduler()
                    .scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS),
                            this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);
        }
        else if (msg instanceof Gof3DiscoveryMsg) {
            Gof3DiscoveryMsg incoming = (Gof3DiscoveryMsg) msg;

            log.debug("[Gof3DiscoveryRouter] discovery update for nsaId={}, nsaURL={}",
                    incoming.getNsaId(), incoming.getNsaURL());

            discovery.put(incoming.getNsaURL(), incoming);
        }
        else if (msg instanceof Terminated) {
            Terminated terminated = ((Terminated) msg);
            log.error("[Gof3DiscoveryRouter] terminate event for {}", terminated.actor().path());
            router = router.removeRoutee(terminated.actor());
            ActorRef r = getContext().actorOf(Props.create(Gof3DiscoveryActor.class));
            getContext().watch(r);
            router = router.addRoutee(new ActorRefRoutee(r));
        }
        else {
            log.error("[Gof3DiscoveryRouter] unhandled event {}", Message.getDebug(msg));
            unhandled(msg);
        }

        log.debug("[Gof3DiscoveryRouter] onReceive done.");
    }

    private void routeTimerEvent() {
        Set<String> notSent = Sets.newHashSet(discovery.keySet());

        for (PeerURLType url : discoveryConfiguration.getDiscoveryURL()) {
            if (!url.getType().equalsIgnoreCase(NsiConstants.NSI_NSA_V1)) {
                continue;
            }

            log.debug("routeTimerEvent: url=" + url.getValue());

            Gof3DiscoveryMsg msg = discovery.get(url.getValue());
            if (msg == null) {
                msg = new Gof3DiscoveryMsg("Gof3DiscoveryRouter", this.self().path());
                msg.setNsaURL(url.getValue());
            }

            if (msg.getInteration() < refresh) {
                msg.setInteration(msg.getInteration() + 1);
            }
            else {
                msg.setNsaLastModifiedTime(0L);
                for (String top : msg.getTopologyURL()) {
                    msg.setTopologyLastModified(top, 0L);
                }

                msg.setInteration(0);
            }

            router.route(msg, getSelf());
            notSent.remove(msg.getNsaURL());
        }

        // Clean up the entries no longer in the configuration.
        for (String url : notSent) {
            log.debug("routeTimerEvent: entry no longer needed, url=" + url);
            discovery.remove(url);
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

    /**
     * @return the interval
     */
    public long getInterval() {
        return interval;
    }

    /**
     * @param interval the interval to set
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * @return the refresh
     */
    public long getRefresh() {
        return refresh;
    }

    /**
     * @param refresh the refresh to set
     */
    public void setRefresh(long refresh) {
        this.refresh = refresh;
    }
}
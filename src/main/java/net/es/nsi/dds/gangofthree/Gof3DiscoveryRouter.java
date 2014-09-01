/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.gangofthree;

import net.es.nsi.dds.actors.DdsActorSystem;
import net.es.nsi.dds.messages.TimerMsg;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.es.nsi.dds.dao.DiscoveryConfiguration;
import net.es.nsi.dds.api.jaxb.PeerURLType;
import net.es.nsi.dds.messages.StartMsg;
import net.es.nsi.dds.schema.NsiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class Gof3DiscoveryRouter extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsActorSystem ddsActorSystem;
    private DiscoveryConfiguration discoveryConfiguration;
    private int poolSize;
    private long interval;
    private Router router;
    private Map<String, Gof3DiscoveryMsg> discovery = new ConcurrentHashMap<>();

    public Gof3DiscoveryRouter(DdsActorSystem ddsActorSystem, DiscoveryConfiguration discoveryConfiguration) {
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
        TimerMsg message = new TimerMsg();

        // Check to see if we got the go ahead to start registering.
        if (msg instanceof StartMsg) {
            // Create a Register event to start us off.
            msg = message;
        }

        if (msg instanceof TimerMsg) {
            routeTimerEvent();
            ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);
        }
        else if (msg instanceof Gof3DiscoveryMsg) {
            Gof3DiscoveryMsg incoming = (Gof3DiscoveryMsg) msg;
            
            log.debug("onReceive: discovery update for nsaId=" + incoming.getNsaId());

            discovery.put(incoming.getNsaURL(), incoming);
        }
        else if (msg instanceof Terminated) {
            log.debug("onReceive: terminate event.");
            router = router.removeRoutee(((Terminated) msg).actor());
            ActorRef r = getContext().actorOf(Props.create(Gof3DiscoveryActor.class));
            getContext().watch(r);
            router = router.addRoutee(new ActorRefRoutee(r));
        }
        else {
            log.error("onReceive: unhandled event.");
            unhandled(msg);
        }
    }
    
    private void routeTimerEvent() {
        Set<PeerURLType> discoveryURL = discoveryConfiguration.getDiscoveryURL();
        Set<String> notSent = new HashSet<>(discovery.keySet());

        for (PeerURLType url : discoveryURL) {
            if (!url.getType().equalsIgnoreCase(NsiConstants.NSI_NSA_V1)) {
                continue;
            }
            
            log.debug("routeTimerEvent: url=" + url.getValue());
            
            Gof3DiscoveryMsg msg = discovery.get(url.getValue());
            if (msg == null) {
                msg = new Gof3DiscoveryMsg();
                msg.setNsaURL(url.getValue());
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
}
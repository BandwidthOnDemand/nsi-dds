package net.es.nsi.pce.discovery.agole;

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
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.discovery.actors.DdsActorSystem;
import net.es.nsi.pce.discovery.dao.DiscoveryConfiguration;
import net.es.nsi.dds.api.jaxb.PeerURLType;
import net.es.nsi.pce.discovery.messages.StartMsg;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.discovery.messages.TimerMsg;
import net.es.nsi.dds.management.jaxb.TopologyStatusType;
import net.es.nsi.pce.management.logs.PceErrors;
import net.es.nsi.pce.management.logs.PceLogger;
import net.es.nsi.pce.management.logs.PceLogs;
import net.es.nsi.pce.management.api.ProviderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class AgoleDiscoveryRouter extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private PceLogger topologyLogger = PceLogger.getLogger();
    private ProviderStatus manifestStatus = null;

    private DdsActorSystem ddsActorSystem;
    private long interval;
    private int poolSize;
    private Router router = null;
    private Map<String, AgoleDiscoveryMsg> discovery = new ConcurrentHashMap<>();

    private TopologyManifest manifest;

    private DiscoveryConfiguration discoveryConfiguration;
    private AgoleManifestReader manifestReader;

    private boolean isConfigured = false;

    public AgoleDiscoveryRouter(DdsActorSystem ddsActorSystem, DiscoveryConfiguration discoveryConfiguration, AgoleManifestReader manifestReader) {
        this.ddsActorSystem = ddsActorSystem;
        this.discoveryConfiguration = discoveryConfiguration;
        this.manifestReader = manifestReader;
    }

    @Override
    public void preStart() {
        Set<PeerURLType> discoveryURL = discoveryConfiguration.getDiscoveryURL();
        for (PeerURLType url : discoveryURL) {
            if (url.getType().equalsIgnoreCase(NsiConstants.NSI_TOPOLOGY_V1)) {
                manifestReader.setTarget(url.getValue());
                isConfigured = true;
                break;
            }
        }

        if (!isConfigured) {
            log.info("AgoleDiscoveryRouter: No AGOLE URL provisioned so disabling audit.");
            return;
        }

        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < getPoolSize(); i++) {
            ActorRef r = getContext().actorOf(Props.create(AgoleDiscoveryActor.class));
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
            if (!isConfigured) {
                log.info("onReceive: StartMsg no AGOLE URL provisioned so disabling audit.");
                return;
            }
        }

        if (msg instanceof TimerMsg) {
            log.debug("onReceive: timer event.");
            if (!isConfigured) {
                log.info("onReceive: TimerMsg no AGOLE URL provisioned so disabling audit.");
                return;
            }
            if (readManifest() != null) {
                routeTimerEvent();
            }

            ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);
        }
        else if (msg instanceof AgoleDiscoveryMsg) {
            AgoleDiscoveryMsg incoming = (AgoleDiscoveryMsg) msg;

            log.debug("onReceive: discovery update for nsaId=" + incoming.getNsaId());

            discovery.put(incoming.getTopologyURL(), incoming);
        }
        else if (msg instanceof Terminated) {
            log.debug("onReceive: terminate event.");
            if (router != null) {
                router = router.removeRoutee(((Terminated) msg).actor());
                ActorRef r = getContext().actorOf(Props.create(AgoleDiscoveryActor.class));
                getContext().watch(r);
                router = router.addRoutee(new ActorRefRoutee(r));
            }
        }
        else {
            log.debug("onReceive: unhandled event.");
            unhandled(msg);
        }
    }

    private TopologyManifest readManifest() {
        log.debug("readManifest: starting manifest audit for " + manifestReader.getTarget());
        manifestAuditStart();
        try {
            TopologyManifest manifestIfModified = manifestReader.getManifestIfModified();
            if (manifestIfModified != null) {
                manifest = manifestIfModified;
            }
        } catch (NotFoundException nf) {
            log.error("readManifest: could not find manifest file " + manifestReader.getTarget(), nf);
            manifestAuditError();
        } catch (JAXBException jaxb) {
            log.error("readManifest: could not parse manifest file " + manifestReader.getTarget(), jaxb);
            manifestAuditError();
        }

        manifestAuditSuccess();
        log.debug("readManifest: completed manifest audit for " + manifestReader.getTarget());
        return manifest;
    }

    private void routeTimerEvent() {
        log.debug("routeTimerEvent: entering.");
        Set<String> notSent = new HashSet<>(discovery.keySet());

        for (Map.Entry<String, String> entry : manifest.getEntryList().entrySet()) {
            String id = entry.getKey();
            String url =  entry.getValue();

            log.debug("routeTimerEvent: id=" + id + ", url=" + url);

            AgoleDiscoveryMsg msg = discovery.get(url);
            if (msg == null) {
                msg = new AgoleDiscoveryMsg();
                msg.setTopologyURL(url);
                msg.setId(id);
            }

            router.route(msg, getSelf());
            notSent.remove(url);
        }

        // Clean up the entries no longer in the configuration.
        for (String url : notSent) {
            log.debug("routeTimerEvent: entry no longer needed, url=" + url);
            discovery.remove(url);
        }

        log.debug("routeTimerEvent: exiting.");
    }

    private void manifestAuditStart() {
        topologyLogger.logAudit(PceLogs.AUDIT_MANIFEST_START, manifestReader.getId(), manifestReader.getTarget());

        if (manifestStatus == null) {
            manifestStatus = new ProviderStatus();

            if (manifestReader != null) {
                manifestStatus.setId(manifestReader.getId());
                manifestStatus.setHref(manifestReader.getTarget());
            }
        }
        else {
            manifestStatus.setStatus(TopologyStatusType.AUDITING);
            manifestStatus.setLastAudit(System.currentTimeMillis());
        }
    }

    private void manifestAuditError() {
        topologyLogger.errorAudit(PceErrors.AUDIT_MANIFEST, manifestReader.getId(), manifestReader.getTarget());
        manifestStatus.setStatus(TopologyStatusType.ERROR);
    }

    private void manifestAuditSuccess() {
        topologyLogger.logAudit(PceLogs.AUDIT_MANIFEST_SUCCESSFUL, manifestReader.getId(), manifestReader.getTarget());
        manifestStatus.setId(manifestReader.getId());
        manifestStatus.setHref(manifestReader.getTarget());
        manifestStatus.setStatus(TopologyStatusType.COMPLETED);
        manifestStatus.setLastSuccessfulAudit(manifestStatus.getLastAudit());
        manifestStatus.setLastDiscovered(manifestReader.getLastModified());
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
}
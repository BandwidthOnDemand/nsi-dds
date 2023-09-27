package net.es.nsi.dds.agole;

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
import jakarta.ws.rs.NotFoundException;
import jakarta.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.es.nsi.dds.actors.DdsActorSystem;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.jaxb.configuration.PeerURLType;
import net.es.nsi.dds.jaxb.management.TopologyStatusType;
import net.es.nsi.dds.management.api.ProviderStatus;
import net.es.nsi.dds.management.logs.DdsErrors;
import net.es.nsi.dds.management.logs.DdsLogger;
import net.es.nsi.dds.management.logs.DdsLogs;
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
public class AgoleDiscoveryRouter extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final DdsLogger topologyLogger = DdsLogger.getLogger();
    private ProviderStatus manifestStatus = null;

    private final DdsActorSystem ddsActorSystem;
    private long interval;
    private int poolSize;
    private Router router = null;
    private final Map<String, AgoleDiscoveryMsg> discovery = new ConcurrentHashMap<>();

    private TopologyManifest manifest;

    private final DdsConfiguration discoveryConfiguration;
    private final AgoleManifestReader manifestReader;

    private boolean isConfigured = false;

    public AgoleDiscoveryRouter(DdsActorSystem ddsActorSystem, DdsConfiguration discoveryConfiguration, AgoleManifestReader manifestReader) {
        this.ddsActorSystem = ddsActorSystem;
        this.discoveryConfiguration = discoveryConfiguration;
        this.manifestReader = manifestReader;
    }

    @Override
    public void preStart() {
        Optional<PeerURLType> peerURL = discoveryConfiguration.getDiscoveryURL()
                .stream()
                .filter((url) -> url.getType().equalsIgnoreCase(NsiConstants.NSI_TOPOLOGY_V1))
                .findFirst();

        isConfigured = peerURL.isPresent();
        if (isConfigured) {
            manifestReader.setTarget(peerURL.get().getValue());
        }
        else {
            log.info("[AgoleDiscoveryRouter] No AGOLE URL provisioned so disabling audit.");
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
        log.debug("[AgoleDiscoveryRouter] onReceive {}", Message.getDebug(msg));

        // Check to see if we got the go ahead to start registering.
        if (msg instanceof StartMsg) {
            // Create a Register event to start us off.
            if (!isConfigured) {
                log.info("[AgoleDiscoveryRouter] StartMsg no AGOLE URL provisioned so disabling audit.");
                return;
            }

            StartMsg sm = (StartMsg) msg;
            msg = new TimerMsg(sm.getInitiator(), sm.getPath());
        }

        if (msg instanceof TimerMsg) {
            log.debug("[AgoleDiscoveryRouter] timer event.");
            if (!isConfigured) {
                log.info("[AgoleDiscoveryRouter] TimerMsg no AGOLE URL provisioned so disabling audit.");
                return;
            }
            if (readManifest() != null) {
                routeTimerEvent();
            }

            TimerMsg message = new TimerMsg("AgoleDiscoveryRouter", this.self().path());
            ddsActorSystem.getActorSystem().scheduler()
                .scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.self(), message,
                    ddsActorSystem.getActorSystem().dispatcher(), null);
        }
        else if (msg instanceof AgoleDiscoveryMsg incoming) {
            log.debug("[AgoleDiscoveryRouter] discovery update for nsaId={}", incoming.getNsaId());
            discovery.put(incoming.getTopologyURL(), incoming);
        }
        else if (msg instanceof Terminated terminated) {
            log.error("[AgoleDiscoveryRouter] terminate event for {}", terminated.actor().path());
            if (router != null) {
                router = router.removeRoutee(((Terminated) msg).actor());
                ActorRef r = getContext().actorOf(Props.create(AgoleDiscoveryActor.class));
                getContext().watch(r);
                router = router.addRoutee(new ActorRefRoutee(r));
            }
        }
        else {
            log.error("[RegistrationRouter] unhandled event {}", Message.getDebug(msg));
            unhandled(msg);
        }

        log.debug("[AgoleDiscoveryRouter] onReceive done.");
    }

    private TopologyManifest readManifest() {
        log.debug("readManifest: starting manifest audit for {}", manifestReader.getTarget());
        manifestAuditStart();
        try {
            TopologyManifest manifestIfModified = manifestReader.getManifestIfModified();
            if (manifestIfModified != null) {
                manifest = manifestIfModified;
            }
        } catch (NotFoundException nf) {
            log.error("readManifest: could not find manifest file {}", manifestReader.getTarget(), nf);
            manifestAuditError();
        } catch (JAXBException jaxb) {
            log.error("readManifest: could not parse manifest file {}", manifestReader.getTarget(), jaxb);
            manifestAuditError();
        }

        manifestAuditSuccess();
        log.debug("readManifest: completed manifest audit for {}", manifestReader.getTarget());
        return manifest;
    }

    private void routeTimerEvent() {
        log.debug("routeTimerEvent: entering.");
        Set<String> notSent = new HashSet<>(discovery.keySet());

        manifest.getEntryList().entrySet().stream().forEach((entry) -> {
            String id = entry.getKey();
            String url =  entry.getValue();

            log.debug("routeTimerEvent: id={}, url={}", id, url);

            AgoleDiscoveryMsg msg = discovery.get(url);
            if (msg == null) {
                msg = new AgoleDiscoveryMsg();
                msg.setTopologyURL(url);
                msg.setId(id);
            }

            router.route(msg, getSelf());
            notSent.remove(url);
        });

        // Clean up the entries no longer in the configuration.
        notSent.forEach((url) -> {
            log.debug("routeTimerEvent: entry no longer needed, url={}", url);
            discovery.remove(url);
        });

        log.debug("routeTimerEvent: exiting.");
    }

    private void manifestAuditStart() {
        topologyLogger.logAudit(DdsLogs.AUDIT_MANIFEST_START, manifestReader.getId(), manifestReader.getTarget());

        if (manifestStatus == null) {
            manifestStatus = new ProviderStatus();
            manifestStatus.setId(manifestReader.getId());
            manifestStatus.setHref(manifestReader.getTarget());
        }
        else {
            manifestStatus.setStatus(TopologyStatusType.AUDITING);
            manifestStatus.setLastAudit(System.currentTimeMillis());
        }
    }

    private void manifestAuditError() {
        topologyLogger.errorAudit(DdsErrors.AUDIT_MANIFEST, manifestReader.getId(), manifestReader.getTarget());
        manifestStatus.setStatus(TopologyStatusType.ERROR);
    }

    private void manifestAuditSuccess() {
        topologyLogger.logAudit(DdsLogs.AUDIT_MANIFEST_SUCCESSFUL, manifestReader.getId(), manifestReader.getTarget());
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
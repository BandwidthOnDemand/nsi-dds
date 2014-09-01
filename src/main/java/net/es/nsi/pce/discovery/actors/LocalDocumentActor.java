package net.es.nsi.pce.discovery.actors;

import net.es.nsi.pce.discovery.messages.TimerMsg;
import akka.actor.UntypedActor;
import java.util.concurrent.TimeUnit;
import net.es.nsi.pce.discovery.dao.DiscoveryConfiguration;
import net.es.nsi.pce.discovery.provider.DdsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 * This actor fires periodically to inspect the document directory on permanent
 * storage for any new or updated documents.
 *
 * @author hacksaw
 */
public class LocalDocumentActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DdsActorSystem ddsActorSystem;
    private final DiscoveryConfiguration discoveryConfiguration;
    private long interval;

    public LocalDocumentActor(DdsActorSystem ddsActorSystem, DiscoveryConfiguration discoveryConfiguration) {
        this.ddsActorSystem = ddsActorSystem;
        this.discoveryConfiguration = discoveryConfiguration;
    }

    @Override
    public void preStart() {
        TimerMsg message = new TimerMsg();
        if (!discoveryConfiguration.isDocumentsConfigured()) {
            log.info("Disabling local document audit, local directory not configured.");
            return;
        }

        ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof TimerMsg) {
            TimerMsg message = (TimerMsg) msg;
            if (!discoveryConfiguration.isDocumentsConfigured()) {
                return;
            }

            DdsProvider.getInstance().loadDocuments();
            ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);

        } else {
            unhandled(msg);
        }
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
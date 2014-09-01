package net.es.nsi.pce.discovery.actors;

import net.es.nsi.pce.discovery.messages.TimerMsg;
import akka.actor.UntypedActor;
import java.util.concurrent.TimeUnit;
import net.es.nsi.pce.discovery.dao.DocumentCache;
import scala.concurrent.duration.Duration;

/**
 * This actor fires periodically to inspect the DDS document cache for any
 * expired documents.
 *
 * @author hacksaw
 */
public class DocumentExpiryActor extends UntypedActor {
    private DdsActorSystem ddsActorSystem;
    private DocumentCache documentCache;
    private long interval;

    public DocumentExpiryActor(DdsActorSystem ddsActorSystem, DocumentCache documentCache) {
        this.ddsActorSystem = ddsActorSystem;
        this.documentCache = documentCache;
    }

    @Override
    public void preStart() {
        TimerMsg message = new TimerMsg();
        ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof TimerMsg) {
            TimerMsg message = (TimerMsg) msg;
            documentCache.expire();
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
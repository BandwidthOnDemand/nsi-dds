package net.es.nsi.dds.actors;

import akka.actor.UntypedAbstractActor;
import java.util.concurrent.TimeUnit;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import net.es.nsi.dds.dao.DocumentCache;
import net.es.nsi.dds.messages.TimerMsg;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 * This actor fires periodically to inspect the DDS document cache for any expired documents.
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class DocumentExpiryActor extends UntypedAbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private final DdsActorSystem ddsActorSystem;
  private final DocumentCache documentCache;
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
    log.debug("[DocumentExpiryActor] onReceive {}", msg.getClass().getCanonicalName());

    if (msg instanceof TimerMsg) {
      log.info("[DocumentExpiryActor]: auditing expired documents.");

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

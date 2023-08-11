package net.es.nsi.dds.actors;

import akka.actor.UntypedAbstractActor;
import java.util.concurrent.TimeUnit;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import net.es.nsi.dds.dao.DocumentCache;
import net.es.nsi.dds.messages.Message;
import net.es.nsi.dds.messages.StartMsg;
import net.es.nsi.dds.messages.TimerMsg;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 * This actor fires periodically to inspect the DDS document cache for any expired documents.
 * All constructor parameters and properties configured via beans.xml file.
 *
 * @author hacksaw
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DocumentExpiryActor extends UntypedAbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private final DdsActorSystem ddsActorSystem;
  private final DocumentCache documentCache;
  private long interval;

  /**
   * Constructor for the DocumentExpiryActor.
   *
   * @param ddsActorSystem
   * @param documentCache
   */
  public DocumentExpiryActor(DdsActorSystem ddsActorSystem, DocumentCache documentCache) {
    this.ddsActorSystem = ddsActorSystem;
    this.documentCache = documentCache;
  }

  /**
   * Initialize the actor by scheduling a timer message.
   */
  @Override
  public void preStart() {
    TimerMsg message = new TimerMsg("DocumentExpiryActor", this.getSelf().path());
    ddsActorSystem.getActorSystem().scheduler()
        .scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message,
            ddsActorSystem.getActorSystem().dispatcher(), this.getSelf());
  }

  /**
   * Process an incoming message to the actor.  This is typically a timer
   * message triggering an audit for expired documents in the cache.
   *
   * @param msg A TimerMsg triggering the action to expire documents.
   */
  @Override
  public void onReceive(Object msg) {
    log.debug("[DocumentExpiryActor] onReceive {}", Message.getDebug(msg));

    // We can ignore the broadcast start message.
    if (msg instanceof StartMsg) {
      log.debug("[DocumentExpiryActor] ignoring unimplemented StartMsg.");
    } else if (msg instanceof TimerMsg) {
      log.info("[DocumentExpiryActor]: auditing expired documents.");

      // Expire the document cache.
      documentCache.expire();

      TimerMsg message = (TimerMsg) msg;
      message.setInitiator("DocumentExpiryActor");
      message.setPath(this.getSelf().path());
      ddsActorSystem.getActorSystem().scheduler()
          .scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message,
              ddsActorSystem.getActorSystem().dispatcher(), this.getSelf());

    } else {
      log.error("[DocumentExpiryActor] onReceive unhandled message {} {}", this.getSender(), Message.getDebug(msg));
      unhandled(msg);
    }

    log.debug("[DocumentExpiryActor] onReceive done.");
  }

  /**
   * Get the audit interval.
   *
   * @return
   */
  public long getInterval() {
    return interval;
  }

  /**
   * Sent the audit interval.
   *
   * @param interval the interval to set
   */
  public void setInterval(long interval) {
    this.interval = interval;
  }

}

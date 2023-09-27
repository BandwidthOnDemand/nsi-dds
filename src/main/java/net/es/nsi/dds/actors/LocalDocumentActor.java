package net.es.nsi.dds.actors;

import java.util.concurrent.TimeUnit;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.messages.Message;
import net.es.nsi.dds.messages.StartMsg;
import net.es.nsi.dds.messages.TimerMsg;
import net.es.nsi.dds.provider.DdsProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 * This actor fires periodically to inspect the document directory on permanent
 * storage for any new or updated documents.  These are not cached documents, but
 * documents the load nsi-dds instance will advertise.  All constructor parameters
 * and properties configured via beans.xml file.
 *
 * @author hacksaw
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LocalDocumentActor extends UntypedAbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private final DdsActorSystem ddsActorSystem;
  private final DdsConfiguration discoveryConfiguration;
  private long interval;

  /**
   * Constructor initialized by Spring.
   *
   * @param ddsActorSystem
   * @param discoveryConfiguration
   */
  public LocalDocumentActor(DdsActorSystem ddsActorSystem, DdsConfiguration discoveryConfiguration) {
    this.ddsActorSystem = ddsActorSystem;
    this.discoveryConfiguration = discoveryConfiguration;
  }

  /**
   * Initialize the actor by scheduling a timer message.
   */
  @Override
  public void preStart() {
    log.debug("[LocalDocumentActor] preStart().");
    if (!discoveryConfiguration.isDocumentsConfigured()) {
      log.info("[LocalDocumentActor] Disabling local document audit, local directory not configured.");
      return;
    }

    log.info("[LocalDocumentActor] Scheduling first audit for {} seconds.", getInterval());
    DdsProvider.getInstance().loadDocuments();

    TimerMsg message = new TimerMsg("LocalDocumentActor", this.getSelf().path());
    ddsActorSystem.getActorSystem().scheduler()
        .scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message,
            ddsActorSystem.getActorSystem().dispatcher(), null);
  }

  /**
   * Process an incoming message to the actor.  This is typically a timer
   * message triggering a load of documents from the local document repository.
   *
   * @param msg
   */
  @Override
  public void onReceive(Object msg) {
    log.debug("[LocalDocumentActor] onReceive {}", Message.getDebug(msg));

    // We can ignore the broadcast start message.
    if (msg instanceof StartMsg) {
      log.debug("[LocalDocumentActor] ignoring unimplemented StartMsg.");
    } else if (msg instanceof TimerMsg message) {
      log.debug("[LocalDocumentActor] processing timer message.");

      if (!discoveryConfiguration.isDocumentsConfigured()) {
        log.error("[LocalDocumentActor] audit was scheduled but no document directory provisioned.");
        return;
      }

      log.info("[LocalDocumentActor] loading local documents from {}.", discoveryConfiguration.getDocuments());
      DdsProvider.getInstance().loadDocuments();

      log.info("[LocalDocumentActor] Scheduling next audit for {} seconds.", getInterval());
      message.setInitiator("LocalDocumentActor");
      message.setPath(this.getSelf().path());
      ddsActorSystem.getActorSystem().scheduler()
          .scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message,
              ddsActorSystem.getActorSystem().dispatcher(), null);

    } else {
      log.error("[LocalDocumentActor] onReceive unhandled message {} {}", this.getSender(), Message.getDebug(msg));
      unhandled(msg);
    }

    log.debug("[LocalDocumentActor] onReceive done.");
  }

  /**
   * Get the audit interval.
   *
   * @return the interval
   */
  public long getInterval() {
    return interval;
  }

  /**
   * Set the audit interval.
   *
   * @param interval the interval to set
   */
  public void setInterval(long interval) {
    this.interval = interval;
  }
}

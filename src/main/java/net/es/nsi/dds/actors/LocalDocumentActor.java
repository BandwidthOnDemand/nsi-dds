package net.es.nsi.dds.actors;

import akka.actor.UntypedActor;
import java.util.concurrent.TimeUnit;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.messages.TimerMsg;
import net.es.nsi.dds.provider.DdsProvider;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import scala.concurrent.duration.Duration;

/**
 * This actor fires periodically to inspect the document directory on permanent storage for any new or updated
 * documents.
 *
 * @author hacksaw
 */
public class LocalDocumentActor extends UntypedActor {

  private final Logger log = LogManager.getLogger(getClass());
  private final DdsActorSystem ddsActorSystem;
  private final DdsConfiguration discoveryConfiguration;
  private long interval;

  public LocalDocumentActor(DdsActorSystem ddsActorSystem, DdsConfiguration discoveryConfiguration) {
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

    log.info("[LocalDocumentActor] Scheduling first audit for {} seconds.", getInterval());
    DdsProvider.getInstance().loadDocuments();
    ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);
  }

  @Override
  public void onReceive(Object msg) {
    if (msg instanceof TimerMsg) {
      TimerMsg message = (TimerMsg) msg;
      if (!discoveryConfiguration.isDocumentsConfigured()) {
        log.error("[LocalDocumentActor] audit was scheduled but no document directory provisioned.");
        return;
      }

      log.info("[LocalDocumentActor] loading local documents from {}.", discoveryConfiguration.getDocuments());
      DdsProvider.getInstance().loadDocuments();

      log.info("[LocalDocumentActor] Scheduling next audit for {} seconds.", getInterval());
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

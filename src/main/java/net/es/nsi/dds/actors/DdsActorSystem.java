package net.es.nsi.dds.actors;

import akka.actor.ActorSystem;
import static net.es.nsi.dds.spring.SpringExtension.SpringExtProvider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Creates the base DDS actor system.
 *
 * @author hacksaw
 */
@Slf4j
public class DdsActorSystem implements ApplicationContextAware {
  private ActorSystem actorSystem;
  private ApplicationContext applicationContext;

  public DdsActorSystem() {
    // Initialize the AKKA actor system for the PCE and subsystems.
    log.info("DdsActorSystem: Initializing actor framework...");
    actorSystem = akka.actor.ActorSystem.create("NSI-DISCOVERY");
    SpringExtProvider.get(actorSystem).initialize(applicationContext);
    log.info("DdsActorSystem: ... Actor framework initialized.");
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  /**
   * @return the actorSystem
   */
  public akka.actor.ActorSystem getActorSystem() {
    return actorSystem;
  }

  /**
   * @param actorSystem the actorSystem to set
   */
  public void setActorSystem(akka.actor.ActorSystem actorSystem) {
    this.actorSystem = actorSystem;
  }
}

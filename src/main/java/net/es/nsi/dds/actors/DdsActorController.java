package net.es.nsi.dds.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Terminated;
import jakarta.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.messages.StartMsg;
import net.es.nsi.dds.spring.SpringExtension;
import net.es.nsi.dds.spring.SpringExtension.SpringExt;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import scala.concurrent.Future;

/**
 * This is a controller class that initializes the actor system. All constructor
 * parameters and properties configured via beans.xml file.
 *
 * @author hacksaw
 */
@Slf4j
public class DdsActorController implements ApplicationContextAware {
    // Configuration reader.
    private final DdsActorSystem ddsActorSystem;
    private final DdsConfiguration configReader;
    private final List<ActorEntry> actorEntries;
    private ApplicationContext applicationContext;

    private final List<ActorRef> startList = new ArrayList<>();

    /**
     * Default constructor called by Spring to initialize the actor.
     *
     * @param ddsActorSystem
     * @param configReader
     * @param entries
     */
    public DdsActorController(DdsActorSystem ddsActorSystem, DdsConfiguration configReader, ActorEntry... entries) {
        this.ddsActorSystem = ddsActorSystem;
        this.configReader = configReader;
        this.actorEntries = Arrays.asList(entries);
    }

    /**
     * Set the Spring application context.
     *
     * @param applicationContext
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Initialize the DDS Actor system by initializing the AKKA context and create all actors.
     *
     * @throws IllegalArgumentException
     * @throws JAXBException
     * @throws FileNotFoundException
     */
    public void init() throws IllegalArgumentException, JAXBException, FileNotFoundException {
        final ActorSystem actorSystem = ddsActorSystem.getActorSystem();
        SpringExtension.SpringExtProvider.get(actorSystem).initialize(applicationContext);
        final SpringExt ext = SpringExtension.SpringExtProvider.get(actorSystem);

        // Initialize the injects actors.
        actorEntries.forEach((ActorEntry entry) -> {
            try {
                log.info("DdsActorController: Initializing {}", entry.getActor());
                startList.add(actorSystem.actorOf(ext.props(entry.getActor()), "discovery-" + entry.getActor()));
                log.info("DdsActorController: Initialized {}", entry.getActor());
            } catch (Exception ex) {
                log.error("DdsActorController: Failed to initialize {}", entry.getActor(), ex);
            }
        });
    }

    /**
     * Returns the AKKA actor system.
     *
     * @return
     */
    public ActorSystem getActorSystem() {
        return ddsActorSystem.getActorSystem();
    }

    /**
     * Return the DDS configuration reader.
     *
     * @return
     */
    public DdsConfiguration getConfigReader() {
        return configReader;
    }

    /**
     * Schedule the delivery of a notification message within the AKKA system.
     *
     * @param message
     * @param delay
     * @return
     * @throws BeansException
     */
    public Cancellable scheduleNotification(Object message, long delay) throws BeansException {
        NotificationRouter notificationRouter = (NotificationRouter) applicationContext.getBean("notificationRouter");
        return notificationRouter.scheduleNotification(message, delay);
    }

    /**
     * Send a notification message within the AKKA system.
     *
     * @param message The notification message to send.
     */
    public void sendNotification(Object message) {
        NotificationRouter notificationRouter = (NotificationRouter) applicationContext.getBean("notificationRouter");
        notificationRouter.sendNotification(message);
    }

    /**
     * Start the discovery process within AKKA by sending a StartMsg to all actors.
     */
    public void start() {
        log.info("[DdsActorController] Starting discovery process...");

        StartMsg msg = new StartMsg("DdsActorController");
        startList.forEach((ref) -> {
            ref.tell(msg, ActorRef.noSender());
        });

        log.debug("[DdsActorController] dead letters waiting {}",
            this.getActorSystem().mailboxes().deadLetterMailbox().numberOfMessages());
    }

    /**
     * Shutdown the DDS actor system.
     *
     * @throws InterruptedException If the sleeping thread is interrupted.
     */
    public void shutdown() throws InterruptedException {
        log.info("DdsActorController: Shutting down actor system...");
        Future<Terminated> terminate = ddsActorSystem.getActorSystem().terminate();
        while (!terminate.isCompleted()) {
            log.info("Terminating ...");
            Thread.sleep(1000);
        }
    }
}

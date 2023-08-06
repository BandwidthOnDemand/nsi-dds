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
 * This is a controller class that initializes the actor system.
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

    public DdsActorController(DdsActorSystem ddsActorSystem, DdsConfiguration configReader, ActorEntry... entries) {
        this.ddsActorSystem = ddsActorSystem;
        this.configReader = configReader;
        this.actorEntries = Arrays.asList(entries);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void init() throws IllegalArgumentException, JAXBException, FileNotFoundException {
        final ActorSystem actorSystem = ddsActorSystem.getActorSystem();
        SpringExtension.SpringExtProvider.get(actorSystem).initialize(applicationContext);
        final SpringExt ext = SpringExtension.SpringExtProvider.get(actorSystem);

        // Initialize the injectes actors.
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

    public ActorSystem getActorSystem() {
        return ddsActorSystem.getActorSystem();
    }

    public DdsConfiguration getConfigReader() {
        return configReader;
    }

    public Cancellable scheduleNotification(Object message, long delay) throws BeansException {
        NotificationRouter notificationRouter = (NotificationRouter) applicationContext.getBean("notificationRouter");
        Cancellable scheduleOnce = notificationRouter.scheduleNotification(message, delay);
        return scheduleOnce;
    }

    public void sendNotification(Object message) {
        NotificationRouter notificationRouter = (NotificationRouter) applicationContext.getBean("notificationRouter");
        notificationRouter.sendNotification(message);
    }

    public void start() {
        log.info("DdsActorController: Starting discovery process...");
        StartMsg msg = new StartMsg();
        startList.stream().forEach((ref) -> {
            ref.tell(msg, null);
        });
    }
    public void shutdown() throws InterruptedException {
        log.info("DdsActorController: Shutting down actor system...");
        Future<Terminated> terminate = ddsActorSystem.getActorSystem().terminate();
        while (!terminate.isCompleted()) {
            log.info("Terminating ...");
            Thread.sleep(1000);
        }
    }
}

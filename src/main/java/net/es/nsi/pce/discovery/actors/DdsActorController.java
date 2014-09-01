/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.discovery.dao.DiscoveryConfiguration;
import net.es.nsi.pce.discovery.messages.StartMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import scala.concurrent.duration.Duration;
import static net.es.nsi.pce.spring.SpringExtension.SpringExtProvider;
import org.springframework.context.ApplicationContextAware;

/**
 *
 * @author hacksaw
 */
public class DdsActorController implements ApplicationContextAware {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // Configuration reader.
    private DdsActorSystem ddsActorSystem;
    private DiscoveryConfiguration configReader;
    private List<ActorEntry> actorEntries;
    private ApplicationContext applicationContext;
    
    private List<ActorRef> startList = new ArrayList<>();
    
    public DdsActorController(DdsActorSystem ddsActorSystem, DiscoveryConfiguration configReader, ActorEntry... entries) {
        this.ddsActorSystem = ddsActorSystem;
        this.configReader = configReader;
        this.actorEntries = Arrays.asList(entries);
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public void init() throws IllegalArgumentException, JAXBException, FileNotFoundException {
        /*ClassLoader myClassLoader = ClassLoader.getSystemClassLoader();
        try {
            Class<?> myClass = myClassLoader.loadClass("net.es.nsi.pce.discovery.actors.ConfigurationActor");
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DdsActorController.class.getName()).log(Level.SEVERE, null, ex);
        }*/

        // Each actor must take the parameters (DdsActorController ddsActorSystem,
        // int poolSize, long interval) which are configured via Spring.
        int poolSize = configReader.getActorPool();
        long interval = configReader.getAuditInterval();
        
        ActorSystem actorSystem = ddsActorSystem.getActorSystem();
        SpringExtProvider.get(actorSystem).initialize(applicationContext);
        
        // Initialize the injectes actors.
        ActorRef actor;
        for (ActorEntry entry : actorEntries) {
            try {
                log.info("DdsActorController: Initializing " + entry.getActor());
                actor = actorSystem.actorOf(SpringExtProvider.get(actorSystem).props(entry.getActor()), "discovery-" + entry.getActor());
                startList.add(actor);
                log.info("DdsActorController: Initialized " + entry.getActor());
            } catch (Exception ex) {
                log.error("DdsActorController: Failed to initialize " + entry.getActor(), ex);
            }
        }
    }
    
    public ActorSystem getActorSystem() {
        return ddsActorSystem.getActorSystem();
    }
    
    public DiscoveryConfiguration getConfigReader() {
        return configReader;
    }
    
    public Cancellable scheduleNotification(Object message, long delay) {
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
        for (ActorRef ref : startList) {
            ref.tell(msg, null);
        }
    }
    public void shutdown() {
        log.info("DdsActorController: Shutting down actor system...");
        ddsActorSystem.getActorSystem().shutdown();
    }
}

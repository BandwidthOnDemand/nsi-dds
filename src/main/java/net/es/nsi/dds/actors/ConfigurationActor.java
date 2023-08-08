package net.es.nsi.dds.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.concurrent.TimeUnit;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.messages.Message;
import net.es.nsi.dds.messages.StartMsg;
import net.es.nsi.dds.messages.TimerMsg;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 * This actor is on a timer to periodically load the DDS configuration file.  All constructor
 * parameters and properties configured via beans.xml file.
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class ConfigurationActor extends UntypedAbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final DdsActorSystem ddsActorSystem;
    private final DdsConfiguration discoveryConfiguration;
    private long interval;

    /**
     * Default constructor called by Spring to initialize the actor.
     *
     * @param ddsActorSystem
     * @param discoveryConfiguration
     */
    public ConfigurationActor(DdsActorSystem ddsActorSystem, DdsConfiguration discoveryConfiguration) {
        this.ddsActorSystem = ddsActorSystem;
        this.discoveryConfiguration = discoveryConfiguration;
    }

    /**
     * Initialize the actor by scheduling a timer message.
     */
    @Override
    public void preStart() {
        TimerMsg message = new TimerMsg("ConfigurationActor", this.self().path());
        ddsActorSystem.getActorSystem().scheduler()
            .scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message,
                ddsActorSystem.getActorSystem().dispatcher(), null);
    }

    /**
     * Process an incoming message to the actor.  This is typically a timer
     * message triggering a reload the DDS configuration file.
     *
     * @param msg
     */
    @Override
    public void onReceive(Object msg) {
        log.debug("[ConfigurationActor] onReceive {}", Message.getDebug(msg));

        // We can ignore the broadcast start message.
        if (msg instanceof StartMsg) {
            log.debug("[ConfigurationActor] ignoring unimplemented StartMsg.");
        } else if (msg instanceof TimerMsg) {
            log.debug("[ConfigurationActor] onReceive TimerMsg.");

            try {
                discoveryConfiguration.load();
            }
            catch (Exception ex) {
                log.error("[ConfigurationActor] onReceive: Configuration load failed.", ex);
            }

            TimerMsg event = (TimerMsg) msg;
            event.setInitiator("ConfigurationActor");
            event.setPath(this.getSelf().path());

            ddsActorSystem.getActorSystem().scheduler()
                .scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS),
                    this.getSelf(), event, ddsActorSystem.getActorSystem().dispatcher(), null);

        } else {
            log.error("[ConfigurationActor] onReceive unhandled message {} {}", this.getSender(), Message.getDebug(msg));
            unhandled(msg);
        }

        log.debug("[ConfigurationActor] onReceive done.");
    }

    /**
     * Get the timer interval.
     *
     * @return the interval
     */
    public long getInterval() {
        return interval;
    }

    /**
     * Set the timer interval.
     *
     * @param interval the interval to set
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }

}

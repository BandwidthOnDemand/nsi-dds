/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import net.es.nsi.pce.discovery.messages.TimerMsg;
import akka.actor.UntypedActor;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.discovery.dao.DiscoveryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class ConfigurationActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsActorSystem ddsActorSystem;
    private DiscoveryConfiguration discoveryConfiguration;
    private long interval;

    public ConfigurationActor(DdsActorSystem ddsActorSystem, DiscoveryConfiguration discoveryConfiguration) {
        this.ddsActorSystem = ddsActorSystem;
        this.discoveryConfiguration = discoveryConfiguration;
    }
    @Override
    public void preStart() {
        TimerMsg message = new TimerMsg();
        ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), message, ddsActorSystem.getActorSystem().dispatcher(), null);
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof TimerMsg) {
            TimerMsg event = (TimerMsg) msg;

            try {
                discoveryConfiguration.load();
            }
            catch (IllegalArgumentException | JAXBException | IOException | NullPointerException ex) {
                log.error("onReceive: Configuration load failed.", ex);
            }

            ddsActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(getInterval(), TimeUnit.SECONDS), this.getSelf(), event, ddsActorSystem.getActorSystem().dispatcher(), null);        

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

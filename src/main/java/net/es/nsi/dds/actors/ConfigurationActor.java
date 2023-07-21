package net.es.nsi.dds.actors;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.messages.TimerMsg;
import scala.concurrent.duration.Duration;

/**
 * This actor is on a timer to periodically load the DDS configuration file.
 *
 * @author hacksaw
 */
public class ConfigurationActor extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final DdsActorSystem ddsActorSystem;
    private final DdsConfiguration discoveryConfiguration;
    private long interval;

    public ConfigurationActor(DdsActorSystem ddsActorSystem, DdsConfiguration discoveryConfiguration) {
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
            catch (IllegalArgumentException | JAXBException | IOException | KeyStoreException
                    | NoSuchAlgorithmException | CertificateException | KeyManagementException 
                    | NoSuchProviderException | UnrecoverableKeyException ex) {
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

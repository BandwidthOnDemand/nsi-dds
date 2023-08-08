/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.config.actors;

import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import net.es.nsi.dds.messages.Message;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.Future;

/**
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class Terminator extends UntypedAbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorRef ref;

    public Terminator(ActorRef ref) {
        this.ref = ref;
        getContext().watch(ref);
    }

    @Override
    public void onReceive(Object msg) throws InterruptedException {
        log.debug("[Terminator] onReceive {}", Message.getDebug(msg));

        if (msg instanceof Terminated) {
            log.info("{} has terminated, shutting down system", ref.path());
            Future<Terminated> terminate = getContext().system().terminate();
            while (!terminate.isCompleted()) {
                log.info("Terminating ...");
                Thread.sleep(1000);
            }
        } else {
            log.error("[Terminator] unhandled message {}", Message.getDebug(msg));
            unhandled(msg);
        }

        log.debug("[Terminator] onReceive done.");
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.messages;

import java.io.Serializable;

import akka.actor.ActorPath;
import lombok.ToString;
import net.es.nsi.dds.provider.Subscription;

/**
 *
 * @author hacksaw
 */
@ToString(callSuper=true)
public class SubscriptionEvent extends Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Event { New, Update, Delete };

    private Event event;
    private Subscription subscription;

    public SubscriptionEvent() {
        super();
    }

    public SubscriptionEvent(String initiator) {
        super(initiator);
    }

    public SubscriptionEvent(String initiator, ActorPath path) {
        super(initiator, path);
    }

    /**
     * @return the event
     */
    public Event getEvent() {
        return event;
    }

    /**
     * @param event the event to set
     */
    public void setEvent(Event event) {
        this.event = event;
    }

    /**
     * @return the subscription
     */
    public Subscription getSubscription() {
        return subscription;
    }

    /**
     * @param subscription the subscription to set
     */
    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }
}

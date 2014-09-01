/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.messages;

import java.io.Serializable;
import net.es.nsi.pce.discovery.provider.Subscription;

/**
 *
 * @author hacksaw
 */
public class SubscriptionEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Event { New, Update, Delete };

    private Event event;
    private Subscription subscription;

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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.messages;

import net.es.nsi.pce.discovery.messages.RemoteSubscription;
import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
public class RegistrationEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Event { Audit, Register, Update, Delete };

    private Event event;
    private RemoteSubscription subscription;
    
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
    public RemoteSubscription getSubscription() {
        return subscription;
    }

    /**
     * @param subscription the subscription to set
     */
    public void setSubscription(RemoteSubscription subscription) {
        this.subscription = subscription;
    }
}

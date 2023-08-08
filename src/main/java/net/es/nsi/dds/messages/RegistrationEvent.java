/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.messages;

import akka.actor.ActorPath;
import lombok.ToString;

import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
@ToString(callSuper=true)
public class RegistrationEvent extends Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Event { Audit, Register, Update, Delete };

    private Event event;
    private String url;

    public RegistrationEvent() {
        super();
    }

    public RegistrationEvent(String initiator) {
        super(initiator);
    }

    public RegistrationEvent(String initiator, ActorPath path) {
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
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }
}

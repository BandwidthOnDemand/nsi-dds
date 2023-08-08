/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.messages;

import java.io.Serializable;
import java.util.Collection;

import akka.actor.ActorPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.es.nsi.dds.jaxb.dds.DocumentEventType;
import net.es.nsi.dds.provider.Document;
import net.es.nsi.dds.provider.Subscription;

/**
 *
 * @author hacksaw
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper=true)
@Data
public class Notification extends Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private DocumentEventType event;
    private Subscription subscription;
    private Collection<Document> documents;

    public Notification() {
        super();
    }

    public Notification(String initiator) {
        super(initiator);
    }

    public Notification(String initiator, ActorPath path) {
        super(initiator, path);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.messages;

import java.io.Serializable;

import akka.actor.ActorPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.es.nsi.dds.jaxb.dds.DocumentEventType;
import net.es.nsi.dds.provider.Document;

/**
 *
 * @author hacksaw
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper=true)
@Data
public class DocumentEvent extends Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private DocumentEventType event;
    private Document document;

    public DocumentEvent() {
        super();
    }

    public DocumentEvent(String initiator) {
        super(initiator);
    }

    public DocumentEvent(String initiator, ActorPath path) {
        super(initiator, path);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.messages;

import akka.actor.ActorPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper=true)
@Data
public class StartMsg extends Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public StartMsg() {
        super();
    }

    public StartMsg(String initiator) {
        super(initiator);
    }

    public StartMsg(String initiator, ActorPath path) {
        super(initiator, path);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.agole;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.es.nsi.dds.messages.Message;

import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper=true)
@Data
public class AgoleDiscoveryMsg extends Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String topologyURL;
    private long topologyLastModifiedTime = 0;
    private String nsaId;
}

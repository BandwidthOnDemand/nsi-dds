/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.agole;

import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
public class AgoleDiscoveryMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String topologyURL;
    private long topologyLastModifiedTime = 0;
    private String nsaId;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the nsaId to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the topologyURL
     */
    public String getTopologyURL() {
        return topologyURL;
    }

    /**
     * @param topologyURL the topologyURL to set
     */
    public void setTopologyURL(String topologyURL) {
        this.topologyURL = topologyURL;
    }

    /**
     * @return the topologyLastModifiedTime
     */
    public long getTopologyLastModifiedTime() {
        return topologyLastModifiedTime;
    }

    /**
     * @param topologyLastModifiedTime the topologyLastModifiedTime to set
     */
    public void setTopologyLastModifiedTime(long topologyLastModifiedTime) {
        this.topologyLastModifiedTime = topologyLastModifiedTime;
    }

    /**
     * @return the nsaId
     */
    public String getNsaId() {
        return nsaId;
    }

    /**
     * @param nsaId the nsaId to set
     */
    public void setNsaId(String nsaId) {
        this.nsaId = nsaId;
    }
}

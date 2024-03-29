/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.gangofthree;

import akka.actor.ActorPath;
import lombok.ToString;
import net.es.nsi.dds.messages.Message;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 *
 * @author hacksaw
 */
@ToString(callSuper=true)
public class Gof3DiscoveryMsg extends Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private long interation = 0;
    private String nsaURL;
    private long nsaLastModifiedTime = 0;
    private String nsaId;
    private final Map<String, Long> topology = new ConcurrentHashMap<>(); // key = topologyURL, Long == topologyLastModifiedTime

    public Gof3DiscoveryMsg() {
        super();
    }

    public Gof3DiscoveryMsg(String initiator) {
        super(initiator);
    }

    public Gof3DiscoveryMsg(String initiator, ActorPath path) {
        super(initiator, path);
    }

    /**
     * @return the nsaURL
     */
    public String getNsaURL() {
        return nsaURL;
    }

    /**
     * @param nsaURL the nsaURL to set
     */
    public void setNsaURL(String nsaURL) {
        this.nsaURL = nsaURL;
    }

    public void addTopology(String topologyURL, Long topologyLastModifiedTime) {
        this.topology.put(topologyURL, topologyLastModifiedTime);
    }

    public void clearTopology() {
        this.topology.clear();
    }

    public Set<String> getTopologyURL() {
        return Collections.unmodifiableSet(new ConcurrentSkipListSet<>(this.topology.keySet()));
    }

    public Long removeTopologyURL(String url) {
        return this.topology.remove(url);
    }

    public Map<String, Long> getTopology() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(this.topology));
    }

    public Long getTopologyLastModified(String url) {
        return topology.get(url);
    }

    public Long setTopologyLastModified(String url, Long lastModifiedTime) {
        return topology.put(url, lastModifiedTime);
    }

    /**
     * @return the nsaLastModifiedTime
     */
    public long getNsaLastModifiedTime() {
        return nsaLastModifiedTime;
    }

    /**
     * @param nsaLastModifiedTime the nsaLastModifiedTime to set
     */
    public void setNsaLastModifiedTime(long nsaLastModifiedTime) {
        this.nsaLastModifiedTime = nsaLastModifiedTime;
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

    /**
     * @return the interation
     */
    public long getInteration() {
        return interation;
    }

    /**
     * @param interation the interation to set
     */
    public void setInteration(long interation) {
        this.interation = interation;
    }
}

package net.es.nsi.dds.management.api;

import net.es.nsi.dds.jaxb.management.TopologyStatusType;

/**
 *
 * @author hacksaw
 */
public class ProviderStatus {
    private String id;
    private String href;

    private TopologyStatusType status = TopologyStatusType.INITIALIZING;
    private long lastAudit = System.currentTimeMillis();
    private long lastAuditDuration = -1;
    private long lastSuccessfulAudit = 0;
    private long lastDiscovered = 0;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the href
     */
    public String getHref() {
        return href;
    }

    /**
     * @param href the href to set
     */
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * @return the status
     */
    public TopologyStatusType getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(TopologyStatusType status) {
        this.status = status;
    }

    /**
     * @return the lastAudit
     */
    public long getLastAudit() {
        return lastAudit;
    }

    /**
     * @param lastAudit the lastAudit to set
     */
    public void setLastAudit(long lastAudit) {
        this.lastAudit = lastAudit;
    }

    /**
     * @return the lastSuccessfulAudit
     */
    public long getLastSuccessfulAudit() {
        return lastSuccessfulAudit;
    }

    /**
     * @param lastSuccessfulAudit the lastSuccessfulAudit to set
     */
    public void setLastSuccessfulAudit(long lastSuccessfulAudit) {
        this.lastSuccessfulAudit = lastSuccessfulAudit;
    }

    /**
     * @return the lastDiscovered
     */
    public long getLastDiscovered() {
        return lastDiscovered;
    }

    /**
     * @param lastDiscovered the lastDiscovered to set
     */
    public void setLastDiscovered(long lastDiscovered) {
        this.lastDiscovered = lastDiscovered;
    }

    /**
     * @return the lastAuditDuration
     */
    public long getLastAuditDuration() {
        return lastAuditDuration;
    }

    /**
     * @param lastAuditDuration the lastAuditDuration to set
     */
    public void setLastAuditDuration(long lastAuditDuration) {
        this.lastAuditDuration = lastAuditDuration;
    }
}


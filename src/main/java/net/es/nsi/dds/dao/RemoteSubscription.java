/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.dao;

import java.io.Serializable;
import java.util.Date;
import net.es.nsi.dds.jaxb.dds.SubscriptionType;

/**
 *
 * @author hacksaw
 */
public class RemoteSubscription implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ddsURL;
    private SubscriptionType subscription;

    private final Date created = new Date(0);
    private final Date lastModified = new Date(0);
    private final Date lastAudit = new Date(0);
    private final Date lastSuccessfulAudit = new Date(0);

    /**
     * @return the ddsURL
     */
    public String getDdsURL() {
        return ddsURL;
    }

    /**
     * @param ddsURL the ddsURL to set
     */
    public void setDdsURL(String ddsURL) {
        this.ddsURL = ddsURL;
    }

    /**
     * @return the subscription
     */
    public SubscriptionType getSubscription() {
        return subscription;
    }

    /**
     * @param subscription the subscription to set
     */
    public void setSubscription(SubscriptionType subscription) {
        this.subscription = subscription;
    }

    public void setCreated(Date date) {
        created.setTime(date.getTime());
        lastModified.setTime(date.getTime());
        lastAudit.setTime(date.getTime());
        lastSuccessfulAudit.setTime(date.getTime());
    }

    public void setLastSuccessfulAudit(Date date) {
        lastAudit.setTime(date.getTime());
        lastSuccessfulAudit.setTime(date.getTime());
    }

    public void setLastAudit(Date date) {
        lastAudit.setTime(date.getTime());
    }

    /**
     * @return the lastModified
     */
    public Date getLastModified() {
        return new Date(lastModified.getTime());
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(Date lastModified) {
        this.lastModified.setTime(lastModified.getTime());
    }

    /**
     * @return the lastAudit
     */
    public Date getLastAudit() {
        return new Date(lastAudit.getTime());
    }

    public Date getLastSuccessfulAduit() {
        return new Date(lastSuccessfulAudit.getTime());
    }
}

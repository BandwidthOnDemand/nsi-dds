/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.discovery.messages;

import java.io.Serializable;
import java.util.Collection;
import net.es.nsi.dds.api.jaxb.DocumentEventType;
import net.es.nsi.dds.discovery.provider.Document;
import net.es.nsi.dds.discovery.provider.Subscription;

/**
 *
 * @author hacksaw
 */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;
  
    private DocumentEventType event;
    private Subscription subscription;
    private Collection<Document> documents;

    /**
     * @return the event
     */
    public DocumentEventType getEvent() {
        return event;
    }

    /**
     * @param event the event to set
     */
    public void setEvent(DocumentEventType event) {
        this.event = event;
    }

    /**
     * @return the subscription
     */
    public Subscription getSubscription() {
        return subscription;
    }

    /**
     * @param subscription the subscription to set
     */
    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    /**
     * @return the documents
     */
    public Collection<Document> getDocuments() {
        return documents;
    }

    /**
     * @param documents the documents to set
     */
    public void setDocuments(Collection<Document> documents) {
        this.documents = documents;
    }
}

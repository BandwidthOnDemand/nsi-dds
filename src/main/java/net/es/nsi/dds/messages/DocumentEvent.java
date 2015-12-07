/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.messages;

import java.io.Serializable;
import net.es.nsi.dds.jaxb.dds.DocumentEventType;
import net.es.nsi.dds.provider.Document;

/**
 *
 * @author hacksaw
 */
public class DocumentEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private DocumentEventType event;
    private Document document;

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
     * @return the document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * @param document the document to set
     */
    public void setDocument(Document document) {
        this.document = document;
    }

}

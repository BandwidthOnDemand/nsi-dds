package net.es.nsi.dds.provider;

import java.util.Collection;
import java.util.Date;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.FilterType;
import net.es.nsi.dds.jaxb.dds.NotificationType;
import net.es.nsi.dds.jaxb.dds.SubscriptionRequestType;
import net.es.nsi.dds.messages.DocumentEvent;

/**
 *
 * @author hacksaw
 */
public interface DiscoveryProvider {

    public void init() throws Exception;
    public void start();
    public void shutdown();

    public Subscription addSubscription(SubscriptionRequestType request, String encoding);
    public Subscription deleteSubscription(String id) throws WebApplicationException;
    public Subscription editSubscription(String id, SubscriptionRequestType request, String encoding) throws WebApplicationException;
    public Subscription getSubscription(String id, Date lastModified) throws WebApplicationException;
    public Collection<Subscription> getSubscriptions(String requesterId, Date lastModified) throws WebApplicationException;

    public Document addDocument(DocumentType document, Source context) throws WebApplicationException;
    public Document deleteDocument(String nsa, String type, String id) throws WebApplicationException;
    public Document updateDocument(String nsa, String type, String id, DocumentType document, Source context) throws WebApplicationException, InvalidVersionException;
    public Document updateDocument(DocumentType request, Source context) throws WebApplicationException, InvalidVersionException;
    public Collection<Document> getDocuments(String nsa, String type, String id, Date lastDiscovered);
    public Collection<Document> getDocumentsByNsa(String nsa, String type, String id, Date lastDiscovered) throws WebApplicationException;
    public Collection<Document> getDocumentsByNsaAndType(String nsa, String type, String id, Date lastDiscovered) throws WebApplicationException;
    public Document getDocument(String nsa, String type, String id, Date lastDiscovered) throws WebApplicationException;
    public Document getDocument(DocumentType document) throws WebApplicationException;
    public Collection<Document> getLocalDocuments(String type, String id, Date lastDiscovered) throws WebApplicationException;
    public Collection<Document> getLocalDocumentsByType(String type, String id, Date lastDiscovered) throws WebApplicationException;
    public Document getLocalDocument(String type, String id, Date lastDiscovered) throws WebApplicationException;

    public void processNotification(NotificationType notification);

    public Collection<Document> getDocuments(FilterType filter);
    public Collection<Subscription> getSubscriptions(DocumentEvent event);

    public void loadDocuments();
}

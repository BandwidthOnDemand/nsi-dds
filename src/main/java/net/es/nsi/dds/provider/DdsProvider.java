/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.provider;

import akka.actor.Cancellable;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.actors.DdsActorController;
import net.es.nsi.dds.api.DiscoveryError;
import net.es.nsi.dds.api.Exceptions;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.dao.DocumentCache;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.DocumentEventType;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.FilterType;
import net.es.nsi.dds.jaxb.dds.NotificationType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.jaxb.dds.SubscriptionRequestType;
import net.es.nsi.dds.jaxb.dds.SubscriptionType;
import net.es.nsi.dds.messages.DocumentEvent;
import net.es.nsi.dds.messages.SubscriptionEvent;
import net.es.nsi.dds.util.XmlUtilities;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class DdsProvider implements DiscoveryProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();

    // Configuration reader.
    private DdsConfiguration configReader;

    // In-memory document cache.
    private final DocumentCache documentCache;

    // Local document repository for persistent document storage.
    private final DocumentCache documentRepository;

    // The actor system used to send notifications.
    private final DdsActorController ddsActorController;

    // In-memory subscription cache indexed by subscriptionId.  These are
    // subscriptions from remote DDS servers.
    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    public DdsProvider(DdsConfiguration configuration, DocumentCache documentCache, DocumentCache documentRepository, DdsActorController ddsActorController) {
        this.configReader = configuration;
        this.documentCache = documentCache;
        this.documentRepository = documentRepository;
        this.ddsActorController = ddsActorController;
    }

    public static DiscoveryProvider getInstance() {
        DdsProvider ddsProvider = SpringApplicationContext.getBean("discoveryProvider", DdsProvider.class);
        return ddsProvider;
    }

    @Override
    public void init() {
        log.debug("Starting DDS Service with cached documents:");
        for (Document document : documentCache.values()) {
            try {
                log.debug("id=" + URLDecoder.decode(document.getId(), "UTF-8"));
            } catch (Exception ex) {
                log.error("invalid id=" + document.getId());
            }
        }

        log.debug("Starting DDS Service with repository documents:");
        for (Document document : documentRepository.values()) {
            try {
                log.debug("id=" + URLDecoder.decode(document.getId(), "UTF-8"));
            } catch (Exception ex) {
                log.error("invalid id=" + document.getId());
            }
        }

        // Copy documents from permanet repository to cache before we start
        // processing requests.
        documentCache.putAll(documentRepository);

    }

    @Override
    public void start() {
        ddsActorController.start();
    }

    @Override
    public Subscription addSubscription(SubscriptionRequestType request, String encoding) {
        log.debug("DdsProvider.addSubscription: requesterId=" + request.getRequesterId());

        // Populate a subscription object.
        Subscription subscription = new Subscription(request, encoding, configReader.getBaseURL());

        // Save the subscription.
        subscriptions.put(subscription.getId(), subscription);

        // Now we need to schedule the send of the initial set of matching
        // documents in a notification to this subscription.  We delay the
        // send so that the requester has time to return and store the
        // subscription identifier.
        SubscriptionEvent se = new SubscriptionEvent();
        se.setEvent(SubscriptionEvent.Event.New);
        se.setSubscription(subscription);
        Cancellable scheduleOnce = ddsActorController.scheduleNotification(se, 5);
        subscription.setAction(scheduleOnce);

        log.debug("DdsProvider.addSubscription: schedule notification delivery for " + subscription.getId());

        return subscription;
    }

    @Override
    public Subscription deleteSubscription(String id) throws WebApplicationException {
        if (id == null || id.isEmpty()) {
            throw Exceptions.missingParameterException("subscription", "id");
        }

        Subscription subscription = subscriptions.remove(id);
        if (subscription == null) {
             throw Exceptions.doesNotExistException(DiscoveryError.SUBCRIPTION_DOES_NOT_EXIST, "id", id);
        }

        if (subscription.getAction() != null) {
            subscription.getAction().cancel();
            subscription.setAction(null);
        }

        return subscription;
    }

    @Override
    public Subscription editSubscription(String id, SubscriptionRequestType request, String encoding) throws WebApplicationException {
        // Make sure we have all needed parameters.
        if (id == null || id.isEmpty()) {
            throw Exceptions.missingParameterException("subscription", "id");
        }

        Subscription subscription = subscriptions.get(id);
        if (subscription == null) {
            throw Exceptions.doesNotExistException(DiscoveryError.SUBCRIPTION_DOES_NOT_EXIST, "id", id);
        }

        // Get the current time and remove milliseconds.
        Date currentTime = new Date();
        long fixed = currentTime.getTime() / 1000;
        currentTime.setTime(fixed * 1000);

        log.debug("editSubscription: changing lastModified time from " + subscription.getLastModified() + " to "+ currentTime);

        subscription.setEncoding(encoding);
        subscription.setLastModified(currentTime);
        SubscriptionType sub = subscription.getSubscription();
        sub.setRequesterId(request.getRequesterId());
        sub.setFilter(request.getFilter());
        sub.setCallback(request.getCallback());
        sub.getAny().addAll(request.getAny());
        sub.getOtherAttributes().putAll(request.getOtherAttributes());

        SubscriptionEvent se = new SubscriptionEvent();
        se.setEvent(SubscriptionEvent.Event.Update);
        se.setSubscription(subscription);
        ddsActorController.sendNotification(se);

        return subscription;
    }

    @Override
    public Subscription getSubscription(String id, Date lastModified) throws WebApplicationException {
        if (id == null || id.isEmpty()) {
            throw Exceptions.missingParameterException("subscription", "id");
        }

        Subscription subscription = subscriptions.get(id);
        if (subscription == null) {
            throw Exceptions.doesNotExistException(DiscoveryError.SUBCRIPTION_DOES_NOT_EXIST, "id", id);
        }

        // Check to see if the document was modified after provided date.
        if (lastModified != null &&
                lastModified.compareTo(subscription.getLastModified()) >= 0) {
            // NULL will represent not modified.
            return null;
        }

        return subscription;
    }

    @Override
    public Collection<Subscription> getSubscriptions(String requesterId, Date lastModified) {

        Collection<Subscription> subs = new ArrayList<>();
        if (requesterId != null && !requesterId.isEmpty()) {
            subs = getSubscriptionByRequesterId(requesterId, subscriptions.values());
        }
        else {
            subs.addAll(subscriptions.values());
        }

        if (lastModified != null) {
            subs = getSubscriptionsByDate(lastModified, subs);
        }

        return subs;
    }

    public Collection<Subscription> getSubscriptionByRequesterId(String requesterId, Collection<Subscription> input) {
        Collection<Subscription> output = new ArrayList<>();
        for (Subscription subscription : input) {
            if (subscription.getSubscription().getRequesterId().equalsIgnoreCase(requesterId)) {
                output.add(subscription);
            }
        }

        return output;
    }

    public Collection<Subscription> getSubscriptionsByDate(Date lastModified, Collection<Subscription> input) {
        Collection<Subscription> output = new ArrayList<>();
        for (Subscription subscription : input) {
            if (subscription.getLastModified().after(lastModified)) {
                output.add(subscription);
            }
        }

        return output;
    }

    @Override
    public void processNotification(NotificationType notification) throws WebApplicationException {
        log.debug("processNotification: event=" + notification.getEvent() + ", discovered=" + notification.getDiscovered());

        // TODO: We discard the event type and discovered time, however, the
        // discovered time could be used for an audit.  Perhaps save it?

        // Determine if we have already seen this document event.
        DocumentType document = notification.getDocument();
        if (document == null) {
            log.debug("processNotification: Document null.");
            return;
        }

        String documentId = Document.documentId(document);

        Document entry = documentCache.get(documentId);
        if (entry == null) {
            // This must be the first time we have seen the document so add it
            // into our cache.
            log.debug("processNotification: new documentId=" + documentId);
            addDocument(document, Source.REMOTE);
        }
        else {
            // We have seen the document before.
            log.debug("processNotification: update documentId=" + documentId);
            try {
                updateDocument(document, Source.REMOTE);
            }
            catch (InvalidVersionException ex) {
                // This is an old document version so discard.
                log.debug("processNotification: old document version documentId=" + documentId);
            }
        }
    }

    @Override
    public Document addDocument(DocumentType request, Source context) throws WebApplicationException {
        log.debug("addDocument: " + request.getId());

        // Create and populate our internal document.
        Document document = new Document(request, configReader.getBaseURL());

        // See if we already have a document under this id.
        Document get = documentCache.get(document.getId());
        if (get != null) {
            throw Exceptions.resourceExistsException(DiscoveryError.DOCUMENT_EXISTS, "document", document.getId());
        }

        // Validate basic fields.
        if (request.getNsa() == null || request.getNsa().isEmpty()) {
            throw Exceptions.missingParameterException(document.getId(), "nsa");
        }

        if (request.getType() == null || request.getType().isEmpty()) {
            throw Exceptions.missingParameterException(document.getId(), "type");
        }

        if (request.getId() == null || request.getId().isEmpty()) {
            throw Exceptions.missingParameterException(document.getId(), "id");
        }

        if (request.getVersion() == null || !request.getVersion().isValid()) {
            throw Exceptions.missingParameterException(document.getId(), "version");
        }

        if (request.getExpires() == null || !request.getExpires().isValid()) {
            throw Exceptions.missingParameterException(document.getId(), "expires");
        }

        // This is a new document so add it into the document space.
        try {
            documentCache.put(document.getId(), document);

            if (context == Source.LOCAL) {
                // Make a new copy of the document meta data to hold the
                // repository file name.
                Document repo = new Document(request, configReader.getBaseURL());
                documentRepository.put(repo.getId(), repo);
            }
        }
        catch (JAXBException | IOException ex) {
            log.error("addDocument: failed to add document to document store", ex);
            throw Exceptions.illegalArgumentException(DiscoveryError.DOCUMENT_INVALID, "document", document.getId());
        }

        // Route a new document event.
        DocumentEvent de = new DocumentEvent();
        de.setEvent(DocumentEventType.NEW);
        de.setDocument(document);
        ddsActorController.sendNotification(de);
        return document;
    }

    /**
     * Delete a document from the DDS document space.  The document is deleted
     * from the local repository if stored locally, and a document update is
     * propagated with and expiry time of now.
     *
     * @param nsa
     * @param type
     * @param id
     * @return
     * @throws WebApplicationException
     */
    @Override
    public Document deleteDocument(String nsa, String type, String id) throws WebApplicationException {
        // See if we already have a document under this id.
        String documentId = Document.documentId(nsa, type, id);

        log.debug("deleteDocument: documentId=" + documentId);

        Document document = documentCache.get(documentId);
        if (document == null) {
            throw Exceptions.doesNotExistException(DiscoveryError.DOCUMENT_DOES_NOT_EXIST, "document", documentId);
        }

        // See if this document is store in the local repository and remove it.
        documentRepository.remove(documentId);

        // Create a new document for the update.
        DocumentType expiredDoc = factory.createDocumentType();
        try {
            XMLGregorianCalendar currentDate = XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis());
            expiredDoc.setVersion(currentDate);
            expiredDoc.setExpires(currentDate);
        }
        catch (DatatypeConfigurationException ex) {
            throw Exceptions.internalServerErrorException("XMLGregorianCalendar", ex.getMessage());
        }
        expiredDoc.setContent(document.getDocument().getContent());
        expiredDoc.setHref(document.getDocument().getHref());
        expiredDoc.setId(document.getDocument().getId());
        expiredDoc.setNsa(document.getDocument().getNsa());
        expiredDoc.setSignature(document.getDocument().getSignature());
        expiredDoc.setType(document.getDocument().getType());

        Document newDoc = new Document(expiredDoc, configReader.getBaseURL());

        try {
            documentCache.update(documentId, newDoc);
        }
        catch (JAXBException jaxb) {
            log.error("deleteDocument: Failed to generate document XML, documentId=" + documentId, jaxb);
        }
        catch (IOException io) {
            log.error("deleteDocument: Failed to write document to cache, documentId=" + documentId, io);
        }

        // Route a update document event.
        DocumentEvent de = new DocumentEvent();
        de.setEvent(DocumentEventType.UPDATED);
        de.setDocument(newDoc);
        ddsActorController.sendNotification(de);

        return newDoc;
    }

    @Override
    public Document updateDocument(String nsa, String type, String id, DocumentType request, Source context) throws WebApplicationException, InvalidVersionException {
        // Create a document identifier to look up in our documet table.
        String documentId = Document.documentId(nsa, type, id);

        // See if we have a document under this id.
        Document document = documentCache.get(documentId);
        if (document == null) {
            String error = DiscoveryError.getErrorString(DiscoveryError.DOCUMENT_DOES_NOT_EXIST, "document", documentId);
            throw new NotFoundException(error);
        }

        // Validate basic fields.
        if (request.getNsa() == null || request.getNsa().isEmpty() || !request.getNsa().equalsIgnoreCase(document.getDocument().getNsa())) {
            throw Exceptions.missingParameterException(documentId, "nsa");
        }

        if (request.getType() == null || request.getType().isEmpty() || !request.getType().equalsIgnoreCase(document.getDocument().getType())) {
            throw Exceptions.missingParameterException(documentId, "type");
        }

        if (request.getId() == null || request.getId().isEmpty() || !request.getId().equalsIgnoreCase(document.getDocument().getId())) {
            throw Exceptions.missingParameterException(documentId, "id");
        }

        if (request.getVersion() == null || !request.getVersion().isValid()) {
            throw Exceptions.missingParameterException(documentId, "version");
        }

        if (request.getExpires() == null || !request.getExpires().isValid()) {
            throw Exceptions.missingParameterException(documentId, "expires");
        }

        // Make sure this is a new version of the document.
        if (request.getVersion().compare(document.getDocument().getVersion()) == DatatypeConstants.EQUAL) {
            log.debug("updateDocument: received document is a duplicate id=" + documentId);
            throw Exceptions.invalidVersionException(DiscoveryError.DOCUMENT_VERSION, request.getId(), request.getVersion(), document.getDocument().getVersion());
        }
        else if (request.getVersion().compare(document.getDocument().getVersion()) == DatatypeConstants.LESSER) {
            log.debug("updateDocument: received document is an old version id=" + documentId);
            throw Exceptions.invalidVersionException(DiscoveryError.DOCUMENT_VERSION, request.getId(), request.getVersion(), document.getDocument().getVersion());
        }

        Document newDoc = new Document(request, configReader.getBaseURL());
        try {
            documentCache.update(documentId, newDoc);

            if (context == Source.LOCAL) {
                Document repo = new Document(request, configReader.getBaseURL());
                documentRepository.update(repo.getId(), repo);
            }
        }
        catch (JAXBException jaxb) {
            log.error("updateDocument: Failed to generate document XML, documentId=" + documentId, jaxb);
        }
        catch (IOException io) {
            log.error("updateDocument: Failed to write document to cache, documentId=" + documentId, io);
        }

        log.debug("updateDocument: updated documentId=" + documentId);

        // Route a update document event.
        DocumentEvent de = new DocumentEvent();
        de.setEvent(DocumentEventType.UPDATED);
        de.setDocument(newDoc);
        ddsActorController.sendNotification(de);

        return newDoc;
    }

    @Override
    public Document updateDocument(DocumentType request, Source context) throws IllegalArgumentException, NotFoundException, InvalidVersionException {
        return updateDocument(request.getNsa(), request.getType(), request.getId(), request, context);
    }

    @Override
    public Collection<Document> getDocuments(String nsa, String type, String id, Date lastDiscovered) {
        // We need to search for matching documents using the supplied criteria.
        // We will do this linearly now, but we will need multiple indicies later
        // to make this faster (perhaps a database).

        // Seed the results.
        Collection<Document> results = documentCache.values();

        // This may be the most often used so filter by this first.
        if (lastDiscovered != null) {
            results = getDocumentsByDate(lastDiscovered, results);
        }

        if (nsa != null && !nsa.isEmpty()) {
            results = getDocumentsByNsa(nsa, results);
        }

        if (type != null && !type.isEmpty()) {
            results = getDocumentsByType(type, results);
        }

        if (id != null && !id.isEmpty()) {
            results = getDocumentsById(id, results);
        }

        return results;
    }

    @Override
    public Collection<Document> getDocumentsByNsa(String nsa, String type, String id, Date lastDiscovered) throws WebApplicationException {
        // Seed the results.
        Collection<Document> results = documentCache.values();

        // This is the primary search value.  Make sure it is present.
        if (nsa != null && !nsa.isEmpty()) {
            results = getDocumentsByNsa(nsa, results);
            if (results == null || results.isEmpty()) {
                throw Exceptions.doesNotExistException(DiscoveryError.NOT_FOUND, "nsa", nsa);
            }
        }
        else {
            throw Exceptions.illegalArgumentException(DiscoveryError.MISSING_PARAMETER, "document", "nsa");
        }

        // The rest are additional filters.
        if (lastDiscovered != null) {
            results = getDocumentsByDate(lastDiscovered, results);
        }

        if (type != null && !type.isEmpty()) {
            results = getDocumentsByType(type, results);
        }

        if (id != null && !id.isEmpty()) {
            results = getDocumentsById(id, results);
        }

        return results;
    }

    @Override
    public Collection<Document> getDocumentsByNsaAndType(String nsa, String type, String id, Date lastDiscovered) throws WebApplicationException {
        // Seed the results.
        Collection<Document> results = documentCache.values();

        // This is the primary search value.  Make sure it is present.
        if (nsa == null || nsa.isEmpty()) {
            throw Exceptions.illegalArgumentException(DiscoveryError.MISSING_PARAMETER, "document", "nsa");
        }

        if (type == null || type.isEmpty()) {
            throw Exceptions.illegalArgumentException(DiscoveryError.MISSING_PARAMETER, "document", "type");
        }

        results = getDocumentsByNsa(nsa, results);
        results = getDocumentsByType(type, results);

        if (id != null && !id.isEmpty()) {
            results = getDocumentsById(id, results);
        }

        // The rest are additional filters.
        if (lastDiscovered != null) {
            results = getDocumentsByDate(lastDiscovered, results);
        }

        return results;
    }

    @Override
    public Document getDocument(String nsa, String type, String id, Date lastDiscovered) throws WebApplicationException {
        String documentId = Document.documentId(nsa, type, id);
        Document document = documentCache.get(documentId);

        if (document == null) {
            throw Exceptions.doesNotExistException(DiscoveryError.DOCUMENT_DOES_NOT_EXIST, "document", "nsa=" + nsa + ", type=" + type + ", id=" + id);
        }

        // Check to see if the document was modified after provided date.
        if (lastDiscovered != null && lastDiscovered.compareTo(document.getLastDiscovered()) >= 0) {
            // NULL will represent not modified.
            return null;
        }

        return document;
    }

    @Override
    public Document getDocument(DocumentType document) throws IllegalArgumentException, NotFoundException {
        String documentId = Document.documentId(document);
        return documentCache.get(documentId);
    }

    @Override
    public Collection<Document> getLocalDocuments(String type, String id, Date lastDiscovered) throws IllegalArgumentException {
        String nsaId = getConfigReader().getNsaId();

        // Seed the results.
        Collection<Document> results = documentCache.values();

        // This is the primary search value.  Make sure it is present.
        if (nsaId != null && !nsaId.isEmpty()) {
            results = getDocumentsByNsa(nsaId, results);
        }
        else {
            throw Exceptions.illegalArgumentException(DiscoveryError.MISSING_PARAMETER, "document", "nsa");
        }

        // The rest are additional filters.
        if (lastDiscovered != null) {
            results = getDocumentsByDate(lastDiscovered, results);
        }

        if (type != null && !type.isEmpty()) {
            results = getDocumentsByType(type, results);
        }

        if (id != null && !id.isEmpty()) {
            results = getDocumentsById(id, results);
        }

        return results;
    }

    @Override
    public Collection<Document> getLocalDocumentsByType(String type, String id, Date lastDiscovered) throws IllegalArgumentException {
        return getDocumentsByNsaAndType(getConfigReader().getNsaId(), type, id, lastDiscovered);
    }

    @Override
    public Document getLocalDocument(String type, String id, Date lastDiscovered) throws IllegalArgumentException, NotFoundException {
        return getDocument(getConfigReader().getNsaId(), type, id, lastDiscovered);
    }

    public Collection<Document> getDocumentsByDate(Date lastDiscovered, Collection<Document> input) {
        Collection<Document> output = new ArrayList<>();
        input.stream().filter((document) -> (document.getLastDiscovered().after(lastDiscovered)))
                .forEach((document) -> {
            output.add(document);
        });

        return output;
    }

    public Collection<Document> getDocumentsByNsa(String nsa, Collection<Document> input) {
        Collection<Document> output = new ArrayList<>();
        input.stream().filter((document) -> (document.getDocument().getNsa().equalsIgnoreCase(nsa)))
                .forEach((document) -> {
            output.add(document);
        });

        return output;
    }

    public Collection<Document> getDocumentsByType(String type, Collection<Document> input) {
        Collection<Document> output = new ArrayList<>();
        input.stream().filter((document) -> (document.getDocument().getType().equalsIgnoreCase(type)))
                .forEach((document) -> {
            output.add(document);
        });

        return output;
    }

    public Collection<Document> getDocumentsById(String id, Collection<Document> input) {
        Collection<Document> output = new ArrayList<>();
        input.stream().filter((document) -> (document.getDocument().getId().equalsIgnoreCase(id)))
                .forEach((document) -> {
            output.add(document);
        });

        return output;
    }

    @Override
    public Collection<Document> getDocuments(FilterType filter) {
        // TODO: Match everything for demo.  Need to fix later.
        return documentCache.values();
    }

    @Override
    public Collection<Subscription> getSubscriptions(DocumentEvent event) {
        // TODO: Match everything for demo.  Need to fix later.
        return subscriptions.values();
    }

    @Override
    public void shutdown() {
        ddsActorController.shutdown();
    }

    /**
     * @return the configReader
     */
    public DdsConfiguration getConfigReader() {
        return configReader;
    }

    /**
     * @param configReader the configReader to set
     */
    public void setConfigReader(DdsConfiguration configReader) {
        this.configReader = configReader;
    }

    /**
     * Audit the contents of the local document directory for an new additions.
     * This directory contains documents administrators want to locally add
     * through the file system (as an alternative to the REST API ADD).
     */
    @Override
    public void loadDocuments() {
        Collection<String> xmlFilenames = XmlUtilities.getXmlFilenames(configReader.getDocuments());
        for (String filename : xmlFilenames) {
            DocumentType document;
            try {
                document = DdsParser.getInstance().parseFile(DocumentType.class, filename);
                if (document == null) {
                    log.error("loadDocuments: Loaded empty document from " + filename);
                    continue;
                }
            }
            catch (JAXBException | IOException ex) {
                log.error("loadDocuments: Failed to load file " + filename, ex);
                continue;
            }

            // We need to determine if this document is still valid
            // before proceding.
            XMLGregorianCalendar expires = document.getExpires();
            if (expires != null) {
                Date expiresTime = expires.toGregorianCalendar().getTime();

                // We take the current time and add the expiry buffer.
                Date now = new Date();
                now.setTime(now.getTime() + this.getConfigReader().getExpiryInterval() * 1000);
                if (expiresTime.before(now)) {
                    // This document has expired.  Remove from directory but add
                    // to document space just in case this is a delete of an
                    // existing document.
                    log.error("loadDocuments: Loaded document has expired " + filename + ", expires=" + expires.toGregorianCalendar().getTime().toString());

                    // Remove from documents directory.
                    try {
                        Path file = Paths.get(filename);
                        Files.deleteIfExists(file);
                        log.info("loadDocuments: Local document deleted " + filename);
                    } catch (Exception ex) {
                        log.error("loadDocuments: Local document delete failed " + filename, ex);
                    }
                }
            }

            // Have we seen this document before?
            Document existingDocument = this.getDocument(document);
            if (existingDocument == null) {
                // We have not seen this document so add it.
                try {
                    addDocument(document, Source.LOCAL);
                }
                catch (WebApplicationException ex) {
                    log.error("loadDocuments: Could not add document " + filename, ex);
                    continue;
                }

                log.debug("loadDocuments: added document " + filename);
            }
            else {
                // We need to check if this is a new version of the document.
                XMLGregorianCalendar existingVersion = existingDocument.getDocument().getVersion();
                if (existingVersion != null &&
                        existingVersion.compare(document.getVersion()) == DatatypeConstants.LESSER) {
                    // The existing version is older so add the new one.
                    try {
                        this.updateDocument(document, Source.LOCAL);
                        log.debug("loadDocuments: updated document " + filename);
                    }
                    catch (WebApplicationException ex) {
                        log.error("loadDocuments: Could not update document " + filename, ex);
                    }
               }
            }
        }
    }
}

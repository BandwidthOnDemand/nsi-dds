package net.es.nsi.dds.dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.provider.Document;
import net.es.nsi.dds.util.XmlUtilities;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DocumentCache encapsulates DDS document storage through an in memory map
 * and temporary directory cache for fast restarts.  This singleton object has
 * a life cycle managed through Spring.
 *
 * @author hacksaw
 */
public class DocumentCache {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // The holder of our configuration.
    private final DdsProfile ddsProfile;

    // In-memory document cache indexed by nsa/type/id.
    private final Map<String, Document> documents = new ConcurrentHashMap<>();

    // Are we configured to use the temporary document cache directory?
    private boolean enabled = false;
    private String cachePath;

    /**
     * Create an instance of the DocumentCache.  This is instantiated as a
     * singleton bean from Spring.
     *
     * @param ddsProfile The configuration reader bean holding the DDS runtime
     * configuration information.
     * @throws FileNotFoundException
     */
    public DocumentCache(DdsProfile ddsProfile) throws FileNotFoundException {
        this.ddsProfile = ddsProfile;

        // Check to see if we have a local cache to fast start from last contents.
        if (ddsProfile.getDirectory() != null && !ddsProfile.getDirectory().isEmpty()) {
            File dir = new File(ddsProfile.getDirectory());
            cachePath = dir.getAbsolutePath();
            log.debug("Cache directory configured " + cachePath);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    throw new FileNotFoundException("Cannot create directory: " + cachePath);
                }
            }

            // We will be using the cache for this deployment.
            enabled = true;
        }
    }

    /**
     * Get an instance of this document cache singleton.
     *
     * @return
     */
    public static DocumentCache getInstance() {
        DocumentCache documentCache = SpringApplicationContext.getBean("documentCache", DocumentCache.class);
        return documentCache;
    }

    /**
     * Get the document object associated with id.
     *
     * @param id Unique identifier of the document to get.
     * @return
     */
    public Document get(String id) {
        return documents.get(id);
    }

    /**
     * Add the specified document object to the cache and temporary storage
     * overwriting any document of the same id.
     *
     * @param id Unique identifier of the document to add.
     * @param doc The new document object.
     * @return The previous document object associated with id or null.
     * @throws JAXBException
     * @throws IOException
     */
    public Document put(String id, Document doc) throws JAXBException, IOException {
        // Store the document.
        Document result = documents.put(id, doc);
        if (result == null) {
            writeFile(doc, null);
        }
        else {
            writeFile(doc, result.getFilename());
        }
        return result;
    }

    /**
     * Replace existing document in cache with specified new document object
     * and update contents in temporary storage.
     *
     * @param id Unique identifier of the document to replace.
     * @param doc The new document object.
     * @return The new document with update filename component.
     * @throws JAXBException
     * @throws IOException
     */
    public Document update(String id, Document doc) throws JAXBException, IOException {
        return put(id, doc);
    }

    /**
     * Remove the specified document from the cache.
     *
     * @param id Unique identifier of the document to remove.
     * @return Return the document removed from cache, null otherwise.
     */
    public Document remove(String id) {
        Document doc = documents.remove(id);
        if (doc != null && enabled) {
            deleteFile(doc.getFilename());
        }

        return doc;
    }

    public void putAll(DocumentCache cache) {
        this.documents.putAll(cache.getDocuments());
    }

    /**
     * Returns a collection of all documents currently in the document cache.
     *
     * @return
     */
    public Collection<Document> values() {
        return documents.values();
    }

    /**
     * Load cache with all document files from local cache directory.
     */
    public void load() {
        loadDirectory(cachePath);
    }

    /**
     * Load cache with all document files from local cache directory.
     * @param path
     * @return
     */
    public Map<String, String> loadDirectory(String path) {
        Map<String, String> list = new ConcurrentHashMap<>();

        if (!enabled) {
            log.info("load: cache directory not configured.");
            return list;
        }

        Collection<String> xmlFilenames = XmlUtilities.getXmlFilenames(path);

        for (String filename : xmlFilenames) {
            log.info("load: loading " + filename);
            DocumentType document;
            try {
                document = DdsParser.getInstance().readDocument(filename);
                if (document == null) {
                    log.error("load: Loaded empty document from " + filename);
                    deleteFile(filename);
                    continue;
                }
            }
            catch (JAXBException | IOException ex) {
                log.error("load: Failed to load file " + filename, ex);
                deleteFile(filename);
                continue;
            }

            // We need to determine if this document is still valid
            // before proceding.
            XMLGregorianCalendar expires = document.getExpires();
            if (expires != null) {
                Date expiresTime = expires.toGregorianCalendar().getTime();

                // We take the current time and add the expiry buffer.
                Date now = new Date();
                now.setTime(now.getTime() + ddsProfile.getExpiryInterval() * 1000);
                if (expiresTime.before(now)) {
                    // This document is old and no longer valid.
                    log.error("load: Loaded document has expired " + filename + ", expires=" + expires.toGregorianCalendar().getTime().toString());
                    deleteFile(filename);
                    continue;
                }
            }
            else {
                // No expire value provided so make one.
                Date date = new Date(System.currentTimeMillis() + XmlUtilities.ONE_YEAR);
                XMLGregorianCalendar xmlGregorianCalendar;
                try {
                    xmlGregorianCalendar = XmlUtilities.xmlGregorianCalendar(date);
                } catch (DatatypeConfigurationException ex) {
                    log.error("load: Document does not contain an expires date and creation of one failed id=" + document.getId());
                    continue;
                }

                document.setExpires(xmlGregorianCalendar);
            }

            Document entry = new Document(document, ddsProfile.getBaseURL());
            entry.setFilename(filename);

            // Make sure the file we are loading does not overwrite a newer
            // version of the document.  If it is an older version then remove
            // it from the cache and disk.
            Document result = documents.get(entry.getId());

            if (result == null) {
                documents.put(entry.getId(), entry);
                log.debug("load: added document id=" + entry.getId() + ", filename=" + filename);
                list.put(entry.getId(), entry.getFilename());
            }
            else if (entry.getDocument().getVersion().compare(result.getDocument().getVersion()) == DatatypeConstants.GREATER) {
                log.info("load: new document found so removing old cached document id=" + result.getId() + ", filename="+ result.getFilename());
                documents.remove(result.getId());
                deleteFile(result.getFilename());
                documents.put(entry.getId(), entry);
                log.info("load: added new document id=" + entry.getId() + ", filename=" + filename);
                list.put(entry.getId(), entry.getFilename());
            }
            else {
                log.info("load: document currently in cache is newer, removing old document id=" + entry.getId() + ", filename=" + filename);
                deleteFile(filename);
            }
        }

        return list;
    }

    /**
     * Expire any documents in the cache past expire time plus the expiryInterval
     * offset.  We give this extra padding to allow clients to get any delete
     * updates that were sent (deletes are done in the DDS protocol by setting
     * expire time to now.
     */
    public void expire() {
        for (Document document : documents.values()) {
            // We need to determine if this document is still valid
            // before proceding.
            DocumentType doc = document.getDocument();
            XMLGregorianCalendar expires = doc.getExpires();
            if (expires != null) {
                Date expiresTime = expires.toGregorianCalendar().getTime();

                // We take the current time and add the expiry buffer.
                Date now = new Date();
                now.setTime(now.getTime() + ddsProfile.getExpiryInterval() * 1000);
                if (expiresTime.before(now)) {
                    // This document is old and no longer valid.
                    log.debug("expire: document has expired " + document.getId() + ", expires=" + expires.toGregorianCalendar().getTime().toString());
                    this.remove(document.getId());
                }
            }
        }
    }

    /**
     * Is document cache configured for temporary storage of in memory documents
     * on disk?
     *
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Delete specified file from document cache.
     *
     * @param filename File to delete.
     */
    public void deleteFile(String filename) {
        File file = new File(filename);
        if(!file.delete()) {
            log.error("DocumentCache: Delete failed for file  " + filename);
        }
        else {
            log.info("DocumentCache: Deleted old document " + filename);
        }
    }

    private void writeFile(Document doc, String filename) throws JAXBException, IOException {
        // We need to write this new document to disk.
        if (enabled) {
            if (filename == null || filename.isEmpty()) {
                filename = Paths.get(cachePath, UUID.randomUUID().toString() + ".xml").toString();
            }
            doc.setFilename(filename);
            DdsParser.getInstance().writeDocument(doc.getFilename(), doc.getDocument());
        }
    }

    /**
     * @return the documents
     */
    public Map<String, Document> getDocuments() {
        return Collections.unmodifiableMap(documents);
    }
}

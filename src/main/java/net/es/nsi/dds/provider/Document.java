package net.es.nsi.dds.provider;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.dds.api.DiscoveryError;
import net.es.nsi.dds.api.Exceptions;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;

/**
 *
 * @author hacksaw
 */
public class Document implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DOCUMENTS_URL = "documents";
    private static final ObjectFactory factory = new ObjectFactory();

    private String id;
    private String filename;
    private DocumentType document;
    private Date lastDiscovered;

    public Document(DocumentType document, String baseURL) throws WebApplicationException {
        this.id = documentId(document.getNsa(), document.getType(), document.getId());
        this.document = document;
        this.document.setNsa(document.getNsa().trim());
        this.document.setType(document.getType().trim());
        this.document.setId(document.getId().trim());
        this.document.setHref(getDocumentURL(baseURL));

        lastDiscovered = new Date();
        lastDiscovered.setTime(lastDiscovered.getTime() - lastDiscovered.getTime() % 1000);
    }

    public static String documentId(String nsa, String type, String id) throws WebApplicationException {
        if (nsa == null || nsa.trim().isEmpty()) {
            throw Exceptions.missingParameterException("document", "nsa");
        }
        else if (type == null || type.trim().isEmpty()) {
            throw Exceptions.missingParameterException("document", "type");
        }
        else if (id == null || id.trim().isEmpty()) {
            throw Exceptions.missingParameterException("document", "id");
        }

        StringBuilder sb = new StringBuilder();

        try {
            sb.append(URLEncoder.encode(nsa.trim(), "UTF-8"));
            sb.append("/").append(URLEncoder.encode(type.trim(), "UTF-8"));
            sb.append("/").append(URLEncoder.encode(id.trim(), "UTF-8"));
        }
        catch (UnsupportedEncodingException ex) {
            throw Exceptions.illegalArgumentException(DiscoveryError.DOCUMENT_INVALID, "document", "id");
        }
        return sb.toString();
    }

    public static String documentId(DocumentType document) throws IllegalArgumentException {
        return documentId(document.getNsa(), document.getType(), document.getId());
    }

    private String getDocumentURL(String baseURL) throws WebApplicationException {
        URL url;
        try {
            if (!baseURL.endsWith("/")) {
                baseURL = baseURL + "/";
            }
            url = new URL(baseURL);
            url = new URL(url, DOCUMENTS_URL + "/" + this.id);
        } catch (MalformedURLException ex) {
            throw Exceptions.illegalArgumentException(DiscoveryError.INVALID_PARAMETER, "document", "href");
        }

        return url.toExternalForm();
    }

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
     * @return the document
     */
    public DocumentType getDocument() {
        return document;
    }

    /**
     * @return the document
     */
    public DocumentType getDocumentSummary() {
        DocumentType newDocType = factory.createDocumentType();
        newDocType.setExpires(document.getExpires());
        newDocType.setHref(document.getHref());
        newDocType.setId(document.getId());
        newDocType.setNsa(document.getNsa());
        newDocType.setType(document.getType());
        newDocType.setVersion(document.getVersion());
        return newDocType;
    }

    /**
     * @param document the document to set
     */
    public void setDocument(DocumentType document) {
        this.document = document;
    }

    /**
     * @return the lastModified
     */
    public Date getLastDiscovered() {
        return lastDiscovered;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastDiscovered(Date lastModified) {
        this.lastDiscovered = lastModified;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }
}

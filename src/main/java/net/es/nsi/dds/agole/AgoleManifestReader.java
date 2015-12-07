package net.es.nsi.dds.agole;

import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import net.es.nsi.dds.client.RestClient;
import net.es.nsi.dds.jaxb.NmlParser;
import net.es.nsi.dds.jaxb.nml.NmlNetworkObject;
import net.es.nsi.dds.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.management.logs.DdsErrors;
import net.es.nsi.dds.management.logs.DdsLogger;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class reads a remote XML formatted NML topology containing the list of
 * network topologies and their NSA.  Each instance of the class
 * models a single NSA in NML.
 *
 * @author hacksaw
 */
public class AgoleManifestReader {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DdsLogger topologyLogger = DdsLogger.getLogger();

    private final static QName _isReference_QNAME = new QName("http://schemas.ogf.org/nsi/2013/09/topology#", "isReference");

    // The remote location of the file to read.
    private final String id = getClass().getName();

    // The remote location of the file to read.
    private String target = null;

    // Time we last read the master topology.
    private long lastModified = 0;

    // The version of the last read master topology.
    private TopologyManifest manifest = null;

    private final RestClient restClient;

    /**
     * Default class constructor.
     *
     * @param restClient
     */
    public AgoleManifestReader(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Returns the identifier of this manifest reader.
     *
     * @return the identifier of the manifest reader.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the configured remote topology endpoint.
     *
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the remote topology endpoint.
     *
     * @param target the target to set
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Get the date the remote topology endpoint reported as the last time the
     * topology document was modified.
     *
     * @return the lastModified date of the remote topology document.
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Set the last modified date of the cached remote topology document.
     *
     * @param lastModified the lastModified to set
     */
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Read the NML topology from target location using HTTP GET operation.
     * This method will return a list of target topology endpoints if the
     * master topology document was retrieved.  NULL is returned if the
     * topology endpoint reports no modifications since last retrieval.  An
     * exception is thrown for any errors.
     *
     * @return The list of topology endpoints from the remote NML topology.
     */
    private TopologyManifest readManifest() throws NotFoundException, JAXBException {
        // Use the REST client to retrieve the master topology as a string.
        Client client = restClient.get();
        WebTarget webGet = client.target(getTarget());

        Response response = null;
        try {
            response = webGet.request(MediaType.APPLICATION_XML) .header("If-Modified-Since", DateUtils.formatDate(new Date(getLastModified()), DateUtils.PATTERN_RFC1123)).get();

            // A 304 Not Modified indicates we already have a up-to-date document.
            if (response.getStatus() == Status.NOT_MODIFIED.getStatusCode()) {
                log.debug("readManifest: no changes to {}", getTarget());
            }
            else if (response.getStatus() == Status.OK.getStatusCode()) {
                log.debug("readManifest: processing changes to {}", getTarget());

                // We want to store the last modified date as viewed from the HTTP server.
                Date lastMod = response.getLastModified();
                if (lastMod != null) {
                    log.debug("readManifest: Updating last modified time {}", DateUtils.formatDate(lastMod, DateUtils.PATTERN_RFC1123));
                    setLastModified(lastMod.getTime());
                }

                // Now we want the NML XML document.
                String xml = response.readEntity(String.class);

                // Parse the master topology.
                NmlTopologyType topology = NmlParser.getInstance().xml2Jaxb(NmlTopologyType.class, xml);

                // Create an internal object to hold the master list.
                TopologyManifest newManifest = new TopologyManifest();
                newManifest.setId(topology.getId());
                if (topology.getVersion() != null) {
                    newManifest.setVersion(topology.getVersion().toGregorianCalendar().getTimeInMillis());
                }

                // Pull out the indivdual network entries.
                List<NmlNetworkObject> networkObjects = topology.getGroup();
                for (NmlNetworkObject networkObject : networkObjects) {
                    if (networkObject instanceof NmlTopologyType) {
                        NmlTopologyType innerTopology = (NmlTopologyType) networkObject;
                        Map<QName, String> otherAttributes = innerTopology.getOtherAttributes();
                        String isReference = otherAttributes.get(_isReference_QNAME);
                        if (isReference != null && !isReference.isEmpty()) {
                            log.debug("readManifest: topology id: {}, isReference: ", networkObject.getId(), isReference);
                            newManifest.setTopologyURL(networkObject.getId(), isReference);
                        }
                        else {
                            topologyLogger.errorAudit(DdsErrors.AUDIT_MANIFEST_MISSING_ISREFERENCE, getTarget(), networkObject.getId());
                        }
                    }
                }

                return newManifest;
            }
            else {
                throw new NotFoundException("Failed to retrieve master topology, status = " + Integer.toString(response.getStatus()) + ", url=" + getTarget());
            }
        }
        catch (JAXBException ex) {
            topologyLogger.errorAudit(DdsErrors.AUDIT_MANIFEST_XML_PARSE, getTarget(), ex.getMessage());
            throw ex;
        }
        catch (NotFoundException ex) {
            topologyLogger.errorAudit(DdsErrors.AUDIT_MANIFEST_COMMS, getTarget(), ex.getMessage());
            //client.close();
            throw ex;
        }
        finally {
            if (response != null) {
                response.close();
            }
        }

        return null;
    }

    /**
     * Returns a current version of the master topology, retrieving a new
     * version from the remote endpoint if available.
     *
     * @throws javax.xml.bind.JAXBException
     */
    public synchronized void loadManifest() throws NotFoundException, JAXBException {

        TopologyManifest newManifest = this.readManifest();
        if (newManifest != null && manifest == null) {
            // We don't have a previous version so update with this version.
            manifest = newManifest;
        }
        else if (newManifest != null && manifest != null) {
            // Only update if this version is newer.
            if (newManifest.getVersion() == 0) {
                // Missing version information so we have to assume an update.
                manifest = newManifest;
            }
            else if (newManifest.getVersion() > manifest.getVersion()) {
                manifest = newManifest;
            }
        }
    }

    /**
     * Returns a current version of the master topology.  The masterTopology
     * will be loaded only if there has yet to be a successful load.
     *
     * @return Master topology.
     * @throws javax.xml.bind.JAXBException
     */
    public TopologyManifest getManifest() throws NotFoundException, JAXBException {
        if (manifest == null) {
            loadManifest();
        }

        return manifest;
    }

    /**
     * Returns a current version of the master topology only if a new version
     * was available from the remote endpoint if available.
     *
     * @return
     * @throws javax.xml.bind.JAXBException
     */
    public TopologyManifest getManifestIfModified() throws NotFoundException, JAXBException {
        TopologyManifest oldMasterTopology = manifest;
        loadManifest();
        TopologyManifest newMasterTopology = manifest;

        if (newMasterTopology != null && oldMasterTopology == null) {
            // We don't have a previous version so there is a change.
            return manifest;
        }
        else if (newMasterTopology != null && oldMasterTopology != null) {
            // Only update if this version is newer.
            if (newMasterTopology.getVersion() > oldMasterTopology.getVersion()) {
                return manifest;
            }
        }

        // There must not have been a change.
        return null;
    }
}

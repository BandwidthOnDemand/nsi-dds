package net.es.nsi.dds.agole;

import java.util.Date;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.client.RestClient;
import net.es.nsi.dds.jaxb.NmlParser;
import net.es.nsi.dds.jaxb.nml.NmlNSAType;
import net.es.nsi.dds.management.logs.DdsErrors;
import net.es.nsi.dds.management.logs.DdsLogger;
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ChunkedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class reads a remote XML formatted NML topology and creates simple
 * network objects used to later build NSI topology.  Each instance of the class
 * models a single NSA in NML.
 *
 * @author hacksaw
 */
public class AgoleTopologyReader {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DdsLogger topologyLogger = DdsLogger.getLogger();
    private String target;
    private long lastModifiedTime;

    private final Client client;

    /**
     * Default class constructor.
     */
    public AgoleTopologyReader() {
        client = RestClient.getInstance().get();
    }

    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     *
     * @param target Location of the NSA's XML based NML topology.
     * @param lastModifiedTime
     */
    public AgoleTopologyReader(String target, long lastModifiedTime) {
        this.target = target;
        this.lastModifiedTime = lastModifiedTime;
        client = RestClient.getInstance().get();
    }

    /**
     * Read the NML topology from target location using HTTP GET operation.
     *
     * @return The JAXB NSA element from the NML topology.
     * @throws javax.xml.bind.JAXBException
     */
    public NmlNSAType readNsaTopology() throws NotFoundException, IllegalStateException, JAXBException {
        // Use the REST client to retrieve the master topology as a string.
        WebTarget webGet = client.target(target);

        Response response = null;
        NmlNSAType topology = null;
        try {
            response = webGet.request(MediaType.APPLICATION_XML).header("If-Modified-Since", DateUtils.formatDate(new Date(getLastModifiedTime()), DateUtils.PATTERN_RFC1123)).get();

            // A 304 Not Modified indicates we already have a up-to-date document.
            if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
                log.debug("readNsaTopology: NOT_MODIFIED returned {}", target);
                return null;
            }
            else if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                topologyLogger.errorAudit(DdsErrors.AUDIT_NSA_COMMS, target, Integer.toString(response.getStatus()));
                throw new NotFoundException("Failed to retrieve NSA topology " + target);
            }

            // We want to store the last modified date as viewed from the HTTP server.
            Date lastMod = response.getLastModified();
            log.debug("readNsaTopology: lastModified = {}, current = {}", new Date(getLastModifiedTime()), lastMod);

            if (lastMod != null) {
                log.debug("readNsaTopology: Updating last modified time to {}", lastMod);
                lastModifiedTime = lastMod.getTime();
            }

            // Now we want the NML XML document.  We have to read this as a string
            // because GitHub is returning incorrect media type (text/plain).
            StringBuilder result = new StringBuilder();
            try (ChunkedInput<String> chunkedInput = response.readEntity(new GenericType<ChunkedInput<String>>() {})) {
                String chunk;
                while ((chunk = chunkedInput.read()) != null) {
                    result.append(chunk);
                }
            }

            // Parse the NSA topology.
            topology = NmlParser.getInstance().xml2Jaxb(NmlNSAType.class, result.toString());
        }
        catch (NotFoundException | IllegalStateException | JAXBException ex) {
            topologyLogger.errorAudit(DdsErrors.AUDIT_NSA_COMMS, target, ex.getMessage());
            throw ex;
        }
        finally {
            if (response != null) {
                response.close();
            }
        }

        // We should never get this - an exception should be thrown.
        if (topology == null) {
            topologyLogger.errorAudit(DdsErrors.AUDIT_NSA_XML_PARSE, target);
        }

        return topology;
    }

    /**
     * @return the lastModifiedTime
     */
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }
}

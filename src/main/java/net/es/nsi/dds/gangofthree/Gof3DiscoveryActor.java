/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.gangofthree;

import akka.actor.UntypedActor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.api.jaxb.AnyType;
import net.es.nsi.dds.api.jaxb.DocumentEventType;
import net.es.nsi.dds.api.jaxb.DocumentType;
import net.es.nsi.dds.api.jaxb.InterfaceType;
import net.es.nsi.dds.api.jaxb.NmlTopologyType;
import net.es.nsi.dds.api.jaxb.NotificationType;
import net.es.nsi.dds.api.jaxb.NsaType;
import net.es.nsi.dds.api.jaxb.ObjectFactory;
import net.es.nsi.dds.client.RestClient;
import net.es.nsi.dds.provider.DdsProvider;
import net.es.nsi.dds.schema.NmlParser;
import net.es.nsi.dds.schema.NsiConstants;
import net.es.nsi.dds.schema.XmlUtilities;
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ChunkedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class Gof3DiscoveryActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();
    private Client client;

    /**
     *
     */
    @Override
    public void preStart() {
        client = RestClient.getInstance().get();
    }

    /**
     *
     * @param msg
     */
    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Gof3DiscoveryMsg) {
            Gof3DiscoveryMsg message = (Gof3DiscoveryMsg) msg;

            // Read the NSA discovery document.
            if (discoverNSA(message) == false) {
                // No update so return.
                return;
            }

            // Read the associated Topology document.
            for (Map.Entry<String, Long> entry : message.getTopology().entrySet()) {
                Long result = discoverTopology(message.getNsaId(), entry.getKey(), entry.getValue());
                if (result != null) {
                    message.setTopologyLastModified(entry.getKey(), result);
                }
            }

            // Send an updated discovery message back to the router.
            getSender().tell(message, getSelf());
        } else {
            unhandled(msg);
        }
    }

    /**
     * Performs a Gang of Three NSA discovery by reading an NSA description
     * file from a pre-defined URL.
     *
     * @param message Discovery instructions for the target NSA.
     * @return Returns true if the NSA document was successfully discovered,
     *         false otherwise.
     */
    private boolean discoverNSA(Gof3DiscoveryMsg message) {
        log.debug("discoverNSA: nsa=" + message.getNsaURL() + ", lastModifiedTime=" + new Date(message.getNsaLastModifiedTime()));

        //Client client = ClientBuilder.newClient(clientConfig);
        long time = message.getNsaLastModifiedTime();
        Response response = null;
        try {
            WebTarget nsaTarget = client.target(message.getNsaURL());

            log.debug("discoverNSA: querying with If-Modified-Since: " + DateUtils.formatDate(new Date(time)));

            // Some NSA do not support the NSI_NSA_V1 application string so
            // accept any encoding for the document.
            response = nsaTarget.request("*/*") // NsiConstants.NSI_NSA_V1
                    .header("If-Modified-Since", DateUtils.formatDate(new Date(time), DateUtils.PATTERN_RFC1123))
                    .get();

            Date lastModified = response.getLastModified();
            if (lastModified != null ) {
                log.debug("discoverNSA: time=" + new Date(time) + ", lastModified=" + new Date(lastModified.getTime()));
                time = lastModified.getTime();
            }
            else {
                log.debug("discoverNSA: lastModified returned null");
            }

            log.debug("discoverNSA: response status " + response.getStatus());

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                // Read the NSA description into a string buffer to avoid parsing
                // errors on goofy characters.
                StringBuilder result = new StringBuilder();
                try (ChunkedInput<String> chunkedInput = response.readEntity(new GenericType<ChunkedInput<String>>() {})) {
                    String chunk;
                    while ((chunk = chunkedInput.read()) != null) {
                        result.append(chunk);
                    }
                }

                // Now parse the string into an NML Topology object.
                NsaType nsa = NmlParser.getInstance().parseNsaFromString(result.toString());

                // We have a document to process.
                if (nsa == null) {
                    // Clear the topology URL and lastModified dates for this
                    // error.
                    log.error("discoverNSA: NSA document is empty from endpoint " + message.getNsaURL());
                    message.setNsaLastModifiedTime(0L);
                    message.clearTopology();
                    return false;
                }

                message.setNsaId(nsa.getId());

                // Find the topology endpoint for the next step.
                Map<String, Long> oldTopologyRefs = new HashMap<>(message.getTopology());
                for (InterfaceType inf : nsa.getInterface()) {
                    if (NsiConstants.NSI_TOPOLOGY_V2.equalsIgnoreCase(inf.getType().trim())) {
                        String url = inf.getHref().trim();
                        Long previous = oldTopologyRefs.remove(url);
                        if (previous == null) {
                            message.addTopology(url, 0L);
                        }
                    }
                }

                // Now we clean up the topology URL that are no longer in the
                // NSA description document.
                for (String url : oldTopologyRefs.keySet()) {
                    message.removeTopologyURL(url);
                }

                XMLGregorianCalendar cal = XmlUtilities.longToXMLGregorianCalendar(time);

                if (addNsaDocument(nsa, cal) == false) {
                    log.debug("discoverNSA: addNsaDocument() returned false " + message.getNsaURL());
                    return false;
                }
            }
            else if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
                // We did not get an updated document.
                log.debug("discoverNSA: NSA document not modified " + message.getNsaURL());
            }
            else {
                log.error("discoverNSA: get of NSA document failed " + response.getStatus() + ", url=" + message.getNsaURL());
                // TODO: Should we clear the topology URL and lastModified
                // dates for errors and route back?
                return false;
            }
        }
        catch (IllegalStateException ex) {
            log.error("discoverNSA: failed to retrieve NSA document from endpoint " + message.getNsaURL(), ex);
            return false;
        }
        catch (JAXBException ex) {
            log.error("discoverNSA: invalid document returned from endpoint " + message.getNsaURL(), ex);
            return false;
        }
        catch (DatatypeConfigurationException ex) {
            log.error("discoverNSA: NSA document failed to create lastModified " + message.getNsaURL(), ex);
            return false;
        }
        catch (Exception ex) {
            log.error("discoverNSA: unknown error " + message.getNsaURL(), ex);
            return false;
        }
        finally {
            if (response != null) {
                // Close the response to avoid leaking.
                log.error("discoverNSA: closing response.");
                response.close();
            }
            else {
                log.error("discoverNSA: not closing response.");
            }
        }

        message.setNsaLastModifiedTime(time);

        log.debug("discoverNSA: exiting for nsa=" + message.getNsaURL() + " with lastModifiedTime=" + new Date(time));

        return true;
    }

    /**
     *
     */
    private Long discoverTopology(String nsaId, String url, Long lastModifiedTime) {
        log.debug("discoverTopology: topology=" + url);

        //Client client = ClientBuilder.newClient(clientConfig);
        long time = lastModifiedTime;
        Response response = null;
        try {
            WebTarget topologyTarget = client.target(url);
            response = topologyTarget.request("*/*") // MediaTypes.NSI_TOPOLOGY_V2
                    .header("If-Modified-Since", DateUtils.formatDate(new Date(time), DateUtils.PATTERN_RFC1123))
                    .get();

            Date lastModified = response.getLastModified();
            if (lastModified != null) {
                log.debug("discoverTopology: time=" + new Date(time) + ", lastModified=" + new Date(lastModified.getTime()));
                time = lastModified.getTime();
            }
            else {
                log.debug("discoverTopology: lastModified is null time=" + new Date(time));
            }

            log.debug("discoverNSA: response status " + response.getStatus());

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                // Read the NML topology into a string buffer to avoid parsing
                // errors on goofy characters.
                StringBuilder result = new StringBuilder();
                try (ChunkedInput<String> chunkedInput = response.readEntity(new GenericType<ChunkedInput<String>>() {})) {
                    String chunk;
                    while ((chunk = chunkedInput.read()) != null) {
                        result.append(chunk);
                    }
                }

                // Now parse the string into an NML Topology object.
                NmlTopologyType nml = NmlParser.getInstance().parseTopologyFromString(result.toString());

                if (nml != null) {
                    // Add the document.
                    XMLGregorianCalendar cal;
                    cal = XmlUtilities.longToXMLGregorianCalendar(time);

                    if (addTopologyDocument(nml, cal, nsaId) == false) {
                        log.debug("discoverTopology: addTopologyDocument() returned false " + url);
                    }
                }
                else {
                    // TODO: Should we clear the topology URL and lastModified
                    // dates for errors and route back?
                    log.error("discoverTopology: Topology document is empty from endpoint " + url);
                }
            }
            else if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
                // We did not get an updated document.
                log.debug("discoverTopology: Topology document not modified (" + response.getLastModified() + ") for topology=" + url);
            }
            else {
                log.error("discoverTopology: get of Topology document failed " + response.getStatus() + ", topology=" + url);
            }
        }
        catch (IllegalStateException ex) {
            // TODO: Should we clear the topology URL and lastModified
            // dates for errors and route back?
            log.error("discoverTopology: failed to retrieve Topology document from endpoint " + url);
            time = 0L;
        }
        catch (JAXBException ex) {
            log.error("discoverTopology: invalid document returned from endpoint " + url);
            time = 0L;
        }
        catch (DatatypeConfigurationException ex) {
            log.error("discoverTopology: Topology document failed to create lastModified " + url);
            time = 0L;
        }
        finally {
            if (response != null) {
                // Close the response to avoid leaking.
                log.error("discoverTopology: closing response.");
                response.close();
            }
            else {
                log.error("discoverTopology: not closing response.");
            }
        }

        log.debug("discoverTopology: exiting for topology=" + url + " with lastModifiedTime=" + new Date(time));
        return time;
    }

    /**
     *
     * @param nsa
     * @param discovered
     * @return
     */
    private boolean addNsaDocument(NsaType nsa, XMLGregorianCalendar discovered) {
        // Now we add the NSA document into the DDS.
        DocumentType document = factory.createDocumentType();

        // Set the naming attributes.
        document.setId(nsa.getId());
        document.setType(NsiConstants.NSI_DOC_TYPE_NSA_V1);
        document.setNsa(nsa.getId());

        // We need the version of the document.
        document.setVersion(nsa.getVersion());

        // If there is no expires time specified then it is infinite.
        if (nsa.getExpires() == null || !nsa.getExpires().isValid()) {
            // No expire value provided so make one.
            Date date = new Date(System.currentTimeMillis() + XmlUtilities.ONE_YEAR);
            XMLGregorianCalendar xmlGregorianCalendar;
            try {
                xmlGregorianCalendar = XmlUtilities.xmlGregorianCalendar(date);
            } catch (DatatypeConfigurationException ex) {
                log.error("discover: NSA document does not contain an expires date, id=" + nsa.getId());
                return false;
            }

            document.setExpires(xmlGregorianCalendar);
        }
        else {
            document.setExpires(nsa.getExpires());
        }

        // Add the NSA document into the entry.
        AnyType any = factory.createAnyType();
        any.getAny().add(factory.createNsa(nsa));
        document.setContent(any);

        // Try to add the document as a notification of document change.
        NotificationType notify = factory.createNotificationType();
        notify.setEvent(DocumentEventType.ALL);
        notify.setDiscovered(discovered);
        notify.setDocument(document);

        DdsProvider.getInstance().processNotification(notify);

        return true;
    }

    /**
     *
     * @param topology
     * @param discovered
     * @param nsaId
     * @return
     */
    private boolean addTopologyDocument(NmlTopologyType topology, XMLGregorianCalendar discovered, String nsaId) {

        // Now we add the Topology document into the DDS.
        DocumentType document = factory.createDocumentType();

        // Set the naming attributes.
        document.setId(topology.getId());
        document.setType(NsiConstants.NSI_DOC_TYPE_TOPOLOGY_V2);
        document.setNsa(nsaId);

        // If there is no version specified then we add one.
        if (topology.getVersion() == null || !topology.getVersion().isValid()) {
            // No expire value provided so make one.
            XMLGregorianCalendar xmlGregorianCalendar;
            try {
                xmlGregorianCalendar = XmlUtilities.xmlGregorianCalendar();
            } catch (DatatypeConfigurationException ex) {
                log.error("discover: Topology document does not contain a version, id=" + topology.getId());
                return false;
            }

            document.setVersion(xmlGregorianCalendar);
        }
        else {
            document.setVersion(topology.getVersion());
        }

        // If there is no expires time specified then it is infinite.
        if (topology.getLifetime() == null || topology.getLifetime().getEnd() == null || !topology.getLifetime().getEnd().isValid()) {
            // No expire value provided so make one.
            Date date = new Date(System.currentTimeMillis() + XmlUtilities.ONE_YEAR);
            XMLGregorianCalendar xmlGregorianCalendar;
            try {
                xmlGregorianCalendar = XmlUtilities.xmlGregorianCalendar(date);
            } catch (DatatypeConfigurationException ex) {
                log.error("discover: Topology document does not contain an expires date, id=" + topology.getId());
                return false;
            }

            document.setExpires(xmlGregorianCalendar);
        }
        else {
            document.setExpires(topology.getLifetime().getEnd());
        }

        // Add the NSA document into the entry.
        AnyType any = factory.createAnyType();
        any.getAny().add(factory.createTopology(topology));
        document.setContent(any);

        // Try to add the document as a notification of document change.
        NotificationType notify = factory.createNotificationType();
        notify.setEvent(DocumentEventType.ALL);
        notify.setDiscovered(discovered);
        notify.setDocument(document);

        DdsProvider.getInstance().processNotification(notify);

        return true;
    }
}
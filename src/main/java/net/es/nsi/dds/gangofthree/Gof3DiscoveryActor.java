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
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.api.jaxb.AnyType;
import net.es.nsi.dds.api.jaxb.DocumentEventType;
import net.es.nsi.dds.api.jaxb.DocumentType;
import net.es.nsi.dds.api.jaxb.InterfaceType;
import net.es.nsi.dds.api.jaxb.NotificationType;
import net.es.nsi.dds.api.jaxb.NsaType;
import net.es.nsi.dds.api.jaxb.NmlTopologyType;
import net.es.nsi.dds.api.jaxb.ObjectFactory;
import net.es.nsi.dds.provider.DdsProvider;
import net.es.nsi.dds.jersey.RestClient;
import net.es.nsi.dds.schema.NsiConstants;
import net.es.nsi.dds.schema.XmlUtilities;
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class Gof3DiscoveryActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();
    private ClientConfig clientConfig;

    @Override
    public void preStart() {
        clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
    }

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

    private boolean discoverNSA(Gof3DiscoveryMsg message) {
        log.debug("discover: nsa=" + message.getNsaURL());
        Client client = ClientBuilder.newClient(clientConfig);

        WebTarget nsaTarget = client.target(message.getNsaURL());

        Response response;
        try {
            response = nsaTarget.request("*/*") // NsiConstants.NSI_NSA_V1
                    .header("If-Modified-Since", DateUtils.formatDate(new Date(message.getNsaLastModifiedTime()), DateUtils.PATTERN_RFC1123))
                    .get();
        }
        catch (Exception ex) {
            // TODO: Should we clear the topology URL and lastModified
            // dates for errors and route back?
            log.error("discover: failed to retrieve NSA document from endpoint " + message.getNsaURL(), ex);
            client.close();
            return false;
        }

        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            // We have a document to process.
            NsaType nsa = response.readEntity(NsaType.class);
            if (nsa == null) {
                // TODO: Should we clear the topology URL and lastModified
                // dates for errors and route back?
                log.error("discover: NSA document is empty from endpoint " + message.getNsaURL());
                response.close();
                client.close();
                return false;
            }

            response.close();
            client.close();

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

            // Add the document.
            long time = 0;
            if (response.getLastModified() != null) {
                time = response.getLastModified().getTime();
            }

            XMLGregorianCalendar cal;
            try {
                cal = XmlUtilities.longToXMLGregorianCalendar(time);
            } catch (DatatypeConfigurationException ex) {
                log.error("discover: NSA document failed to create lastModified " + message.getNsaURL());
                return false;
            }

            if (addNsaDocument(nsa, cal) == false) {
                return false;
            }
        }
        else if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
            // We did not get an updated document.
            log.debug("discover: NSA document not modified " + message.getNsaURL());
            response.close();
            client.close();
        }
        else {
            log.error("discover: get of NSA document failed " + response.getStatus() + ", url=" + message.getNsaURL());
            // TODO: Should we clear the topology URL and lastModified
            // dates for errors and route back?
            response.close();
            client.close();
            return false;
        }

        if (response.getLastModified() != null) {
            message.setNsaLastModifiedTime(response.getLastModified().getTime());
        }

        // Now we retrieve the associated topology document.
        log.debug("discover: exiting.");

        return true;
    }

    private Long discoverTopology(String nsaId, String url, Long lastModifiedTime) {
        log.debug("discover: topology=" + url);

        Client client = ClientBuilder.newClient(clientConfig);

        WebTarget topologyTarget = client.target(url);

        Response response;
        try {
            response = topologyTarget.request("*/*") // MediaTypes.NSI_TOPOLOGY_V2
                    .header("If-Modified-Since", DateUtils.formatDate(new Date(lastModifiedTime), DateUtils.PATTERN_RFC1123))
                    .get();
        }
        catch (Exception ex) {
            // TODO: Should we clear the topology URL and lastModified
            // dates for errors and route back?
            log.error("discoverTopology: failed to retrieve Topology document from endpoint " + url, ex);
            client.close();
            return 0L;
        }

        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            NmlTopologyType topology = null;
            try (ChunkedInput<NmlTopologyType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<NmlTopologyType>>() {})) {
                NmlTopologyType chunk;
                while ((chunk = chunkedInput.read()) != null) {
                    topology = chunk;
                }
            }

            response.close();
            client.close();

            if (topology == null) {
                // TODO: Should we clear the topology URL and lastModified
                // dates for errors and route back?
                log.error("discoverTopology: Topology document is empty from endpoint " + url);
                return 0L;
            }

            // Add the document.
            long time = 0;
            if (response.getLastModified() != null) {
                time = response.getLastModified().getTime();
            }

            XMLGregorianCalendar cal;
            try {
                cal = XmlUtilities.longToXMLGregorianCalendar(time);
            } catch (DatatypeConfigurationException ex) {
                log.error("discoverTopology: Topology document failed to create lastModified " + url);
                return 0L;
            }

            if (addTopologyDocument(topology, cal, nsaId) == false) {
                return 0L;
            }
        }
        else if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
            // We did not get an updated document.
            log.debug("discoverTopology: Topology document not modified " + url);
            response.close();
            client.close();
        }
        else {
            log.error("discoverTopology: get of Topology document failed " + response.getStatus() + ", url=" + url);
            // TODO: Should we clear the topology URL and lastModified
            // dates for errors and route back?
            response.close();
            client.close();
            return 0L;
        }

        // Now we retrieve the associated topology document.
        log.debug("discoverTopology: exiting.");

        if (response.getLastModified() == null) {
            return 0L;
        }

        return response.getLastModified().getTime();
    }

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

    private boolean addTopologyDocument(NmlTopologyType topology, XMLGregorianCalendar discovered, String nsaId) {

        // Now we add the NSA document into the DDS.
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.agole;

import akka.actor.UntypedActor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.jaxb.nml.NmlLifeTimeType;
import net.es.nsi.dds.jaxb.nml.NmlLocationType;
import net.es.nsi.dds.jaxb.nml.NmlNSARelationType;
import net.es.nsi.dds.jaxb.nml.NmlNSAType;
import net.es.nsi.dds.jaxb.nml.NmlServiceType;
import net.es.nsi.dds.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.jaxb.nsa.FeatureType;
import net.es.nsi.dds.jaxb.nsa.InterfaceType;
import net.es.nsi.dds.jaxb.nsa.LocationType;
import net.es.nsi.dds.jaxb.nsa.NsaType;
import net.es.nsi.dds.jaxb.nsa.PeerRoleEnum;
import net.es.nsi.dds.jaxb.nsa.PeersWithType;
import net.es.nsi.dds.lib.DocHelper;
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.util.XmlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class AgoleDiscoveryActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final net.es.nsi.dds.jaxb.nsa.ObjectFactory nsaFactory = new net.es.nsi.dds.jaxb.nsa.ObjectFactory();
    private final net.es.nsi.dds.jaxb.nml.ObjectFactory nmlFactory = new net.es.nsi.dds.jaxb.nml.ObjectFactory();

    @Override
    public void preStart() {
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof AgoleDiscoveryMsg) {
            AgoleDiscoveryMsg message = (AgoleDiscoveryMsg) msg;

            try {
                // Read the NML topology document.
                if (discoverTopology(message) == false) {
                    // No update so return.
                    return;
                }
            }
            catch (Exception ex) {
                log.error("onReceive: Caught exception", ex);
                return;
            }

            // Send an updated discovery message back to the router.
            getSender().tell(message, getSelf());
        } else {
            unhandled(msg);
        }
    }

    private boolean discoverTopology(AgoleDiscoveryMsg message) {
        String url = message.getTopologyURL();

        log.debug("discover: topology url={}", url);

        if (url == null || url.isEmpty()) {
            return false;
        }

        AgoleTopologyReader topologyReader = new AgoleTopologyReader(url, message.getTopologyLastModifiedTime());
        NmlNSAType nsa;
        try {
            nsa = topologyReader.readNsaTopology();
        } catch (NotFoundException | IllegalStateException | JAXBException ex) {
            log.error("discoverTopology: failed to read topology for url={}", url, ex);
            return false;
        }

        if (nsa == null) {
            log.debug("discoverTopology: Topology document not modified for url={}", url);
            return false;
        }

        XMLGregorianCalendar lastDiscovered;
        try {
            lastDiscovered = XmlUtilities.xmlGregorianCalendar();
        } catch (DatatypeConfigurationException ex) {
            log.error("discoverTopology: Failed to create a lastDiscovered value, id={}", nsa.getId(), ex);
            return false;
        }

        // We need to create both the NSA Discovery and Topology documents from
        // the contents of this single document.
        NsaType nsaDocument = parseNsa(nsa);
        if (nsaDocument == null || !DocHelper.addNsaDocument(nsaDocument, lastDiscovered)) {
            return false;
        }

        // Update the lastModified time and nsaId in the message so we can sent
        // it back to the router to update the master list.
        message.setTopologyLastModifiedTime(topologyReader.getLastModifiedTime());
        message.setNsaId(nsa.getId());

        Collection<NmlTopologyType> nmlDocuments = parseTopology(nsa, nsaDocument);
        nmlDocuments.stream().forEach((nmlDocument) -> {
            try {
                DocHelper.addTopologyDocument(nmlDocument, lastDiscovered, nsa.getId());
            }
            catch (Exception ex) {
                log.error("discoverTopology: Failed to topology document, nsaId={}, networkId={}", nsa.getId(), nmlDocument.getId());
            }
        });

        // Now we retrieve the associated topology document.
        log.debug("discoverTopology: exiting.");

        return true;
    }

    private NsaType parseNsa(NmlNSAType nsa) {
        // We need to create both the NSA Discovery and Topology documents from
        // the contents of this single document.
        NsaType nsaDocument = nsaFactory.createNsaType();
        nsaDocument.setId(nsa.getId());
        nsaDocument.setVersion(nsa.getVersion());
        nsaDocument.setName(nsa.getName());
        if (nsa.getLifetime() != null) {
            nsaDocument.setExpires(nsa.getLifetime().getEnd());
        }

        NmlLocationType location = nsa.getLocation();
        if (location != null) {
            LocationType loc = nsaFactory.createLocationType();
            loc.setAltitude(location.getAlt());
            loc.setLatitude(location.getLat());
            loc.setLongitude(location.getLong());
            loc.setName(location.getName());
            loc.setUnlocode(location.getUnlocode());
            nsaDocument.setLocation(loc);
        }

        // Add the uPA NSA feature.
        FeatureType upa = nsaFactory.createFeatureType();
        upa.setType(NsiConstants.NSI_CS_UPA);
        List<FeatureType> feature = nsaDocument.getFeature();
        feature.add(upa);

        //nsaDocument.setAdminContact(null);

        // Parse the NML peersWith relationship.
        try {
            nsaDocument.getPeersWith().addAll(parsePeersWith(nsa.getRelation()));
        }
        catch (Exception ex) {
            // Ignore the error for now.
            log.error("discoverTopology: failed to add NML peersWith relationship.", ex);
        }

        // Parse the NML Service element into an interface element.
        try {
            nsaDocument.getInterface().addAll(parseService(nsa.getService()));
        }
        catch (Exception ex) {
            // Ignore the error for now.
            log.error("discoverTopology: failed to add NML Service.", ex);
        }

        // We pull the networkId out of the <Topology> elements.
        List<String> networkId = nsaDocument.getNetworkId();
        nsa.getTopology().stream().forEach((topology) -> {
            networkId.add(topology.getId().trim());
        });

        return nsaDocument;
    }

    private Collection<NmlTopologyType> parseTopology(NmlNSAType nmlNsa, NsaType nsaDocument) {
        List<NmlTopologyType> topologies = nmlNsa.getTopology();
        topologies.stream().map((topology) -> {
            if (topology.getVersion() == null || !topology.getVersion().isValid()) {
                topology.setVersion(nsaDocument.getVersion());
            }
            return topology;
        }).filter((topology) -> (topology.getLifetime() == null || topology.getLifetime().getEnd() == null || !topology.getLifetime().getEnd().isValid())).forEach((topology) -> {
            NmlLifeTimeType lifetime = nmlFactory.createNmlLifeTimeType();
            lifetime.setEnd(nsaDocument.getExpires());
            topology.setLifetime(lifetime);
        });
        return topologies;
    }

    private Collection<PeersWithType> parsePeersWith(List<NmlNSARelationType> relationList) {
        List<PeersWithType> peersWithList = new ArrayList<>();
        relationList.stream().filter((relation) -> (NsiConstants.NML_PEERSWITH_RELATION.equalsIgnoreCase(relation.getType()))).forEach((relation) -> {
            relation.getNSA().stream().map((nsa) -> {
                PeersWithType peersWith = nsaFactory.createPeersWithType();
                peersWith.setRole(PeerRoleEnum.PA);
                peersWith.setValue(nsa.getId().trim());
                return peersWith;
            }).forEach((peersWith) -> {
                peersWithList.add(peersWith);
            });
        });

        return peersWithList;
    }

    private List<InterfaceType> parseService(List<NmlServiceType> services) {
        List<InterfaceType> interfaceList = new ArrayList<>();
        services.stream().map((service) -> {
            InterfaceType aInterface = nsaFactory.createInterfaceType();
            aInterface.setHref(service.getLink().trim());
            return aInterface;
        }).map((aInterface) -> {
            aInterface.setType(NsiConstants.NSI_CS_PROVIDER_V2);
            return aInterface;
        }).forEach((aInterface) -> {
            interfaceList.add(aInterface);
        });

        return interfaceList;
    }
}
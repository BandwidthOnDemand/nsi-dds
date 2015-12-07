/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.schema;

import javax.xml.bind.JAXBException;
import net.es.nsi.dds.jaxb.NmlParser;
import net.es.nsi.dds.jaxb.NsaParser;
import net.es.nsi.dds.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.jaxb.nsa.NsaType;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class JaxbTest {
    private final static net.es.nsi.dds.jaxb.dds.ObjectFactory ddsFactory = new net.es.nsi.dds.jaxb.dds.ObjectFactory();
    private final static net.es.nsi.dds.jaxb.nsa.ObjectFactory nsaFactory = new net.es.nsi.dds.jaxb.nsa.ObjectFactory();

    @Test
    public void xml2NsaJaxb() throws JAXBException {
        final String nsa = "<ns5:nsa xmlns:nml=\"http://schemas.ogf.org/nml/2013/05/base#\" \n" +
"    xmlns:vc=\"urn:ietf:params:xml:ns:vcard-4.0\" \n" +
"    xmlns:ns4=\"http://schemas.ogf.org/nsi/2013/12/services/definition\" \n" +
"    xmlns:ns5=\"http://schemas.ogf.org/nsi/2014/02/discovery/nsa\"\n" +
"    xmlns:nsi=\"http://schemas.ogf.org/nsi/2013/09/topology#\" \n" +
"    id=\"urn:ogf:network:geant.net:2013:nsa\" \n" +
"    version=\"2015-02-06T13:18:33.922Z\">\n" +
"    <name>geant.net</name>\n" +
"    <softwareVersion>AutoBAHN 3.1</softwareVersion>\n" +
"    <adminContact>\n" +
"        <vc:vcard>\n" +
"            <vc:kind>\n" +
"                <vc:text>org</vc:text>\n" +
"            </vc:kind>\n" +
"            <vc:fn>\n" +
"                <vc:text>GEANT</vc:text>\n" +
"            </vc:fn>\n" +
"            <vc:email>\n" +
"                <vc:text>mdsd@geant.net</vc:text>\n" +
"            </vc:email>\n" +
"        </vc:vcard>\n" +
"    </adminContact>\n" +
"    <location>\n" +
"        <longitude>8.24</longitude>\n" +
"        <latitude>49.1</latitude>\n" +
"    </location>\n" +
"    <networkId>urn:ogf:network:geant.net:2013:topology</networkId>\n" +
"    <networkId>urn:ogf:network:heanet.ie:2013:topology</networkId>\n" +
"    <networkId>urn:ogf:network:ja.net:2013:topology</networkId>\n" +
"    <networkId>urn:ogf:network:pionier.net.pl:2013:topology</networkId>\n" +
"    <networkId>urn:ogf:network:funet.fi:2013:topology</networkId>\n" +
"    <networkId>urn:ogf:network:grnet.gr:2013:topology</networkId>\n" +
"    <networkId>urn:ogf:network:dfn.de:2013:topology</networkId>\n" +
"    <networkId>urn:ogf:network:deic.dk:2013:topology</networkId>\n" +
"    <interface>\n" +
"        <type>application/vnd.ogf.nsi.cs.v2.provider+soap</type>\n" +
"        <href>https://prod-bod.geant.net:8091/nsi/ConnectionProvider</href>\n" +
"    </interface>\n" +
"    <interface>\n" +
"        <type>application/vnd.ogf.nsi.topology.v2+xml</type>\n" +
"        <href>http://bodportal.geant.net:8080/autobahn-ts/export/network/urn:ogf:network:geant.net:2013:topology</href>\n" +
"    </interface>\n" +
"    <interface>\n" +
"        <type>application/vnd.ogf.nsi.topology.v2+xml</type>\n" +
"        <href>http://bodportal.geant.net:8080/autobahn-ts/export/network/urn:ogf:network:heanet.ie:2013:topology</href>\n" +
"    </interface>\n" +
"    <interface>\n" +
"        <type>application/vnd.ogf.nsi.topology.v2+xml</type>\n" +
"        <href>http://bodportal.geant.net:8080/autobahn-ts/export/network/urn:ogf:network:ja.net:2013:topology</href>\n" +
"    </interface>\n" +
"    <interface>\n" +
"        <type>application/vnd.ogf.nsi.topology.v2+xml</type>\n" +
"        <href>http://bodportal.geant.net:8080/autobahn-ts/export/network/urn:ogf:network:pionier.net.pl:2013:topology</href>\n" +
"    </interface>\n" +
"    <interface>\n" +
"        <type>application/vnd.ogf.nsi.topology.v2+xml</type>\n" +
"        <href>http://bodportal.geant.net:8080/autobahn-ts/export/network/urn:ogf:network:funet.fi:2013:topology</href>\n" +
"    </interface>\n" +
"    <interface>\n" +
"        <type>application/vnd.ogf.nsi.topology.v2+xml</type>\n" +
"        <href>http://bodportal.geant.net:8080/autobahn-ts/export/network/urn:ogf:network:grnet.gr:2013:topology</href>\n" +
"    </interface>\n" +
"    <interface>\n" +
"        <type>application/vnd.ogf.nsi.topology.v2+xml</type>\n" +
"        <href>http://bodportal.geant.net:8080/autobahn-ts/export/network/urn:ogf:network:dfn.de:2013:topology</href>\n" +
"    </interface>\n" +
"    <interface>\n" +
"        <type>application/vnd.ogf.nsi.topology.v2+xml</type>\n" +
"        <href>http://bodportal.geant.net:8080/autobahn-ts/export/network/urn:ogf:network:deic.dk:2013:topology</href>\n" +
"    </interface>\n" +
"    <interface>\n" +
"        <type>application/vnd.ogf.nsi.discovery.v1+xml</type>\n" +
"        <href>http://bodportal.geant.net:8080/autobahn-ts/export/nsa/urn:ogf:network:geant.net:2013:nsa</href>\n" +
"    </interface>\n" +
"    <feature type=\"vnd.ogf.nsi.cs.v2.role.uPA\"/>\n" +
"    <feature type=\"vnd.ogf.nsi.cs.v2.role.aggregator\"/>\n" +
"    <peersWith>urn:ogf:network:es.net:2013:nsa:nsi-aggr-west</peersWith>\n" +
"    <peersWith>urn:ogf:network:nordu.net:2013:nsa</peersWith>\n" +
"    <peersWith>urn:ogf:network:surfnet.nl:1990:nsa:safnari</peersWith>\n" +
"    <other>\n" +
"        <gns:TopologyReachability xmlns:gns=\"http://nordu.net/namespaces/2013/12/gnsbod\">\n" +
"            <Topology cost=\"1\" id=\"urn:ogf:network:manlan.internet2.edu:2013\"/>\n" +
"            <Topology cost=\"1\" id=\"urn:ogf:network:heanet.ie:2013:topology\"/>\n" +
"            <Topology cost=\"1\" id=\"urn:ogf:network:ja.net:2013:topology\"/>\n" +
"            <Topology cost=\"1\" id=\"urn:ogf:network:funet.fi:2013:topology\"/>\n" +
"            <Topology cost=\"1\" id=\"urn:ogf:network:nordu.net:2013:topology\"/>\n" +
"            <Topology cost=\"1\" id=\"urn:ogf:network:grnet.gr:2013:topology\"/>\n" +
"            <Topology cost=\"1\" id=\"urn:ogf:network:surfnet.nl:1990:production7\"/>\n" +
"            <Topology cost=\"1\" id=\"urn:ogf:network:deic.dk:2013:topology\"/>\n" +
"            <Topology cost=\"1\" id=\"urn:ogf:network:pionier.net.pl:2013:topology\"/>\n" +
"            <Topology cost=\"2\" id=\"urn:ogf:network:es.net:2013\"/>\n" +
"        </gns:TopologyReachability>\n" +
"    </other>\n" +
"</ns5:nsa>";

        NsaType xml2Jaxb = NsaParser.getInstance().xml2Jaxb(NsaType.class, nsa);
        System.out.println("nsaId=" + xml2Jaxb.getId());
        assertEquals("urn:ogf:network:geant.net:2013:nsa", xml2Jaxb.getId());
    }

    @Test
    public void xml2TopologyJaxb() throws JAXBException {
        final String nml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
"<nml:Topology xmlns:nml=\"http://schemas.ogf.org/nml/2013/05/base#\" xmlns:ns3=\"http://schemas.ogf.org/nsi/2013/12/services/definition\" xmlns:nsi=\"http://schemas.ogf.org/nsi/2013/09/topology#\" xmlns:vc=\"urn:ietf:params:xml:ns:vcard-4.0\" id=\"urn:ogf:network:ja.net:2013:topology\" version=\"2015-12-02T05:26:58.087Z\">\n" +
"    <nml:name>ja.net</nml:name>\n" +
"    <nml:BidirectionalPort id=\"urn:ogf:network:ja.net:2013:topology:bonfire-1\">\n" +
"        <nml:name>bonfire-1</nml:name>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:bonfire-1-in\"/>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:bonfire-1-out\"/>\n" +
"    </nml:BidirectionalPort>\n" +
"    <nml:BidirectionalPort id=\"urn:ogf:network:ja.net:2013:topology:caliban-ethfib\">\n" +
"        <nml:name>caliban-ethfib</nml:name>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:caliban-ethfib-in\"/>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:caliban-ethfib-out\"/>\n" +
"    </nml:BidirectionalPort>\n" +
"    <nml:BidirectionalPort id=\"urn:ogf:network:ja.net:2013:topology:p-to-janet\">\n" +
"        <nml:name>p-to-janet</nml:name>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:p-to-janet-in\"/>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:p-to-janet-out\"/>\n" +
"    </nml:BidirectionalPort>\n" +
"    <nml:BidirectionalPort id=\"urn:ogf:network:ja.net:2013:topology:ganymede-ethfib\">\n" +
"        <nml:name>ganymede-ethfib</nml:name>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ganymede-ethfib-in\"/>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ganymede-ethfib-out\"/>\n" +
"    </nml:BidirectionalPort>\n" +
"    <nml:BidirectionalPort id=\"urn:ogf:network:ja.net:2013:topology:ge-1__0__1\">\n" +
"        <nml:name>ge-1__0__1</nml:name>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ge-1__0__1-in\"/>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ge-1__0__1-out\"/>\n" +
"    </nml:BidirectionalPort>\n" +
"    <nml:BidirectionalPort id=\"urn:ogf:network:ja.net:2013:topology:ge-1__1__5\">\n" +
"        <nml:name>ge-1__1__5</nml:name>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ge-1__1__5-in\"/>\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ge-1__1__5-out\"/>\n" +
"    </nml:BidirectionalPort>\n" +
"    <ns3:serviceDefinition id=\"urn:ogf:network:ja.net:2013:topologyServiceDefinition:EVTS.A-GOLE\">\n" +
"        <name>GLIF Automated GOLE Ethernet VLAN Transfer Service</name>\n" +
"        <serviceType>http://services.ogf.org/nsi/2013/07/definitions/EVTS.A-GOLE</serviceType>\n" +
"    </ns3:serviceDefinition>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:bonfire-1-out\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">940-951</nml:LabelGroup>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasInboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:bonfire-1-in\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">940-951</nml:LabelGroup>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:caliban-ethfib-out\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2003-2020</nml:LabelGroup>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasInboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:caliban-ethfib-in\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2003-2020</nml:LabelGroup>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:p-to-janet-out\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2003-2020</nml:LabelGroup>\n" +
"            <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#isAlias\">\n" +
"                <nml:PortGroup id=\"urn:ogf:network:geant.net:2013:topology:p-to-geant-in\"/>\n" +
"            </nml:Relation>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasInboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:p-to-janet-in\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2003-2020</nml:LabelGroup>\n" +
"            <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#isAlias\">\n" +
"                <nml:PortGroup id=\"urn:ogf:network:geant.net:2013:topology:p-to-geant-out\"/>\n" +
"            </nml:Relation>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ganymede-ethfib-out\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2003-2020</nml:LabelGroup>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasInboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ganymede-ethfib-in\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2003-2020</nml:LabelGroup>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ge-1__0__1-out\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2003-2022</nml:LabelGroup>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasInboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ge-1__0__1-in\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2003-2022</nml:LabelGroup>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ge-1__1__5-out\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2003-2022</nml:LabelGroup>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasInboundPort\">\n" +
"        <nml:PortGroup id=\"urn:ogf:network:ja.net:2013:topology:ge-1__1__5-in\">\n" +
"            <nml:LabelGroup labeltype=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\">2003-2022</nml:LabelGroup>\n" +
"        </nml:PortGroup>\n" +
"    </nml:Relation>\n" +
"    <nml:Relation type=\"http://schemas.ogf.org/nml/2013/05/base#hasService\">\n" +
"        <nml:SwitchingService labelSwapping=\"true\" labelType=\"http://schemas.ogf.org/nml/2012/10/ethernet#vlan\" id=\"urn:ogf:network:ja.net:2013:topologyServiceDomain:a-gole:testbed:A-GOLE-EVTS\">\n" +
"            <ns3:serviceDefinition id=\"urn:ogf:network:ja.net:2013:topologyServiceDefinition:EVTS.A-GOLE\">\n" +
"                <name>GLIF Automated GOLE Ethernet VLAN Transfer Service</name>\n" +
"                <serviceType>http://services.ogf.org/nsi/2013/07/definitions/EVTS.A-GOLE</serviceType>\n" +
"            </ns3:serviceDefinition>\n" +
"        </nml:SwitchingService>\n" +
"    </nml:Relation>\n" +
"</nml:Topology>";

        NmlTopologyType xml2Jaxb = NmlParser.getInstance().xml2Jaxb(NmlTopologyType.class, nml);
        System.out.println("topologyId=" + xml2Jaxb.getId());
        assertEquals("urn:ogf:network:ja.net:2013:topology", xml2Jaxb.getId());
    }
}

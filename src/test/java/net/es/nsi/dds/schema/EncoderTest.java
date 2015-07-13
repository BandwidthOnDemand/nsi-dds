/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.schema;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import net.es.nsi.dds.api.jaxb.AnyType;
import net.es.nsi.dds.api.jaxb.DocumentType;
import net.es.nsi.dds.api.jaxb.NsaType;
import net.es.nsi.dds.api.jaxb.ObjectFactory;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class EncoderTest {
    private final static ObjectFactory factory = new ObjectFactory();

    @Test
    public void encodeNsaDocument() throws DatatypeConfigurationException {
        NsaType nsa = factory.createNsaType();
        nsa.setId("urn:ogf:network:example.com:2013:nsa:vixen");
        nsa.setVersion(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis()));
        nsa.setExpires(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + 100000L));
        nsa.setName("Example NSA");
        nsa.setSoftwareVersion("ExampleNsa-Version-1.0");
        nsa.setStartTime(nsa.getVersion());
        nsa.getNetworkId().add("urn:ogf:network:example.com:2013:network:theworkshop");

        AnyType any = factory.createAnyType();
        any.getAny().add(factory.createNsa(nsa));

        DocumentType document = factory.createDocumentType();
        document.setId("urn:ogf:network:example.com:2013:nsa:vixen");
        document.setVersion(nsa.getVersion());
        document.setExpires(nsa.getExpires());
        document.setNsa("urn:ogf:network:example.com:2013:nsa:vixen");
        document.setType("vnd.ogf.nsi.nsa.v1+xml");
        document.setContent(any);

        String jaxbToString = XmlUtilities.jaxbToString(DocumentType.class, factory.createDocument(document));
        System.out.println(jaxbToString);
    }

    @Test
    public void encodeSimpleDocument() throws DatatypeConfigurationException {
        DocumentType document = factory.createDocumentType();
        document.setId("urn:ogf:network:example.com:2013:nsa:vixen:status");
        document.setVersion(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis()));
        document.setExpires(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + 100000L));
        document.setNsa("urn:ogf:network:example.com:2013:nsa:vixen");
        document.setType("vnd.ogf.nsi.nsa.status.v1+xml");
        JAXBElement<Object> createValue = factory.createValue("ACTIVE");
        AnyType any2 = factory.createAnyType();
        any2.getAny().add(createValue);
        document.setContent(any2);

        String jaxbToString = XmlUtilities.jaxbToString(DocumentType.class, factory.createDocument(document));
        System.out.println(jaxbToString);
    }
}

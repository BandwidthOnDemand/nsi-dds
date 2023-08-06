package net.es.nsi.dds.schema;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Optional;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.NsaParser;
import net.es.nsi.dds.jaxb.dds.ContentType;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.nsa.NsaType;
import net.es.nsi.dds.lib.DocumentBuilder;
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.util.XmlUtilities;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class EncoderTest {

  private final static net.es.nsi.dds.jaxb.dds.ObjectFactory ddsFactory = new net.es.nsi.dds.jaxb.dds.ObjectFactory();
  private final static net.es.nsi.dds.jaxb.nsa.ObjectFactory nsaFactory = new net.es.nsi.dds.jaxb.nsa.ObjectFactory();

  @Test
  public void encodeNsaDocument() throws DatatypeConfigurationException, IllegalArgumentException, IOException, JAXBException, ParserConfigurationException {
    log.debug("encodeNsaDocument");
    NsaType nsa = nsaFactory.createNsaType();
    nsa.setId("urn:ogf:network:example.com:2013:nsa:vixen");
    nsa.setVersion(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis()));
    nsa.setExpires(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + 100000L));
    nsa.setName("Example NSA");
    nsa.setSoftwareVersion("ExampleNsa-Version-1.0");
    nsa.setStartTime(nsa.getVersion());
    nsa.getNetworkId().add("urn:ogf:network:example.com:2013:network:theworkshop");

    // Convert the JAXB NSA description document to DOM format.
    Optional<org.w3c.dom.Document> doc = Optional.of(NsaParser.getInstance().jaxb2Dom(nsaFactory.createNsa(nsa)));

    // Build the DDS document to add.
    DocumentBuilder dBuilder = new DocumentBuilder()
            .withNsaId(nsa.getId())
            .withType(NsiConstants.NSI_DOC_TYPE_NSA_V1)
            .withId(nsa.getId())
            .withVersion(nsa.getVersion())
            .withExpires(nsa.getExpires())
            .withContents(doc.get());

    // Create the document we want to add to DDS.
    String jaxbToString = DdsParser.getInstance().jaxb2Xml(ddsFactory.createDocument(dBuilder.build()));
    log.debug(jaxbToString);
  }

  @Test
  public void encodeSimpleDocument() throws DatatypeConfigurationException, JAXBException {
    log.debug("encodeSimpleDocument");
    DocumentType document = ddsFactory.createDocumentType();
    document.setId("urn:ogf:network:example.com:2013:nsa:vixen:status");
    document.setVersion(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis()));
    document.setExpires(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + 100000L));
    document.setNsa("urn:ogf:network:example.com:2013:nsa:vixen");
    document.setType("vnd.ogf.nsi.nsa.status.v1+xml");
    ContentType contentHolder = ddsFactory.createContentType();
    contentHolder.setValue("ACTIVE");
    contentHolder.setContentType("text/plain");
    contentHolder.setContentTransferEncoding("7bit");
    document.setContent(contentHolder);

    String jaxbToString = DdsParser.getInstance().jaxb2Xml(ddsFactory.createDocument(document));
    log.debug(jaxbToString);
  }
}

package net.es.nsi.dds.lib;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Date;
import java.util.Optional;
import javax.xml.bind.JAXBException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.jaxb.NmlParser;
import net.es.nsi.dds.jaxb.NsaParser;
import net.es.nsi.dds.jaxb.dds.DocumentEventType;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.NotificationType;
import net.es.nsi.dds.jaxb.nml.NmlLifeTimeType;
import net.es.nsi.dds.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.jaxb.nsa.NsaType;
import net.es.nsi.dds.provider.DdsProvider;
import net.es.nsi.dds.signing.SignatureFactory;
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.util.XmlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class DocHelper {
    private final static Logger log = LoggerFactory.getLogger(DocHelper.class);
    private final static net.es.nsi.dds.jaxb.dds.ObjectFactory ddsFactory = new net.es.nsi.dds.jaxb.dds.ObjectFactory();
    private final static net.es.nsi.dds.jaxb.nsa.ObjectFactory nsaFactory = new net.es.nsi.dds.jaxb.nsa.ObjectFactory();
    private final static net.es.nsi.dds.jaxb.nml.ObjectFactory nmlFactory = new net.es.nsi.dds.jaxb.nml.ObjectFactory();

    public static boolean addNsaDocument(NsaType nsa, XMLGregorianCalendar discovered) {
        log.debug("addNsaDocument: adding nsaId=" + nsa.getId());

        // If there is no expires time specified then it is infinite.
        if (nsa.getExpires() == null || !nsa.getExpires().isValid()) {
            // No expire value provided so make one.
            Date date = new Date(System.currentTimeMillis() + XmlUtilities.ONE_YEAR);
            XMLGregorianCalendar xmlGregorianCalendar;
            try {
                xmlGregorianCalendar = XmlUtilities.xmlGregorianCalendar(date);
            } catch (DatatypeConfigurationException ex) {
                log.error("addNsaDocument: NSA document does not contain an expires date, id={}", nsa.getId());
                return false;
            }

            nsa.setExpires(xmlGregorianCalendar);
        }

        // Convert the JAXB NSA description document to DOM format.
        Optional<org.w3c.dom.Document> doc;
        try {
            // Convert JAXB representation to DOM for signing and encoding.
            doc = Optional.of(NsaParser.getInstance().jaxb2Dom(nsaFactory.createNsa(nsa)));
        } catch (NullPointerException | JAXBException | ParserConfigurationException ex) {
            log.error("addNsaDocument: invalid NSA document", ex);
            throw new IllegalArgumentException(ex);
        }

        // Build the DDS document to add.
        DocumentBuilder dBuilder = new DocumentBuilder()
                .withNsaId(nsa.getId())
                .withType(NsiConstants.NSI_DOC_TYPE_NSA_V1)
                .withId(nsa.getId())
                .withVersion(nsa.getVersion())
                .withExpires(nsa.getExpires())
                .withContents(doc.get());

        // Generate document signature and add to DDS document if required.
        DdsConfiguration config = DdsConfiguration.getInstance();
        log.debug("addNsaDocument: isSign=" + config.isSign());
        if (config.isSign()) {
            Optional<org.w3c.dom.Document> sig = Optional.empty();
            try {
                SignatureFactory signatureFactory = new SignatureFactory(config.getSigningStore());
                sig = Optional.of(signatureFactory.generateExternalSignature(doc.get(), config.getSigningAlias()));
            } catch (XMLSignatureException | KeyStoreException | RuntimeException ex) {
                log.error("build: unable to create signature document", ex);
                throw new IllegalArgumentException(ex);
            }

            dBuilder.withSignature(sig.get());
        }

        // Generate the document we want to add to DDS.
        DocumentType document;
        try {
            document = dBuilder.build();
        } catch (IllegalArgumentException | IOException ex) {
            log.error("addNsaDocument: Could not create DDS document for NSA id={}", nsa.getId());
            return false;
        }

        // Try to add the document as a notification of document change.
        NotificationType notify = ddsFactory.createNotificationType();
        notify.setEvent(DocumentEventType.ALL);
        notify.setDiscovered(discovered);
        notify.setDocument(document);

        DdsProvider.getInstance().processNotification(notify);

        return true;
    }

    public static boolean addTopologyDocument(NmlTopologyType topology, XMLGregorianCalendar discovered, String nsaId) {
        // If there is no expires time specified then it is infinite.
        if (topology.getLifetime() == null || topology.getLifetime().getEnd() == null || !topology.getLifetime().getEnd().isValid()) {
            // No expire value provided so make one.
            Date date = new Date(System.currentTimeMillis() + XmlUtilities.ONE_YEAR);
            XMLGregorianCalendar xmlGregorianCalendar;
            try {
                xmlGregorianCalendar = XmlUtilities.xmlGregorianCalendar(date);
            } catch (DatatypeConfigurationException ex) {
                log.error("addTopologyDocument: Topology document does not contain an expires date, id={}", topology.getId());
                return false;
            }

            NmlLifeTimeType lifetime = nmlFactory.createNmlLifeTimeType();
            lifetime.setStart(topology.getVersion());
            lifetime.setEnd(xmlGregorianCalendar);
            topology.setLifetime(lifetime);
        }

        // Convert the JAXB NML Topology document to DOM format.
        Optional<org.w3c.dom.Document> doc;
        try {
            doc = Optional.of(NmlParser.getInstance().jaxb2Dom(nmlFactory.createTopology(topology)));
        } catch (NullPointerException | JAXBException | ParserConfigurationException ex) {
            log.error("addTopologyDocument: invalid NML topology document", ex);
            throw new IllegalArgumentException(ex);
        }

        // Build the DDS document to add.
        DocumentBuilder dBuilder = new DocumentBuilder()
                .withNsaId(nsaId)
                .withType(NsiConstants.NSI_DOC_TYPE_TOPOLOGY_V2)
                .withId(topology.getId())
                .withVersion(topology.getVersion())
                .withExpires(topology.getLifetime().getEnd())
                .withContents(doc.get());

        // Generate document signature and add to DDS document if required.
        DdsConfiguration config = DdsConfiguration.getInstance();
        if (config.isSign()) {
            Optional<org.w3c.dom.Document> sig = Optional.empty();
            try {
                SignatureFactory signatureFactory = new SignatureFactory(config.getSigningStore());
                sig = Optional.of(signatureFactory.generateExternalSignature(doc.get(), config.getSigningAlias()));
            } catch (XMLSignatureException | KeyStoreException | RuntimeException ex) {
                log.error("build: unable to create signature document", ex);
                throw new IllegalArgumentException(ex);
            }

            dBuilder.withSignature(sig.get());
        }

        // Generate the document we want to add to DDS.
        DocumentType document;
        try {
            document = dBuilder.build();
        } catch (IllegalArgumentException | IOException ex) {
            log.error("addTopologyDocument: Could not create DDS document for topology id={}", topology.getId(), ex);
            return false;
        }

        // Try to add the document as a notification of document change.
        NotificationType notify = ddsFactory.createNotificationType();
        notify.setEvent(DocumentEventType.ALL);
        notify.setDiscovered(discovered);
        notify.setDocument(document);

        DdsProvider.getInstance().processNotification(notify);

        return true;
    }
}

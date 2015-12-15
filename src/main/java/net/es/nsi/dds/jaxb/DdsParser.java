package net.es.nsi.dds.jaxb;

import java.io.IOException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.jaxb.dds.CollectionType;
import net.es.nsi.dds.jaxb.dds.DocumentListType;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class DdsParser extends JaxbParser {
    private static final String PACKAGES = "net.es.nsi.dds.jaxb.dds";
    private static final ObjectFactory factory = new ObjectFactory();

    private DdsParser() {
        super(PACKAGES);
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ParserHolder {
        public static final DdsParser INSTANCE = new DdsParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An object of the NmwgParser.
     */
    public static DdsParser getInstance() {
            return ParserHolder.INSTANCE;
    }

    public DocumentType readDocument(String filename) throws JAXBException, IOException {
        return this.parseFile(DocumentType.class, filename);
    }

    public void writeDocument(String file, DocumentType document) throws JAXBException, IOException {
        // Parse the specified file.
        JAXBElement<DocumentType> element = factory.createDocument(document);
        this.writeFile(element, file);
    }

    public CollectionType readCollection(String filename) throws JAXBException, IOException {
        return this.parseFile(CollectionType.class, filename);
    }

    public String xmlFormatter(DocumentType document) throws JAXBException {
        return this.jaxb2XmlFormatter(factory.createDocument(document));
    }

    public String xmlFormatter(DocumentListType documents) throws JAXBException {
        return this.jaxb2XmlFormatter(factory.createDocuments(documents));
    }
}

package net.es.nsi.dds.jaxb;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import net.es.nsi.dds.jaxb.management.ObjectFactory;
import net.es.nsi.dds.jaxb.management.ResourceListType;
import net.es.nsi.dds.jaxb.management.VersionType;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class ManagementParser extends JaxbParser {
    private static final String PACKAGES = "net.es.nsi.dds.jaxb.management";
    private static final ObjectFactory factory = new ObjectFactory();

    private ManagementParser() {
        super(PACKAGES);
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ParserHolder {
        public static final ManagementParser INSTANCE = new ManagementParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An object of the NmwgParser.
     */
    public static ManagementParser getInstance() {
            return ParserHolder.INSTANCE;
    }

    public String version2Xml(VersionType version) throws JAXBException, IOException {
        JAXBElement<VersionType> jaxb = factory.createVersion(version);
        return this.jaxb2Xml(jaxb);
    }

    public VersionType xml2Version(String input) throws JAXBException, IllegalArgumentException {
        return this.xml2Jaxb(VersionType.class, input);
    }

    public String xmlFormatter(VersionType version) throws JAXBException {
        return this.jaxb2XmlFormatter(factory.createVersion(version));
    }

    public String xmlFormatter(ResourceListType list) throws JAXBException {
        return this.jaxb2XmlFormatter(factory.createResources(list));
    }
}

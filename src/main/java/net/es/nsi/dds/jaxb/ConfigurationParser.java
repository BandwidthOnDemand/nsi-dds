package net.es.nsi.dds.jaxb;

import java.io.IOException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.jaxb.configuration.DdsConfigurationType;
import net.es.nsi.dds.jaxb.configuration.ObjectFactory;

/**
 * A singleton to load the very expensive JAXBContext once.
 *
 * @author hacksaw
 */
public class ConfigurationParser extends JaxbParser {
    private static final String PACKAGES = "net.es.nsi.dds.jaxb.configuration";
    private static final ObjectFactory factory = new ObjectFactory();

    private ConfigurationParser() {
        super(PACKAGES);
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ParserHolder {
        public static final ConfigurationParser INSTANCE = new ConfigurationParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An NmlParser object of the NSAType.
     */
    public static ConfigurationParser getInstance() {
            return ParserHolder.INSTANCE;
    }

    public DdsConfigurationType readConfiguration(String filename) throws JAXBException, IOException {
        return getInstance().parseFile(DdsConfigurationType.class, filename);
    }

    public void writeConfiguration(String file, DdsConfigurationType config) throws JAXBException, IOException {
        // Parse the specified file.
        JAXBElement<DdsConfigurationType> element = factory.createDds(config);
        getInstance().writeFile(element, file);
    }
}

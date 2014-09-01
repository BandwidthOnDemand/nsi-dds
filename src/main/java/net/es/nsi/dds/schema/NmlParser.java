package net.es.nsi.dds.schema;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.api.jaxb.NmlNSAType;
import net.es.nsi.dds.api.jaxb.NmlTopologyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class loads an NML XML based NSA object from a specified file.  This is
 * a singleton class that optimizes loading of a JAXB parser instance that may
 * take an extremely long time (on the order of 10 seconds).
 *
 * @author hacksaw
 */
public class NmlParser {
    // Get a logger just in case we encounter a problem.
    private final Logger log = LoggerFactory.getLogger(getClass());

    // The JAXB context we load pre-loading in this singleton.
    private static JAXBContext jaxbContextNSA = null;
    private static JAXBContext jaxbContextTopology = null;

    /**
     * Private constructor loads the JAXB context once and prevents
     * instantiation from other classes.
     */
    private NmlParser() {
        try {
            // Load a JAXB context for the NML NSAType parser.
            jaxbContextNSA = JAXBContext.newInstance("net.es.nsi.dds.api.jaxb", net.es.nsi.dds.api.jaxb.ObjectFactory.class.getClassLoader());
            jaxbContextTopology = JAXBContext.newInstance(NmlTopologyType.class);
        }
        catch (JAXBException jaxb) {
            log.error("NmlParser: Failed to load JAXB instance", jaxb);
        }
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class NmlParserHolder {
        public static final NmlParser INSTANCE = new NmlParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An NmlParser object of the NSAType.
     */
    public static NmlParser getInstance() {
            return NmlParserHolder.INSTANCE;
    }

    public void init() {
        log.debug("NmlParser: initializing...");
    }

    /**
     * Parse an NML NSA object from the specified file.
     *
     * @param file File containing the XML formated NSA object.
     * @return A JAXB compiled NSAType object.
     * @throws JAXBException If the XML contained in the file is not valid.
     * @throws FileNotFoundException If the specified file was not found.
     */
    @SuppressWarnings("unchecked")
    public NmlNSAType parseNSAFromFile(String file) throws JAXBException, IOException {
        // Make sure we initialized properly.
        if (jaxbContextNSA == null) {
            throw new JAXBException("NmlParser: Failed to load JAXB instance");
        }

        // Parse the specified file.
        JAXBElement<NmlNSAType> nsaElement;
        try (FileInputStream fileInputStream = new FileInputStream(file); BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
            nsaElement = (JAXBElement<NmlNSAType>) jaxbContextNSA.createUnmarshaller().unmarshal(bufferedInputStream);
        }

        // Return the NSAType object.
        return nsaElement.getValue();
    }

    /**
     * Parse an NML Topology object from the specified string.
     *
     * @param xml String containing the XML formated Topology object.
     * @return A JAXB compiled TopologyType object.
     * @throws JAXBException If the XML contained in the string is not valid.
     * @throws JAXBException If the XML is not well formed.
     */
    @SuppressWarnings("unchecked")
    public NmlTopologyType parseTopologyFromString(String xml) throws JAXBException {
        // Make sure we initialized properly.
        if (jaxbContextTopology == null) {
            throw new JAXBException("NmlParser: Failed to load JAXB Topology instance");
        }

        JAXBElement<NmlTopologyType> topologyElement;
        try (StringReader reader = new StringReader(xml)) {
            topologyElement = (JAXBElement<NmlTopologyType>) jaxbContextNSA.createUnmarshaller().unmarshal(reader);
        }

        // Return the NSAType object.
        return topologyElement.getValue();
    }

    /**
     * Parse an NML NSA object from the specified string.
     *
     * @param xml String containing the XML formated NSA object.
     * @return A JAXB compiled NSAType object.
     * @throws JAXBException If the XML contained in the string is not valid.
     * @throws JAXBException If the XML is not well formed.
     */
    public NmlNSAType parseNsaFromString(String xml) throws JAXBException {
        // Make sure we initialized properly.
        if (jaxbContextNSA == null) {
            throw new JAXBException("NmlParser: Failed to load JAXB NSA instance");
        }

        // Parse the specified XML string.
        StringReader reader = new StringReader(xml);

        @SuppressWarnings("unchecked")
        JAXBElement<NmlNSAType> nsaElement = (JAXBElement<NmlNSAType>) jaxbContextNSA.createUnmarshaller().unmarshal(reader);

        // Return the NSAType object.
        return nsaElement.getValue();
    }
}
package net.es.nsi.dds.util;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;


/**
 * This {@link XmlUtilities} is a utility class providing tools for the
 * manipulation of JAXB generated XML objects.
 *
 * @author hacksaw
 */
@Slf4j
public class XmlUtilities {
    public final static long ONE_YEAR = 31536000000L;
    public final static long ONE_DAY = 86400000L;

	/**
	 * Utility method to marshal a JAXB annotated java object to an XML
         * formatted string.  This class is generic enough to be used for any
         * JAXB annotated java object not containing the {@link XmlRootElement}
         * annotation.
         *
	 * @param xmlClass	The JAXB class of the object to marshal.
	 * @param xmlObject	The JAXB object to marshal.
	 * @return		String containing the XML encoded object.
	 */
	public static String jaxbToXml(Class<?> xmlClass, Object xmlObject) {

            // Make sure we are given the correct input.
            if (xmlClass == null || xmlObject == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            JAXBElement<?> jaxbElement = new JAXBElement(new QName("uri", "local"), xmlClass, xmlObject);

            return jaxbToXml(xmlClass, jaxbElement);
	}

    public static String jaxbToXml(Class<?> xmlClass, JAXBElement<?> jaxbElement) {

            // Make sure we are given the correct input.
            if (xmlClass == null || jaxbElement == null) {
                return null;
            }

            // We will write the XML encoding into a string.
            StringWriter writer = new StringWriter();
            String result;
            try {
                // We will use JAXB to marshal the java objects.
                final JAXBContext jaxbContext = JAXBContext.newInstance(xmlClass);

                // Marshal the object.
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                jaxbMarshaller.marshal(jaxbElement, writer);
                result = writer.toString();
            } catch (JAXBException e) {
                // Something went wrong so get out of here.
                return null;
            }
            finally {
                try { writer.close(); } catch (IOException ignored) {}
            }

            // Return the XML string.
            return result;
	}

    public static Object xmlToJaxb(Class<?> xmlClass, String xml) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(xmlClass);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement<?> element;
        try (StringReader reader = new StringReader(xml)) {
            element = (JAXBElement<?>) unmarshaller.unmarshal(reader);
        }
        return element.getValue();
    }

    public static Object xmlToJaxb(Class<?> xmlClass, InputStream is) throws JAXBException, IOException {
        JAXBContext jaxbContext = JAXBContext.newInstance(xmlClass);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement<?> element = (JAXBElement<?>) unmarshaller.unmarshal(is);
        is.close();
        return element.getValue();
    }

    public static XMLGregorianCalendar longToXMLGregorianCalendar(long time) throws DatatypeConfigurationException {
        if (time <= 0) {
            return null;
        }

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }

    public static XMLGregorianCalendar xmlGregorianCalendar() {
      try {
        GregorianCalendar cal = new GregorianCalendar();
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
      } catch (DatatypeConfigurationException ex) {
        log.error("[XmlUtilities] xmlGregorianCalendar failed", ex);
        return null;
      }
    }

    public static XMLGregorianCalendar xmlGregorianCalendar(Date date) throws DatatypeConfigurationException {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }

    public static Date xmlGregorianCalendarToDate(XMLGregorianCalendar cal) throws DatatypeConfigurationException {
        GregorianCalendar gregorianCalendar = cal.toGregorianCalendar();
        return gregorianCalendar.getTime();
    }

    public static Collection<String> getXmlFilenames(String path) throws NullPointerException {
        Collection<String> results = new ArrayList<>();
        File folder = new File(path);

        // We will grab all XML files from the target directory.
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            String file;
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    file = listOfFiles[i].getAbsolutePath();
                    if (file.endsWith(".xml")) {
                        results.add(file);
                    }
                }
            }
        }
        return new CopyOnWriteArrayList<>(results);
    }
}

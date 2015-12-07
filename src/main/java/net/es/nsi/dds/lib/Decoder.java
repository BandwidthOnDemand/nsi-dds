/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.lib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import net.es.nsi.dds.jaxb.DomParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author hacksaw
 */
public class Decoder {
    private final static Logger log = LoggerFactory.getLogger(Decoder.class);

    public static Document decode(String source) throws IOException {
        byte[] encoded = Base64.getDecoder().decode(source);
        byte[] xml = decompress(encoded);

        try {
            Document doc = DomParser.xml2Dom(new ByteArrayInputStream(xml));
            return doc;
        }
        catch (ParserConfigurationException | SAXException ex) {
            log.error("decode: failed to parse document", ex);
            throw new IOException(ex.getMessage(), ex.getCause());
        }
    }

    private static byte[] decompress(byte[] source) throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(source)) {
            return gunzip(is);
        }
        catch (IOException io) {
            log.error("Failed to decompress document", io);
            throw io;
        }
    }

    private static byte[] gunzip(ByteArrayInputStream is) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(is)) {
            return IOUtils.toByteArray(gis);
        }
        catch (IOException io) {
            log.error("Failed to gunzip document", io);
            throw io;
        }
    }
}

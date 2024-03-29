package net.es.nsi.dds.lib;

import com.google.common.base.Strings;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.ParserConfigurationException;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.jaxb.DomParser;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Decode a document from the encoded representation.
 *
 * @author hacksaw
 */
@Slf4j
public class Decoder {
    public static InputStream decode(String contentTransferEncoding,
            String contentType, String source) throws IOException  {
        if (Strings.isNullOrEmpty(contentTransferEncoding)) {
            contentTransferEncoding = ContentTransferEncoding._7BIT;
        }

        if (Strings.isNullOrEmpty(contentType)) {
            contentType = ContentType.TEXT;
        }

        try (InputStream ctes = ContentTransferEncoding.decode(contentTransferEncoding, source)) {
            return ContentType.decode(contentType, ctes);
        } catch (UnsupportedEncodingException | MessagingException ex) {
            log.error("decode2Dom: failed to parse document", ex);
            throw new IOException(ex.getMessage(), ex.getCause());
        }
    }

    public static Document decode2Dom(String contentTransferEncoding,
            String contentType, String source) throws IOException  {

        try (InputStream dis = decode(contentTransferEncoding, contentType, source)) {
            return DomParser.xml2Dom(dis);
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            log.error("decode2Dom: failed to parse document", ex);
            throw new IOException(ex.getMessage(), ex.getCause());
        }
    }

    public static String decode2String(String contentTransferEncoding,
            String contentType, String source) throws IOException {

        try (InputStream dis = decode(contentTransferEncoding, contentType, source)) {
            return ContentType.decode2String(contentType, dis);
        }
    }

    public static byte[] decode2ByteArray(String contentTransferEncoding,
            String contentType, String source) throws IOException {

        try (InputStream dis = decode(contentTransferEncoding, contentType, source)) {
            return ContentType.decode2ByteArray(contentType, dis);
        }
    }
}

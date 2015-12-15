/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.lib;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.mail.MessagingException;
import javax.xml.parsers.ParserConfigurationException;
import net.es.nsi.dds.jaxb.DomParser;
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

    public static Document decode2Dom(String contentTransferEncoding,
            String contentType, String source) throws IOException, UnsupportedEncodingException, RuntimeException {
        if (Strings.isNullOrEmpty(contentTransferEncoding)) {
            contentTransferEncoding = ContentTransferEncoding._8BIT;
        }

        if (Strings.isNullOrEmpty(contentType)) {
            contentType = ContentType.TEXT;
        }

        try (InputStream ctis = ContentType.decode(contentType, ContentTransferEncoding.decode(contentTransferEncoding, source))) {
            return DomParser.xml2Dom(ctis);
        } catch (MessagingException ex) {
            throw new RuntimeException(ex);
        } catch (ParserConfigurationException | SAXException ex) {
            log.error("decode: failed to parse document", ex);
            throw new IOException(ex.getMessage(), ex.getCause());
        }
    }

    public static String decode2String(String contentTransferEncoding,
            String contentType, String source) throws IOException, UnsupportedEncodingException, RuntimeException {
        if (Strings.isNullOrEmpty(contentTransferEncoding)) {
            contentTransferEncoding = ContentTransferEncoding._7BIT;
        }

        if (Strings.isNullOrEmpty(contentType)) {
            contentType = ContentType.TEXT;
        }

        try (InputStream ctes = ContentTransferEncoding.decode(contentTransferEncoding, source)) {
            return ContentType.decode2String(contentType, ctes);
        } catch (MessagingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] decode2ByteArray(String contentTransferEncoding,
            String contentType, String source) throws IOException, UnsupportedEncodingException, RuntimeException {
        if (Strings.isNullOrEmpty(contentTransferEncoding)) {
            contentTransferEncoding = ContentTransferEncoding._7BIT;
        }

        if (Strings.isNullOrEmpty(contentType)) {
            contentType = ContentType.TEXT;
        }

        try (InputStream ctes = ContentTransferEncoding.decode(contentTransferEncoding, source)) {
            return ContentType.decode2ByteArray(contentType, ctes);
        } catch (MessagingException ex) {
            throw new RuntimeException(ex);
        }
    }
}

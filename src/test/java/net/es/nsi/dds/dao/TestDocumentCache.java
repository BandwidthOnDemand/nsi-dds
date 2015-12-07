/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.dao;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collection;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.provider.Document;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class TestDocumentCache {
    private DdsConfiguration config;

    @Before
    public void setUp() throws IllegalArgumentException, JAXBException, FileNotFoundException, NullPointerException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        config = new DdsConfiguration();
        config.setFilename("src/test/resources/config/dds.xml");
        config.load();
        System.out.println("Configured nsaId=" + config.getNsaId());
    }

    @Test
    public void testLoadCache() throws FileNotFoundException, UnsupportedEncodingException {
        System.out.println("@Test - testLoadCache");
        DdsProfile ddsProfile = new CacheProfile(config);
        DocumentCache cache = new DocumentCache(ddsProfile);
        assertTrue(cache.isEnabled());
        cache.load();
        Collection<Document> values = cache.values();
        assertTrue(values.size() > 0);
        for (Document document : values) {
            System.out.println("id=" + URLDecoder.decode(document.getId(), "UTF-8"));
        }
        int original = values.size();
        cache.expire();
        assertEquals(original, cache.values().size());
    }

    @Test
    public void testLoadRepository() throws FileNotFoundException, UnsupportedEncodingException {
        System.out.println("@Test - testLoadRepository");
        DdsProfile ddsProfile = new RepositoryProfile(config);
        DocumentCache cache = new DocumentCache(ddsProfile);
        assertTrue(cache.isEnabled());
        cache.load();
        Collection<Document> values = cache.values();
        assertTrue(values.isEmpty());
    }
}

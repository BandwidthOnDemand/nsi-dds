/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.authorization;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.util.Log4jHelper;
import org.apache.log4j.xml.DOMConfigurator;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class AclSchemaTest {
    private Logger log;
    private DnAuthorizationProvider provider;

    private static final String DN1 = "CN=Go Daddy Secure Certificate Authority - G2, OU=http://certs.godaddy.com/repository/, O=\"GoDaddy.com, Inc.\", L=Scottsdale, ST=Arizona, C=US";
    private static final String DN2 = "CN=Go Daddy Root Certificate Authority - G2, O=\"GoDaddy.com, Inc.\", L=Scottsdale, ST=Arizona, C=US";
    private static final String DN3 = "CN=TERENA SSL CA, O=TERENA, C=NL";
    private static final String DN4 = "CN=bod.netherlight.net, OU=Domain Control Validated";
    private static final String DN5 = "emailAddress=opennsa@northwestern.edu, OU=iCAIR - StarLight, L=Chicago, O=Northwestern U IT, ST=Illinois, C=US";
    private static final String DN6 = "1.2.840.113549.1.9.1=#16186F70656E6E7361406E6F7274687765737465726E2E656475, OU=iCAIR - StarLight, L=Chicago, O=Northwestern U IT, ST=Illinois, C=US";
    private static final String DN7 = "emailAddress=bob@example.com, CN=Bobby Boogie, OU=Sprockets Manufacturing, O=Sprockets R Us, L=Ottawa, ST=ON, C=CA";
    private static final String DN8 = "C=NL, O=TERENA, CN=TERENA SSL CA, O=TERENA";

    @Before
    public void setUp() throws IllegalArgumentException, JAXBException, FileNotFoundException, NullPointerException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        // Load and watch the log4j configuration file for changes.
        DOMConfigurator.configureAndWatch(Log4jHelper.getLog4jConfig("src/test/resources/config/"), 45 * 1000);
        log = LoggerFactory.getLogger(AclSchemaTest.class);

        DdsConfiguration config = new DdsConfiguration();
        config.setFilename("src/test/resources/config/dds.xml");
        config.load();
        provider = new DnAuthorizationProvider(config);
    }

    @Test
    public void sucessfulAuthorizeTest() {
        assertTrue(provider.authorize(DN1, "GET", "/dds/documents"));
        assertTrue(provider.authorize(DN2, "GET", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa"));
        assertTrue(provider.authorize(DN3, "GET", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml"));
        assertTrue(provider.authorize(DN4, "DELETE", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology" ));
        assertTrue(provider.authorize(DN6, "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Atopology"));
        assertTrue(provider.authorize(DN5, "DELETE", "/dds/documents/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Atopology"));
    }

    @Test
    public void unsuccessfulAuthorizeTest() {
        assertFalse(provider.authorize(DN1, "POST", "/dds/documents"));
        assertFalse(provider.authorize(DN2, "POOP", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa"));
        assertFalse(provider.authorize(DN5, "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));
        assertFalse(provider.authorize(DN5, "POST", "/dds/documents"));
        assertFalse(provider.authorize(DN7, "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));
        assertFalse(provider.authorize(DN8, "GET", "/dds/documents"));
    }

    @Test
    public void npeAuthorizeTest() {
        assertFalse(provider.authorize(null, null, null));
    }
}

package net.es.nsi.dds.authorization;

import jakarta.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import net.es.nsi.dds.dao.DdsConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the basic authorization mechanism implemented through XML
 * configuration, X.509 certificates and HTTP operations.
 *
 * @author hacksaw
 */
public class AclSchemaTest {
    private static final Logger log = LogManager.getLogger(AclSchemaTest.class);
    private DnAuthorizationProvider provider;

    // DBN
    private static final String DN1 = "CN=Go Daddy Secure Certificate Authority - G2, OU=http://certs.godaddy.com/repository/, O=\"GoDaddy.com, Inc.\", L=Scottsdale, ST=Arizona, C=US";
    private static final String DN2 = "CN=Go Daddy Root Certificate Authority - G2, O=\"GoDaddy.com, Inc.\", L=Scottsdale, ST=Arizona, C=US";
    private static final String DN3 = "CN=TERENA SSL CA, O=TERENA, C=NL";
    private static final String DN4 = "CN=bod.netherlight.net, OU=Domain Control Validated";
    private static final String DN5 = "emailAddress=opennsa@northwestern.edu, OU=iCAIR - StarLight, L=Chicago, O=Northwestern U IT, ST=Illinois, C=US";
    private static final String DN6 = "1.2.840.113549.1.9.1=#16186F70656E6E7361406E6F7274687765737465726E2E656475, OU=iCAIR - StarLight, L=Chicago, O=Northwestern U IT, ST=Illinois, C=US";
    private static final String DN7 = "emailAddress=bob@example.com, CN=Bobby Boogie, OU=Sprockets Manufacturing, O=Sprockets R Us, L=Ottawa, ST=ON, C=CA";
    private static final String DN8 = "C=NL, O=TERENA, CN=TERENA SSL CA, O=TERENA";
    private static final String DN9 = "CN=nsi-aggr-west.es.net,OU=Domain Control Validated";

  @BeforeClass
  public static void initialize() {
  }


    /**
     * All ACL schema for this test is held in the file "src/test/resources/config/dds.xml".
     *
     * @throws IllegalArgumentException
     * @throws JAXBException
     * @throws FileNotFoundException
     * @throws NullPointerException
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws java.security.KeyManagementException
     * @throws java.security.NoSuchProviderException
     * @throws java.security.UnrecoverableKeyException
     */
    @Before
    public void setUp() throws Exception {

        // Load DDS configuration file containing the ACL list.
        DdsConfiguration config = new DdsConfiguration();
        config.setFilename("src/test/resources/config/dds-secure.xml");
        try {
          config.load();
        } catch (JAXBException | IOException | IllegalArgumentException | KeyManagementException | KeyStoreException
                | NoSuchAlgorithmException | NoSuchProviderException | UnrecoverableKeyException
                | CertificateException | java.lang.NullPointerException ex) {
          log.error("AclSchemaTest: DdsConfiguration load failed", ex);
          throw ex;
        }
        provider = new DnAuthorizationProvider(config);
    }

    @Test
    public void sucessfulAuthorizeTest() {
      //  <rule access="read">
      //      <dn>CN=Go Daddy Secure Certificate Authority - G2, OU=http://certs.godaddy.com/repository/, O="GoDaddy.com, Inc.", L=Scottsdale, ST=Arizona, C=US</dn>
      //  </rule>
      assertTrue(provider.authorize(DN1, "GET", "/dds/documents"));
      assertTrue(provider.authorize(DN1, "GET", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));

      //  <rule access="read">
      //      <dn>CN=Go Daddy Root Certificate Authority - G2, O="GoDaddy.com, Inc.", L=Scottsdale, ST=Arizona, C=US</dn>
      //  </rule>
      assertTrue(provider.authorize(DN2, "GET", "/dds/documents"));
      assertTrue(provider.authorize(DN2, "GET", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa"));
      assertTrue(provider.authorize(DN2, "GET", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));

      //  <rule access="read">
      //      <dn>CN=TERENA SSL CA, O=TERENA, C=NL</dn>
      //  </rule>
      assertTrue(provider.authorize(DN3, "GET", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml"));

      //  <rule access="admin">
      //      <dn>CN=bod.netherlight.net, OU=Domain Control Validated</dn>
      //  </rule>
      assertTrue(provider.authorize(DN4, "GET", "/dds/documents"));
      assertTrue(provider.authorize(DN4, "POST", "/dds/notifications"));
      assertTrue(provider.authorize(DN4, "POST", "/dds/subscriptions"));
      assertTrue(provider.authorize(DN4, "DELETE", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology" ));

      //  <rule access="write">
      //      <dn>emailAddress=opennsa@northwestern.edu, OU=iCAIR - StarLight, L=Chicago, O=Northwestern U IT, ST=Illinois, C=US</dn>
      //      <nsaId>urn:ogf:network:icair.org:2013:nsa</nsaId>
      //  </rule>
      assertTrue(provider.authorize(DN5, "GET", "/dds/documents"));
      assertTrue(provider.authorize(DN5, "POST", "/dds/documents"));
      assertTrue(provider.authorize(DN6, "POST", "/dds/notifications"));
      assertTrue(provider.authorize(DN5, "POST", "/dds/subscriptions"));
      assertTrue(provider.authorize(DN5, "DELETE", "/dds/subscriptions/6aac04db-9065-4aae-9e97-b556defdfa89"));
      assertTrue(provider.authorize(DN6, "DELETE", "/dds/documents/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Atopology"));
      assertTrue(provider.authorize(DN5, "DELETE", "/dds/documents/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Atopology"));
      assertTrue(provider.authorize(DN6, "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Atopology"));

      // <rule access="peer">
      //    <dn>CN=nsi-aggr-west.es.net,OU=Domain Control Validated</dn>
      //</rule>
      assertTrue(provider.authorize(DN9, "GET", "/dds/documents"));
      assertTrue(provider.authorize(DN9, "POST", "/dds/notifications"));
      assertTrue(provider.authorize(DN9, "POST", "/dds/subscriptions"));
      assertTrue(provider.authorize(DN9, "DELETE", "/dds/subscriptions/6aac04db-9065-4aae-9e97-b556defdfa89"));
      assertTrue(provider.authorize(DN9, "DELETE", "/dds/documents/urn%3Aogf%3Anetwork%3Aes.net%3A2013%3Ansa%3Ansi-aggr-west&type=vnd.ogf.nsi.nsa.v1%2Bxml&id=urn%3Aogf%3Anetwork%3Aes.net%3A2013%3Ansa%3Ansi-aggr-west" ));
      assertTrue(provider.authorize(DN9, "DELETE", "/dds/documents/urn%3Aogf%3Anetwork%3Aes.net%3A2013%3Ansa&type=vnd.ogf.nsi.topology.v2%2Bxml&id=urn%3Aogf%3Anetwork%3Aes.net%3A2013%3A"));
    }

    @Test
    public void unsuccessfulAuthorizeTest() {
      //  <rule access="read">
      //      <dn>CN=Go Daddy Secure Certificate Authority - G2, OU=http://certs.godaddy.com/repository/, O="GoDaddy.com, Inc.", L=Scottsdale, ST=Arizona, C=US</dn>
      //  </rule>
      assertFalse(provider.authorize(DN1, "POST", "/dds/documents"));
      assertFalse(provider.authorize(DN1, "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Atopology"));
      assertFalse(provider.authorize(DN1, "DELETE", "/dds/documents/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Atopology"));
      assertFalse(provider.authorize(DN1, "GET", "/dds/notifications"));
      assertFalse(provider.authorize(DN1, "GET", "/dds/subscriptions"));

      // <rule access="peer">
      //    <dn>CN=nsi-aggr-west.es.net,OU=Domain Control Validated</dn>
      //</rule>
      assertFalse(provider.authorize(DN6, "DELETE", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));
      assertFalse(provider.authorize(DN5, "POST", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));
      assertFalse(provider.authorize(DN2, "POOP", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa"));
      assertFalse(provider.authorize(DN5, "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));
      assertFalse(provider.authorize(DN7, "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));
      assertFalse(provider.authorize(DN8, "GET", "/dds/documents"));
    }

    @Test
    public void npeAuthorizeTest() {
        assertFalse(provider.authorize(null, null, null));
    }
}

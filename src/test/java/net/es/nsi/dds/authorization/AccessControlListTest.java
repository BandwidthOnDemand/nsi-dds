/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.authorization;

import net.es.nsi.dds.jaxb.configuration.AccessControlType;
import net.es.nsi.dds.jaxb.configuration.ObjectFactory;

/**
 * Test cases for the AccessControlList.
 *
 * @author hacksaw
 */
public class AccessControlListTest {
    private final ObjectFactory factory = new ObjectFactory();
    private AccessControlType acl_enabled;
    private AccessControlType acl_disabled;
    /**
    private static final DistinguishedNameType DN1 = new DistinguishedNameType() {{
        setValue("CN=Go Daddy Secure Certificate Authority - G2, OU=http://certs.godaddy.com/repository/, O=\"GoDaddy.com, Inc.\", L=Scottsdale, ST=Arizona, C=US");
        setAccess(AccessControlPermission.READ);
    }};

    private static final DistinguishedNameType DN2 = new DistinguishedNameType() {{
        setValue("CN=Go Daddy Root Certificate Authority - G2, O=\"GoDaddy.com, Inc.\", L=Scottsdale, ST=Arizona, C=US");
        setAccess(AccessControlPermission.READ);
    }};

    private static final DistinguishedNameType DN3 = new DistinguishedNameType() {{
        setValue("CN=TERENA SSL CA, O=TERENA, C=NL");
        setAccess(AccessControlPermission.READ);
    }};
    private static final DistinguishedNameType DN4 = new DistinguishedNameType() {{
        setValue("CN=bod.netherlight.net, OU=Domain Control Validated");
        setAccess(AccessControlPermission.ADMIN);
    }};
    private static final DistinguishedNameType DN5 = new DistinguishedNameType() {{
        setValue("emailAddress=opennsa@northwestern.edu, OU=iCAIR - StarLight, L=Chicago, O=Northwestern U IT, ST=Illinois, C=US");
        setAccess(AccessControlPermission.WRITE);
    }};

    private static final DistinguishedNameType DN6 = new DistinguishedNameType() {{
        setValue("1.2.840.113549.1.9.1=#16186F70656E6E7361406E6F7274687765737465726E2E656475, OU=iCAIR - StarLight, L=Chicago, O=Northwestern U IT, ST=Illinois, C=US");
        setAccess(AccessControlPermission.WRITE);
    }};
    private static final DistinguishedNameType DN7 = new DistinguishedNameType() {{
        setValue("emailAddress=bob@example.com, CN=Bobby Boogie, OU=Sprockets Manufacturing, O=Sprockets R Us, L=Ottawa, ST=ON, C=CA");
        setAccess(AccessControlPermission.ADMIN);
    }};

    private static final DistinguishedNameType DN8 = new DistinguishedNameType() {{
        setValue("C=NL, O=TERENA, CN=TERENA SSL CA, O=TERENA");
        setAccess(AccessControlPermission.READ);
    }};

    @Before
    public void setUp() throws Exception {
        // Load and watch the log4j configuration file for changes.
        DOMConfigurator.configureAndWatch(Log4jHelper.getLog4jConfig("src/test/resources/config/"), 45 * 1000);
        log = LoggerFactory.getLogger(AccessControlListTest.class);

        acl_enabled = factory.createAccessControlType();
        acl_enabled.setEnabled(true);
        acl_enabled.getDn().add(DN1);
        acl_enabled.getDn().add(DN2);
        acl_enabled.getDn().add(DN3);
        acl_enabled.getDn().add(DN4);
        acl_enabled.getDn().add(DN5);
        acl_enabled.getDn().add(DN6);

        // The default constructor creates an empty ACL with enforcement disbaled.
        acl_disabled = factory.createAccessControlType();
    }

    @Test(expected=IllegalArgumentException.class)
    public void nullConstuctorTest() throws IllegalArgumentException {
        AccessControlList accessControlList = new AccessControlList(null);
    }

    @Test
    public void sucessfulAuthorizeTest() throws Exception {
        AccessControlList accessControlList = new AccessControlList(acl_enabled);
        assertTrue(accessControlList.isEnabled());
        assertTrue(accessControlList.isAuthorized(DN1.getValue(), "GET", "/dds/documents"));
        assertTrue(accessControlList.isAuthorized(DN2.getValue(), "GET", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa"));
        assertTrue(accessControlList.isAuthorized(DN3.getValue(), "GET", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml"));
        assertTrue(accessControlList.isAuthorized(DN4.getValue(), "DELETE", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology" ));
        assertTrue(accessControlList.isAuthorized(DN5.getValue(), "POST", "/dds/documents"));
        assertTrue(accessControlList.isAuthorized(DN6.getValue(), "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));
    }

    @Test
    public void unsuccessfulAuthorizeTest() throws Exception {
        AccessControlList accessControlList = new AccessControlList(acl_enabled);
        assertTrue(accessControlList.isEnabled());
        assertFalse(accessControlList.isAuthorized(DN1.getValue(), "POST", "/dds/documents"));
        assertFalse(accessControlList.isAuthorized(DN2.getValue(), "POOP", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa"));
        assertFalse(accessControlList.isAuthorized(DN7.getValue(), "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));
        assertFalse(accessControlList.isAuthorized(DN8.getValue(), "GET", "/dds/documents"));
        assertFalse(accessControlList.isAuthorized("", "GET", "/dds/documents"));
        assertFalse(accessControlList.isAuthorized(null, null, null));
    }

    @Test
    public void disabledAuthorizeTest() throws Exception {
        AccessControlList accessControlList = new AccessControlList(acl_disabled);
        assertFalse(accessControlList.isEnabled());
        assertTrue(accessControlList.isAuthorized(DN1.getValue(), "GET", "/dds/documents"));
        assertTrue(accessControlList.isAuthorized(DN7.getValue(), "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));
        assertTrue(accessControlList.isAuthorized("", "", ""));
        assertTrue(accessControlList.isAuthorized(null, null, null));
    }
    * */
}
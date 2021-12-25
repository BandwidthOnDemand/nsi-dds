package net.es.nsi.dds.authorization;

import net.es.nsi.dds.jaxb.configuration.AccessControlPermission;
import net.es.nsi.dds.jaxb.configuration.AccessControlType;
import net.es.nsi.dds.jaxb.configuration.DistinguishedNameType;
import net.es.nsi.dds.jaxb.configuration.ObjectFactory;
import net.es.nsi.dds.jaxb.configuration.RuleType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for the AccessControlList.
 *
 * @author hacksaw
 */
public class AccessControlListTest {
  private static final Logger log = LogManager.getLogger(AccessControlListTest.class);
  private final ObjectFactory factory = new ObjectFactory();
  private AccessControlType acl_enabled;
  private AccessControlType acl_disabled;

  private static final DistinguishedNameType DN1 = new DistinguishedNameType() {
    {
      setValue("CN=Go Daddy Secure Certificate Authority - G2, OU=http://certs.godaddy.com/repository/, O=\"GoDaddy.com, Inc.\", L=Scottsdale, ST=Arizona, C=US");
    }
  };

  private static final DistinguishedNameType DN2 = new DistinguishedNameType() {
    {
      setValue("CN=Go Daddy Root Certificate Authority - G2, O=\"GoDaddy.com, Inc.\", L=Scottsdale, ST=Arizona, C=US");
    }
  };

  private static final DistinguishedNameType DN3 = new DistinguishedNameType() {
    {
      setValue("CN=TERENA SSL CA, O=TERENA, C=NL");
    }
  };

  private static final DistinguishedNameType DN4 = new DistinguishedNameType() {
    {
      setValue("CN=bod.netherlight.net, OU=Domain Control Validated");
    }
  };

  private static final DistinguishedNameType DN5 = new DistinguishedNameType() {
    {
      setValue("emailAddress=opennsa@northwestern.edu, OU=iCAIR - StarLight, L=Chicago, O=Northwestern U IT, ST=Illinois, C=US");
    }
  };

  private static final DistinguishedNameType DN6 = new DistinguishedNameType() {
    {
      setValue("1.2.840.113549.1.9.1=#16186F70656E6E7361406E6F7274687765737465726E2E656475, OU=iCAIR - StarLight, L=Chicago, O=Northwestern U IT, ST=Illinois, C=US");
    }
  };

  private static final DistinguishedNameType DN7 = new DistinguishedNameType() {
    {
      setValue("emailAddress=bob@example.com, CN=Bobby Boogie, OU=Sprockets Manufacturing, O=Sprockets R Us, L=Ottawa, ST=ON, C=CA");
    }
  };

  @BeforeClass
  public static void initialize() {
  }

  @Before
  public void setUp() throws Exception {
    // Load and watch the log4j configuration file for changes.
    //DOMConfigurator.configureAndWatch(Log4jHelper.getLog4jConfig("src/test/resources/config/"), 45 * 1000);

    log.debug("AccessControlListTest.setUp start");

    acl_enabled = factory.createAccessControlType();
    acl_enabled.setEnabled(true);

    // Build a rule for DN1.
    RuleType rule = factory.createRuleType();
    rule.setDn(DN1);
    rule.setAccess(AccessControlPermission.READ);
    acl_enabled.getRule().add(rule);

    // Build a rule for DN2.
    rule = factory.createRuleType();
    rule.setDn(DN2);
    rule.setAccess(AccessControlPermission.READ);
    acl_enabled.getRule().add(rule);

    // Build a rule for DN3.
    rule = factory.createRuleType();
    rule.setDn(DN3);
    rule.setAccess(AccessControlPermission.READ);
    acl_enabled.getRule().add(rule);

    rule = factory.createRuleType();
    rule.setDn(DN4);
    rule.setAccess(AccessControlPermission.ADMIN);
    rule.getNsaId().add("urn:ogf:network:es.net:2013:nsa");
    acl_enabled.getRule().add(rule);

    rule = factory.createRuleType();
    rule.setDn(DN5);
    rule.setAccess(AccessControlPermission.WRITE);
    rule.getNsaId().add("urn:ogf:network:icair.org:2013:nsa");
    acl_enabled.getRule().add(rule);

    rule = factory.createRuleType();
    rule.setDn(DN6);
    rule.setAccess(AccessControlPermission.WRITE);
    rule.getNsaId().add("urn:ogf:network:icair.org:2013:nsa");
    acl_enabled.getRule().add(rule);

    // The default constructor creates an empty ACL with enforcement disbaled.
    acl_disabled = factory.createAccessControlType();

    log.debug("AccessControlListTest.setUp done");
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullConstuctorTest() throws IllegalArgumentException {
    log.debug("AccessControlListTest.nullConstuctorTest start");
    AccessControlList accessControlList = new AccessControlList(null);
    log.debug("AccessControlListTest.nullConstuctorTest done");
  }

  @Test
  public void sucessfulAuthorizeTest() throws Exception {
    log.debug("AccessControlListTest.sucessfulAuthorizeTest start");
    AccessControlList accessControlList = new AccessControlList(acl_enabled);
    assertTrue(accessControlList.isEnabled());
    assertTrue(accessControlList.isAuthorized(DN1.getValue(), "GET", "/dds/documents"));
    assertTrue(accessControlList.isAuthorized(DN2.getValue(), "GET", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa"));
    assertTrue(accessControlList.isAuthorized(DN3.getValue(), "GET", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml"));
    assertTrue(accessControlList.isAuthorized(DN4.getValue(), "DELETE", "/dds/documents/urn%3Aogf%3Anetwork%3Aes.net.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Aes.net%3A2013%3A"));
    assertTrue(accessControlList.isAuthorized(DN5.getValue(), "POST", "/dds/documents"));
    assertTrue(accessControlList.isAuthorized(DN6.getValue(), "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Aicair.org%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Asl9208.icair.org%3A2013%3Atopology"));
    log.debug("AccessControlListTest.sucessfulAuthorizeTest done");
  }

  @Test
  public void unsuccessfulAuthorizeTest() throws Exception {
    log.debug("AccessControlListTest.unsuccessfulAuthorizeTest start");
    AccessControlList accessControlList = new AccessControlList(acl_enabled);
    assertTrue(accessControlList.isEnabled());
    assertFalse(accessControlList.isAuthorized(DN1.getValue(), "POST", "/dds/documents"));
    assertFalse(accessControlList.isAuthorized(DN2.getValue(), "POOP", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa"));
    assertFalse(accessControlList.isAuthorized(DN7.getValue(), "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));
    assertFalse(accessControlList.isAuthorized("", "GET", "/dds/documents"));
    assertFalse(accessControlList.isAuthorized(null, null, null));
    log.debug("AccessControlListTest.unsuccessfulAuthorizeTest done");
  }

  @Test
  public void disabledAuthorizeTest() throws Exception {
    log.debug("AccessControlListTest.disabledAuthorizeTest start");
    AccessControlList accessControlList = new AccessControlList(acl_disabled);
    assertFalse(accessControlList.isEnabled());
    assertTrue(accessControlList.isAuthorized(DN1.getValue(), "GET", "/dds/documents"));
    assertTrue(accessControlList.isAuthorized(DN7.getValue(), "PUT", "/dds/documents/urn%3Aogf%3Anetwork%3Ageant.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Apionier.net.pl%3A2013%3Atopology"));
    assertTrue(accessControlList.isAuthorized("", "", ""));
    assertTrue(accessControlList.isAuthorized(null, null, null));
    log.debug("AccessControlListTest.disabledAuthorizeTest done");
  }
}

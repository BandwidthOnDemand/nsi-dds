package net.es.nsi.dds.authorization;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.dao.DdsConfiguration;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

/**
 * Tests for the DnAuthorizationProvider.
 *
 * @author hacksaw
 */
@Slf4j
public class DnAuthorizationProviderTest {
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void sucessfulAuthorizeTest() throws Exception {
    log.debug("DnAuthorizationProviderTest: sucessfulAuthorizeTest");
    AccessControlList mockACL = mock(AccessControlList.class);
    when(mockACL.isAuthorized(anyString(), anyString(), anyString())).thenReturn(true);

    DdsConfiguration mockConfig = mock(DdsConfiguration.class);
    when(mockConfig.getAccessControlList()).thenReturn(mockACL);

    DnAuthorizationProvider provider = new DnAuthorizationProvider(mockConfig);
    boolean authorize = provider.authorize("emailAddress=bob@example.com, CN=Bobby Boogie, OU=Sprockets Manufacturing, O=Sprockets R Us, L=Ottawa, ST=ON, C=CA", "POST", "/dds/subscriptions");
    assertTrue(authorize);
  }

  @Test
  public void failedAuthorizeTest() throws Exception {
    log.debug("DnAuthorizationProviderTest: sucessfulAuthorizeTest");
    AccessControlList mockACL = mock(AccessControlList.class);
    when(mockACL.isAuthorized(anyString(), anyString(), anyString())).thenReturn(false);

    DdsConfiguration mockConfig = mock(DdsConfiguration.class);
    when(mockConfig.getAccessControlList()).thenReturn(mockACL);

    DnAuthorizationProvider provider = new DnAuthorizationProvider(mockConfig);
    boolean authorize = provider.authorize("emailAddress=bob@example.com, CN=Bobby Boogie, OU=Sprockets Manufacturing, O=Sprockets R Us, L=Ottawa, ST=ON, C=CA", "POST", "/dds/subscriptions");
    assertFalse(authorize);
  }
}

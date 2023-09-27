package net.es.nsi.dds.management;


import jakarta.ws.rs.client.WebTarget;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.jaxb.management.PingType;
import net.es.nsi.dds.test.TestConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class PingTest {
    private static TestConfig testConfig;
    private static WebTarget management;

    @BeforeClass
    public static void oneTimeSetUp() throws IllegalStateException, KeyManagementException, NoSuchAlgorithmException,
            NoSuchProviderException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        log.debug("*************************************** PingTest oneTimeSetUp ***********************************");
        testConfig = new TestConfig();
        management = testConfig.getTarget().path("dds").path("management").path("v1").path("ping");
        log.debug("*************************************** PingTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() throws InterruptedException {
        log.debug("*************************************** PingTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        log.debug("*************************************** PingTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void testPingXML() {
        log.debug("********************************* testPingXML.testStatusXML Start *********************************");
        Response response = management.request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        PingType ping = response.readEntity(PingType.class);
        assertNotNull(ping);

        log.debug("[PingTest].ping: received response, id = {}", ping.getTime());

        log.debug("********************************* testPingXML.testStatusXML End *********************************");
    }

    @Test
    public void testPingJSON() {
        log.debug("********************************* PingTest.testPingJSON Start *********************************");
        Response response = management.request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        PingType ping = response.readEntity(PingType.class);
        assertNotNull(ping);

        log.debug("[PingTest].ping: received response, id = {}", ping.getTime());

        log.debug("********************************* PingTest.testPingJSON End *********************************");
    }
}

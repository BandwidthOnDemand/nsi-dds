package net.es.nsi.dds.management;


import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.jaxb.management.HealthStatusType;
import net.es.nsi.dds.jaxb.management.PingType;
import net.es.nsi.dds.test.TestConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.*;
import java.security.cert.CertificateException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class HealthTest {
    private static TestConfig testConfig;
    private static WebTarget management;

    @BeforeClass
    public static void oneTimeSetUp() throws IllegalStateException, KeyManagementException, NoSuchAlgorithmException,
            NoSuchProviderException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        log.debug("*************************************** HealthTest oneTimeSetUp ***********************************");
        testConfig = new TestConfig();
        management = testConfig.getTarget().path("dds").path("management").path("v1").path("health");
        log.debug("*************************************** HealthTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() throws InterruptedException {
        log.debug("*************************************** HealthTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        log.debug("*************************************** HealthTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void testHealthXML() {
        log.debug("********************************* HealthTest.testHealthXML Start *********************************");
        Response response = management.request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        HealthStatusType health = response.readEntity(HealthStatusType.class);
        assertNotNull(health);

        log.debug("[HealthTest].testHealthXML: received response, id = {}", health.getStatus());

        log.debug("********************************* HealthTest.testHealthXML End *********************************");
    }

    @Test
    public void testHealthJSON() {
        log.debug("********************************* HealthTest.testHealthJSON Start *********************************");
        Response response = management.request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        HealthStatusType health = response.readEntity(HealthStatusType.class);
        assertNotNull(health);

        log.debug("[HealthTest].testHealthJSON: received response, id = {}", health.getStatus());

        log.debug("********************************* HealthTest.testHealthJSON End *********************************");
    }
}

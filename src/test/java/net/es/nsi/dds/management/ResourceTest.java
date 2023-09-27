package net.es.nsi.dds.management;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.jaxb.ManagementParser;
import net.es.nsi.dds.jaxb.management.ResourceListType;
import net.es.nsi.dds.test.TestConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.*;
import java.security.cert.CertificateException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class ResourceTest {
    private static TestConfig testConfig;
    private static WebTarget management;

    @BeforeClass
    public static void oneTimeSetUp() throws IllegalStateException, KeyManagementException, NoSuchAlgorithmException,
            NoSuchProviderException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        log.debug("*************************************** ResourceTest oneTimeSetUp ***********************************");
        testConfig = new TestConfig();
        management = testConfig.getTarget().path("dds").path("management");
        log.debug("*************************************** ResourceTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() throws InterruptedException {
        log.debug("*************************************** ResourceTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        log.debug("*************************************** ResourceTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void getResourcesXML() throws JAXBException {
        log.debug("********************************* ResourceTest.getVersion Start *********************************");
        Response response = management.request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        ResourceListType resources = response.readEntity(ResourceListType.class);
        assertNotNull(resources);

        log.debug("[ResourceTest].getResources: received response:\n{}",
            ManagementParser.getInstance().xmlFormatter(resources));

        assertTrue(resources.getResource().stream().anyMatch(a -> a.getId().equalsIgnoreCase("version")));
        assertTrue(resources.getResource().stream().anyMatch(a -> a.getId().equalsIgnoreCase("logs")));
        assertTrue(resources.getResource().stream().anyMatch(a -> a.getId().equalsIgnoreCase("ping")));

        log.debug("********************************* ResourceTest.getVersion End *********************************");
    }


    @Test
    public void getResourcesJSON() {
        log.debug("********************************* ResourceTest.getVersion Start *********************************");

        Response response = management.request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        ResourceListType resources = response.readEntity(ResourceListType.class);

        assertTrue(resources.getResource().stream().anyMatch(a -> a.getId().equalsIgnoreCase("version")));
        assertTrue(resources.getResource().stream().anyMatch(a -> a.getId().equalsIgnoreCase("logs")));
        assertTrue(resources.getResource().stream().anyMatch(a -> a.getId().equalsIgnoreCase("ping")));

        log.debug("********************************* ResourceTest.getVersion End *********************************");

    }
}

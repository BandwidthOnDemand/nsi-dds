package net.es.nsi.dds.management;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Optional;

import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.jaxb.ManagementParser;
import net.es.nsi.dds.jaxb.management.AttributeType;
import net.es.nsi.dds.jaxb.management.VersionType;
import net.es.nsi.dds.test.TestConfig;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class VersionTest {
    private static TestConfig testConfig;
    private static WebTarget management;

    @BeforeClass
    public static void oneTimeSetUp() throws IllegalStateException, KeyManagementException, NoSuchAlgorithmException,
            NoSuchProviderException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        log.debug("*************************************** VersionTest oneTimeSetUp ***********************************");
        testConfig = new TestConfig();
        management = testConfig.getTarget().path("dds").path("management").path("v1");
        log.debug("*************************************** VersionTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() throws InterruptedException {
        log.debug("*************************************** VersionTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        log.debug("*************************************** VersionTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void getVersionXML() throws JAXBException {
        log.debug("********************************* VersionTest.getVersion Start *********************************");
        Response response = management.path("version").request(MediaType.APPLICATION_XML).get();
        process(response);
    }

    @Test
    public void getVersionJSON() throws JAXBException {
        log.debug("********************************* VersionTest.getVersion Start *********************************");
        Response response = management.path("version").request(MediaType.APPLICATION_JSON).get();
        process(response);
    }

    public void process(Response response) throws JAXBException {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        VersionType version = response.readEntity(VersionType.class);
        assertNotNull(version);

        Optional<String> projectVersion = Optional.empty();
        Optional<String> gitVersion = Optional.empty();
        for (AttributeType attribute : version.getAttribute()) {
            if (attribute.getType().compareToIgnoreCase("project.version") == 0) {
                projectVersion = Optional.ofNullable(attribute.getType());
            }
            else if (attribute.getType().compareToIgnoreCase("git.commit.id") == 0) {
                gitVersion = Optional.ofNullable(attribute.getType());
            }
        }

        assertTrue(projectVersion.isPresent());
        assertTrue(gitVersion.isPresent());

        log.debug("VersionTest.getVersion: received response:\n{}",
            ManagementParser.getInstance().xmlFormatter(version));

        log.debug("********************************* VersionTest.getVersion End *********************************");

    }
}

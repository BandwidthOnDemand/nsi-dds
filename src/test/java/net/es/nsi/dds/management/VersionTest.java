package net.es.nsi.dds.management;

import com.google.common.base.Optional;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.jaxb.management.AttributeType;
import net.es.nsi.dds.jaxb.management.VersionType;
import net.es.nsi.dds.test.TestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class VersionTest {
    private static TestConfig testConfig;
    private static WebTarget management;
    private static Logger log;

    @BeforeClass
    public static void oneTimeSetUp() {
        System.setProperty(Properties.SYSTEM_PROPERTY_LOG4J, "src/test/resources/config/log4j.xml");
        log = LogManager.getLogger(VersionTest.class);

        log.debug("*************************************** TimersTest oneTimeSetUp ***********************************");
        testConfig = new TestConfig();
        management = testConfig.getTarget().path("dds").path("management");
        log.debug("*************************************** TimersTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        log.debug("*************************************** TimersTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        log.debug("*************************************** TimersTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void getVersion() {
        Response response = management.path("version").request(MediaType.APPLICATION_XML).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        VersionType version = response.readEntity(VersionType.class);
        assertNotNull(version);

        Optional<String> projectVersion = Optional.absent();
        Optional<String> gitVersion = Optional.absent();
        for (AttributeType attribute : version.getAttribute()) {
            if (attribute.getType().compareToIgnoreCase("project.version") == 0) {
                projectVersion = Optional.fromNullable(attribute.getType());
            }
            else if (attribute.getType().compareToIgnoreCase("git.commit.id") == 0) {
                gitVersion = Optional.fromNullable(attribute.getType());
            }
        }

        assertNotNull(projectVersion.get());
        assertNotNull(gitVersion.get());
    }
}

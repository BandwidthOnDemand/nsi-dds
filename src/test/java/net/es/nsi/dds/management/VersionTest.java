package net.es.nsi.dds.management;

import com.google.common.base.Optional;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.dds.jaxb.management.AttributeType;
import net.es.nsi.dds.jaxb.management.VersionType;
import net.es.nsi.dds.test.TestConfig;
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

    @BeforeClass
    public static void oneTimeSetUp() {
        System.out.println("*************************************** TimersTest oneTimeSetUp ***********************************");
        testConfig = new TestConfig();
        management = testConfig.getTarget().path("dds").path("management");
        System.out.println("*************************************** TimersTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        System.out.println("*************************************** TimersTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        System.out.println("*************************************** TimersTest oneTimeTearDown done ***********************************");
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

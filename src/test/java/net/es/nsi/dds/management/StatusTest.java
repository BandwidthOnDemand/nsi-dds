package net.es.nsi.dds.management;


import jakarta.ws.rs.client.WebTarget;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.test.TestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author hacksaw
 */
public class StatusTest {
    private static TestConfig testConfig;
    private static WebTarget management;
    private static Logger log;

    @BeforeClass
    public static void oneTimeSetUp() {
        System.setProperty(Properties.SYSTEM_PROPERTY_LOG4J, "src/test/resources/config/log4j.xml");
        log = LogManager.getLogger(StatusTest.class);

        log.debug("*************************************** StatusTest oneTimeSetUp ***********************************");
        testConfig = new TestConfig();
        management = testConfig.getTarget().path("dds").path("management").path("status");
        log.debug("*************************************** StatusTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        log.debug("*************************************** StatusTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        log.debug("*************************************** StatusTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void testStatus() {
        // Simple status to determine current state of topology discovery.
        /*Response response = management.path("topology").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        StatusType status = response.readEntity(StatusType.class);
        log.debug("Status code = " + status.getStatus());
        assertEquals(TopologyStatusType.COMPLETED, status.getStatus());
        log.debug("Topology status code = " + status.getStatus().value());*/
    }
}

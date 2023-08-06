package net.es.nsi.dds.management;

import jakarta.ws.rs.client.WebTarget;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.test.TestConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class TimersTest {
    private static TestConfig testConfig;
    private static WebTarget management;

    @BeforeClass
    public static void oneTimeSetUp() throws IllegalStateException, KeyManagementException, NoSuchAlgorithmException,
            NoSuchProviderException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        log.debug("*************************************** TimersTest oneTimeSetUp ***********************************");
        testConfig = new TestConfig();
        management = testConfig.getTarget().path("dds").path("management");
        log.debug("*************************************** TimersTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() throws InterruptedException {
        log.debug("*************************************** TimersTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        log.debug("*************************************** TimersTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void getAllTimers() {
        /*Response response = management.path("timers").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        TimerListType timerList = response.readEntity(TimerListType.class);

        assertNotNull(timerList);

        for (TimerType timer : timerList.getTimer()) {
            response = management.path("timers/" + timer.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            TimerType readTimer = response.readEntity(TimerType.class);
            log.debug("Read timer: " + readTimer.getId());
        }*/
    }

    @Test
    public void modifyTimer() {
        /*Response response = management.path("timers/FullTopologyAudit:TopologyManagement").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        TimerType timer = response.readEntity(TimerType.class);

        assertNotNull(timer);

        timer.setTimerInterval(30000);

        ObjectFactory managementFactory = new ObjectFactory();
        JAXBElement<TimerType> createTimer = managementFactory.createTimer(timer);

        response = management.path("timers/FullTopologyAudit:TopologyManagement").request(MediaType.APPLICATION_JSON).put(Entity.entity(new GenericEntity<JAXBElement<TimerType>>(createTimer) {}, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        response = management.path("timers/FullTopologyAudit:TopologyManagement").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        timer = response.readEntity(TimerType.class);
        assertEquals(timer.getTimerInterval(), 30000);*/
    }

    @Test
    public void haultandScheduleTimer() {
        /*Response response = management.path("timers/FullTopologyAudit:TopologyManagement").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        TimerType timer = response.readEntity(TimerType.class);

        assertNotNull(timer);

        // The timer should be idle based on default configuration.
        assertEquals(TimerStatusType.SCHEDULED, timer.getTimerStatus());

        // We need to hault the timer.
        ObjectFactory managementFactory = new ObjectFactory();
        JAXBElement<TimerStatusType> statusElement = managementFactory.createTimerStatus(TimerStatusType.HAULTED);

        response = management.path("timers/FullTopologyAudit:TopologyManagement/status").request(MediaType.APPLICATION_JSON).put(Entity.entity(new GenericEntity<JAXBElement<TimerStatusType>>(statusElement) {}, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        TimerStatusType status = response.readEntity(TimerStatusType.class);
        assertEquals(TimerStatusType.HAULTED, status);

        response = management.path("timers/FullTopologyAudit:TopologyManagement").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        timer = response.readEntity(TimerType.class);
        assertEquals(TimerStatusType.HAULTED, timer.getTimerStatus());

        // Force a run of the job when not scheduled.
        statusElement = managementFactory.createTimerStatus(TimerStatusType.RUNNING);

        response = management.path("timers/FullTopologyAudit:TopologyManagement/status").request(MediaType.APPLICATION_JSON).put(Entity.entity(new GenericEntity<JAXBElement<TimerStatusType>>(statusElement) {}, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        status = response.readEntity(TimerStatusType.class);
        assertEquals(TimerStatusType.RUNNING, status);

        while(true) {
            log.debug("User forced audit...");
            try { Thread.sleep(2000); } catch (Exception ex) {}

            response = management.path("timers/FullTopologyAudit:TopologyManagement/status").request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            status = response.readEntity(TimerStatusType.class);
            if (status != TimerStatusType.RUNNING) {
                log.debug("User forced audit done...");
                break;
            }
        }

        // Force a run of the job when not scheduled.
        statusElement = managementFactory.createTimerStatus(TimerStatusType.SCHEDULED);

        response = management.path("timers/FullTopologyAudit:TopologyManagement/status").request(MediaType.APPLICATION_JSON).put(Entity.entity(new GenericEntity<JAXBElement<TimerStatusType>>(statusElement) {}, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        status = response.readEntity(TimerStatusType.class);
        assertEquals(TimerStatusType.SCHEDULED, status);

        response = management.path("timers/FullTopologyAudit:TopologyManagement/status").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        status = response.readEntity(TimerStatusType.class);
        assertEquals(TimerStatusType.SCHEDULED, status);*/
    }
}

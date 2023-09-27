package net.es.nsi.dds.management;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.jaxb.management.LogListType;
import net.es.nsi.dds.jaxb.management.LogType;
import net.es.nsi.dds.test.TestConfig;
import org.glassfish.jersey.client.ChunkedInput;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class LogsTest {
    private static TestConfig testConfig;
    private static WebTarget management;

    @BeforeClass
    public static void oneTimeSetUp() throws IllegalStateException, KeyManagementException, NoSuchAlgorithmException,
            NoSuchProviderException, KeyStoreException, CertificateException, UnrecoverableKeyException {

        log.debug("*************************************** LogsTest oneTimeSetUp ***********************************");
        testConfig = new TestConfig();
        management = testConfig.getTarget().path("dds").path("management").path("v1");
        log.debug("*************************************** LogsTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() throws InterruptedException {
        log.debug("*************************************** LogsTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        log.debug("*************************************** LogsTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void getAllLogs() {
        log.debug("getAllLogs: entering...");

        Response response = management.path("logs").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final ChunkedInput<LogListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<LogListType>>() {});
        LogListType chunk;
        LogListType finalTopology = null;
        while ((chunk = chunkedInput.read()) != null) {
            log.debug("Chunk received...");
            finalTopology = chunk;
        }
        response.close();
        assertNotNull(finalTopology);

        int count = 0;
        for (LogType l : finalTopology.getLog()) {
            response = management.path("logs/" + l.getId()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            LogType readLog = response.readEntity(LogType.class);
            log.debug("Read log: {}", readLog.getId());
            response.close();

            // Limit the number we retrieve otherwise build will take forever.
            count++;
            if (count > 20) {
                break;
            }
        }
    }

    @Test
    public void getTypeFilteredLogs() {
        log.debug("getTypeFilteredLogs: entering...");

        Response response = management.path("logs").queryParam("type", "Log").queryParam("code", "1001").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final ChunkedInput<LogListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<LogListType>>() {});
        LogListType chunk;
        LogListType finalTopology = null;
        while ((chunk = chunkedInput.read()) != null) {
            log.debug("Chunk received...");
            finalTopology = chunk;
        }
        response.close();

        assertNotNull(finalTopology);
    }

    @Test
    public void getLabelFilteredLogs() {
        log.debug("getLabelFilteredLogs: entering...");

        Response response = management.path("logs").queryParam("label", "AUDIT_SUCCESSFUL").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        LogListType logs = response.readEntity(LogListType.class);
        response.close();

        for (LogType l : logs.getLog()) {
            response = management.path("logs").queryParam("audit", l.getAudit().toXMLFormat()).request(MediaType.APPLICATION_JSON).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final ChunkedInput<LogListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<LogListType>>() {});
            LogListType chunk;
            LogListType finalTopology = null;
            while ((chunk = chunkedInput.read()) != null) {
                log.debug("Chunk received...");
                finalTopology = chunk;
            }
            response.close();

            assertNotNull(finalTopology);
        }
    }

    @Test
    public void badFilter() {
        log.debug("badFilter: entering...");

        Response response = management.path("logs").queryParam("code", "1001").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();

        response = management.path("logs").queryParam("type", "POOP").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();

        response = management.path("logs").path("666").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        response.close();
    }
}

package net.es.nsi.dds.gangofthree;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.client.TestServer;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.discovery.FileUtilities;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.test.TestConfig;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

public class StressTest {

    private final static HttpConfig testServer = new HttpConfig("localhost", "8402", "net.es.nsi.dds.client");

    private final static String DDS_CONFIGURATION = "src/test/resources/config/dds.xml";
    private final static String DOCUMENT_DIR = "src/test/resources/documents/";
    private final static ObjectFactory factory = new ObjectFactory();
    private static DdsConfiguration ddsConfig;
    private static TestConfig testConfig;
    private static WebTarget target;
    private static WebTarget discovery;
    private static String callbackURL;

    @BeforeClass
    public static void oneTimeSetUp() {
        System.out.println("*************************************** DiscoveryTest oneTimeSetUp ***********************************");

        try {
            // Load a copy of the test DDS configuration and clear the document
            // repository for this test.
            ddsConfig = new DdsConfiguration();
            ddsConfig.setFilename(DDS_CONFIGURATION);
            ddsConfig.load();
            File directory = new File(ddsConfig.getRepository());
            FileUtilities.deleteDirectory(directory);

            // Configure the local test client callback server.
            TestServer.INSTANCE.start(testServer);

            callbackURL = new URL(testServer.getURL(), "dds/callback").toString();
        }
        catch (IllegalArgumentException | JAXBException | IOException | IllegalStateException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            System.err.println("oneTimeSetUp: failed to start HTTP server " + ex.getLocalizedMessage());
            fail();
        }

        testConfig = new TestConfig();
        target = testConfig.getTarget();
        discovery = target.path("dds");
        System.out.println("*************************************** DiscoveryTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        System.out.println("*************************************** DiscoveryTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        try {
            TestServer.INSTANCE.shutdown();
        }
        catch (Exception ex) {
            System.err.println("oneTimeTearDown: test server shutdown failed." + ex.getLocalizedMessage());
            fail();
        }
        System.out.println("*************************************** DiscoveryTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void errorTest() {
        // Simple ping to determine if interface is available.
        for (int i = 0; i < 2000; i++) {
            Response response = discovery.path("error").request(MediaType.APPLICATION_XML).get();
            //assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }
}


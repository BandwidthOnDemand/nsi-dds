/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.discovery;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.client.TestServer;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.test.TestConfig;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class TestDocumentRepository {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final static HttpConfig testServer = new HttpConfig("localhost", "8402", "net.es.nsi.dds.client");

    private final static String DOCUMENT_DIR = "src/test/resources/documents/";
    private final static ObjectFactory factory = new ObjectFactory();
    private static DdsConfiguration ddsConfig;
    private static TestConfig testConfig;
    private static WebTarget target;
    private static WebTarget discovery;

    @BeforeClass
    public static void oneTimeSetUp() {
        System.out.println("*************************************** TestDocumentRepository oneTimeSetUp ***********************************");

        testConfig = new TestConfig();
        target = testConfig.getTarget();
        discovery = target.path("dds");

        try {
            // Load a copy of the test DDS configuration and clear the document
            // repository for this test.
            ddsConfig = new DdsConfiguration();
            ddsConfig.setFilename(TestConfig.DEFAULT_DDS_FILE);
            ddsConfig.load();
            File directory = new File(ddsConfig.getRepository());
            FileUtilities.deleteDirectory(directory);

            // Configure the local test client callback server.
            TestServer.INSTANCE.start(testServer);
        }
        catch (IllegalArgumentException | JAXBException | IOException | IllegalStateException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            System.err.println("oneTimeSetUp: failed to start HTTP server " + ex.getLocalizedMessage());
            fail();
        }

        System.out.println("*************************************** TestDocumentRepository oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        System.out.println("*************************************** TestDocumentRepository oneTimeTearDown ***********************************");
        testConfig.shutdown();
        try {
            File directory = new File(ddsConfig.getRepository());
            FileUtilities.deleteDirectory(directory);
            TestServer.INSTANCE.shutdown();
        }
        catch (Exception ex) {
            System.err.println("oneTimeTearDown: test server shutdown failed." + ex.getLocalizedMessage());
            fail();
        }
        System.out.println("*************************************** TestDocumentRepository oneTimeTearDown done ***********************************");
    }

    /**
     * Load the Discovery Service with a default set of documents.
     *
     * @throws Exception
     */
    @Test
    public void addDocuments() throws Exception {
        // For each document file in the document directory load into discovery service.
        List<String> identifiers = new ArrayList<>();
        for (String file : FileUtilities.getXmlFileList(DOCUMENT_DIR)) {
            DocumentType document = DdsParser.getInstance().readDocument(file);
            JAXBElement<DocumentType> jaxbRequest = factory.createDocument(document);
            Response response = discovery.path("documents").request(MediaType.APPLICATION_XML).post(Entity.entity(new GenericEntity<JAXBElement<DocumentType>>(jaxbRequest) {}, MediaType.APPLICATION_XML));
            if (Response.Status.CREATED.getStatusCode() != response.getStatus() &&
                    Response.Status.CONFLICT.getStatusCode() != response.getStatus()) {
                fail();
            }
            response.close();

            identifiers.add(document.getId());
        }

        // Now verify the local repository contains the target files.
        assertTrue(ddsConfig.isRepositoryConfigured());
        assertTrue(verifyAddResults(identifiers));
    }

    private boolean verifyAddResults(List<String> identifiers) throws JAXBException, IOException, NullPointerException {
        List<String> repository = new ArrayList<>();
        for (String file : FileUtilities.getXmlFileList(ddsConfig.getRepository())) {
            DocumentType document = DdsParser.getInstance().readDocument(file);
            repository.add(document.getId());
            log.debug("verifyAddResults: repo=" + document.getId());
        }

        for (String id : identifiers) {
            log.debug("verifyAddResults: id=" + id);
        }
        if (identifiers.containsAll(repository) && repository.containsAll(identifiers)) {
            return true;
        }

        return false;
    }
}

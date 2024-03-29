/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.discovery;

import jakarta.ws.rs.NotFoundException;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URI;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.agole.AgoleManifestReader;
import net.es.nsi.dds.agole.TopologyManifest;
import net.es.nsi.dds.spring.SpringContext;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class AgoleManifestReaderTest {

  private static final String CONFIG_PATH = "configPath";
  private final static String CONFIG_DIR = "src/test/resources/config/";
  private static final String DEFAULT_DDS_FILE = CONFIG_DIR + "dds.xml";
  private static final String DDS_CONFIG_FILE_ARGNAME = "ddsConfigFile";

  @Test
  public void loadMasterList() {
    System.setProperty(CONFIG_PATH, CONFIG_DIR);
    System.setProperty(DDS_CONFIG_FILE_ARGNAME, DEFAULT_DDS_FILE);

    // Initialize the Spring context to load our dependencies.
    SpringContext sc = SpringContext.getInstance();
    ApplicationContext context = sc.initContext("src/test/resources/config/AgoleManifestReaderTest.xml");
    AgoleManifestReader reader = (AgoleManifestReader) context.getBean("agoleManifestReader");
    reader.setTarget("http://localhost:8801/www/master.xml");

    HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://localhost:8801"));
    StaticHttpHandler staticHttpHandler = new StaticHttpHandler("src/test/resources/config/www/");
    server.getServerConfiguration().addHttpHandler(staticHttpHandler, "/www");

    try {
      server.start();

      // Retrieve a copy of the centralized master topology list.
      TopologyManifest master = reader.getManifest();

      assertTrue(master != null);

      log.debug("Master id: {}, version={}", master.getId(), master.getVersion());

      // Test to see if the Netherlight entry is present.
      assertTrue(master.getTopologyURL("urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed") != null);

      // We should not see a change in version.
      master = reader.getManifestIfModified();

      assertTrue(master == null);
    } catch (IOException | NotFoundException | JAXBException ex) {
      log.error("Failed to load master topology list from: {}", reader.getTarget());
      ex.printStackTrace();
      fail();
    } finally {
      server.shutdownNow();
    }
  }
}

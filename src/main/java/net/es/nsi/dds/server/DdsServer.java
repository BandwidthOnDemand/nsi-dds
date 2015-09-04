package net.es.nsi.dds.server;

import java.io.IOException;
import net.es.nsi.dds.api.DiscoveryService;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.management.api.ManagementService;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DdsServer {
    private static final int FILE_CACHE_MAX_AGE = 3600;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final HttpConfig config;
    private RestServer server = null;

    public DdsServer(DdsConfiguration config) {
        this.config = config.getHttpConfig();
    }

    public static DdsServer getInstance() {
        DdsServer ddsProvider = SpringApplicationContext.getBean("ddsServer", DdsServer.class);
        return ddsProvider;
    }

    public void start() throws IllegalStateException, IOException {
        synchronized(this) {
            if (server == null) {
                if (config.isSecure()) {
                    // Start a HTTPS secure server.
                    server = new RestServer(config.getAddress(), config.getPort(), config.getHttpsConfig().getSSLContext());

                }
                else {
                    // Start an insecure HTTP server.
                    server = new RestServer(config.getAddress(), config.getPort());
                }

                server.addInterface(EncodingFilter.class)
                      .addInterface(GZipEncoder.class)
                      .addInterface(DeflateEncoder.class)
                      .addInterface(DiscoveryService.class)
                      .addInterface(ManagementService.class)
                      .setPackages(config.getPackageName())
                      .setFileCacheMaxAge(FILE_CACHE_MAX_AGE);

                if (config.getStaticPath() != null) {
                    server.setStaticPath(config.getStaticPath())
                          .setRelativePath(config.getRelativePath());
                }

                log.debug("DDSServer.start: Starting Grizzly on " + config.getUrl() + " for resources " + config.getPackageName());
                server.start();

                while (!server.isStarted()) {
                    try {
                        log.debug("DDSServer.start: Waiting for Grizzly to start ...");
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        log.debug("Sleep interupted while waiting for DDS server to start", ex);
                    }
                }
                log.debug("DDSServer.start: Started Grizzly.");
            }
            else {
                log.error("DDSServer.start: Grizzly already started.");
                throw new IllegalStateException();
            }
        }
    }

    public void shutdown() throws IllegalStateException {

        synchronized(this) {
            if (server != null) {
                log.debug("DDSServer.stop: Stopping Grizzly.");
                server.stop();
                server = null;
            }
            else {
                log.error("DDSServer.stop: Grizzly not started.");
                throw new IllegalStateException();
            }
        }
    }

    public String getPackageName() {
        return config.getPackageName();
    }

    public String getUrl() {
        return config.getUrl();
    }
}
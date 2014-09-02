package net.es.nsi.dds.server;

import java.io.IOException;
import net.es.nsi.dds.jersey.RestServer;
import java.net.URI;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.config.http.HttpConfigProvider;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DdsServer {
    public static final String PCE_SERVER_CONFIG_NAME = "pce";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private HttpConfig config;
    private HttpServer server = null;

    public DdsServer(HttpConfigProvider provider) {
        this.config = provider.getConfig(PCE_SERVER_CONFIG_NAME);
    }

    public static DdsServer getInstance() {
        DdsServer pceProvider = SpringApplicationContext.getBean("ddsServer", DdsServer.class);
        return pceProvider;
    }

    public void start() throws IllegalStateException, IOException {
        synchronized(this) {
            if (server == null) {
                try {
                    log.debug("DDSServer.start: Starting Grizzly on " + config.getUrl() + " for resources " + config.getPackageName());
                    server = GrizzlyHttpServerFactory.createHttpServer(URI.create(config.getUrl()), RestServer.getConfig(config.getPackageName()), false);

                    if (config.getStaticPath() != null && !config.getStaticPath().isEmpty()) {
                        StaticHttpHandler staticHttpHandler = new StaticHttpHandler(config.getStaticPath());
                        server.getServerConfiguration().addHttpHandler(staticHttpHandler, config.getWwwPath());
                    }

                    server.start();
                    while (!server.isStarted()) {
                        log.debug("DDSServer.start: Waiting for Grizzly to start ...");
                        Thread.sleep(1000);
                    }
                    log.debug("DDSServer.start: Started Grizzly.");
                } catch (IOException ex) {
                    log.error("Could not start HTTP server.", ex);
                    throw ex;
                } catch (InterruptedException ie) {
                    log.debug("Sleep interupted", ie);
                }
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
                server.shutdownNow();
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
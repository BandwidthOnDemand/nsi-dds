package net.es.nsi.pce.server;

import java.io.IOException;
import net.es.nsi.pce.jersey.RestServer;
import java.net.URI;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.config.http.HttpConfigProvider;
import net.es.nsi.pce.spring.SpringApplicationContext;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEServer {
    public static final String PCE_SERVER_CONFIG_NAME = "pce";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private HttpConfig config;
    private HttpServer server = null;

    public PCEServer(HttpConfigProvider provider) {
        this.config = provider.getConfig(PCE_SERVER_CONFIG_NAME);
    }

    public static PCEServer getInstance() {
        PCEServer pceProvider = SpringApplicationContext.getBean("pceServer", PCEServer.class);
        return pceProvider;
    }

    public void start() throws IllegalStateException, IOException {
        synchronized(this) {
            if (server == null) {
                try {
                    log.debug("PCEServer.start: Starting Grizzly on " + config.getUrl() + " for resources " + config.getPackageName());
                    server = GrizzlyHttpServerFactory.createHttpServer(URI.create(config.getUrl()), RestServer.getConfig(config.getPackageName()), false);

                    if (config.getStaticPath() != null && !config.getStaticPath().isEmpty()) {
                        StaticHttpHandler staticHttpHandler = new StaticHttpHandler(config.getStaticPath());
                        server.getServerConfiguration().addHttpHandler(staticHttpHandler, config.getWwwPath());
                    }

                    server.start();
                    while (!server.isStarted()) {
                        log.debug("PCEServer.start: Waiting for Grizzly to start ...");
                        Thread.sleep(1000);
                    }
                    log.debug("PCEServer.start: Started Grizzly.");
                } catch (IOException ex) {
                    log.error("Could not start HTTP server.", ex);
                    throw ex;
                } catch (InterruptedException ie) {
                    log.debug("Sleep interupted", ie);
                }
            }
            else {
                log.error("PCEServer.start: Grizzly already started.");
                throw new IllegalStateException();
            }
        }
    }

    public void shutdown() throws IllegalStateException {

        synchronized(this) {
            if (server != null) {
                log.debug("PCEServer.stop: Stopping Grizzly.");
                server.shutdownNow();
                server = null;
            }
            else {
                log.error("PCEServer.stop: Grizzly not started.");
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
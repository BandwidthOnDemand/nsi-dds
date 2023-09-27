package net.es.nsi.dds.server;

import com.google.common.base.Strings;
import jakarta.ws.rs.ProcessingException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.authorization.SecurityFilter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.Optional;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class RestServer {
    private HttpServer server = null;
    private SSLContext sslContext;
    private String address;
    private String port;
    private String packages;
    private String staticPath;
    private String relativePath;
    private boolean secure = false;
    private int fileCacheMaxAge = 3600;
    private final List<Class<?>> interfaces = new ArrayList<>();

    public RestServer(String address, String port, SSLContext sslContext) {
        log.debug("RestServer: creating secure HTTPS server.");

        // Validate address parameter is not empty.
        if (Optional.ofNullable(Strings.emptyToNull(address)).isPresent()) {
            this.address = address;
        } else {
            throw new IllegalArgumentException("RestServer: Must provide address");
        }

        // Validate port parameter is not empty.
        if (Optional.ofNullable(Strings.emptyToNull(port)).isPresent()) {
            this.port = port;
        } else {
            throw new IllegalArgumentException("RestServer: Must provide port");
        }

        // Validate an sslContext parameter was provided.
        if (Optional.ofNullable(sslContext).isPresent()) {
            this.sslContext = sslContext;
        } else {
            throw new IllegalArgumentException("RestServer: Must provide sslContext");
        }

        secure = true;
    }

    public RestServer(String address, String port) {
        log.debug("RestServer: creating unsecure HTTP server.");

        // Validate address parameter is not empty.
        if (Optional.ofNullable(Strings.emptyToNull(address)).isPresent()) {
            this.address = address;
        } else {
            throw new IllegalArgumentException("RestServer: Must provide address");
        }

        // Validate port parameter is not empty.
        if (Optional.ofNullable(Strings.emptyToNull(port)).isPresent()) {
            this.port = port;
        } else {
            throw new IllegalArgumentException("RestServer: Must provide port");
        }
    }

    private ResourceConfig getResourceConfig() {
        ResourceConfig rs = new ResourceConfig();

        // Validate address parameter is not empty.
        if (Optional.ofNullable(Strings.emptyToNull(packages)).isPresent()) {
            log.debug("RestServer: adding packages {}", packages);
            rs.packages(packages);
        }

        // This will register any interfaces added to the web server.
        for (Class<?> intf : this.getInterfaces()) {
            log.debug("RestServer: adding interface " + intf.getCanonicalName());
            rs.register(intf);
        }

        // Add Moxy support for XML and JSON.
        //rs.register(new MoxyXmlFeature());
        //rs.register(new MoxyJsonFeature());

        // Register the loggers for container tracing.
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(RestServer.class.getName());
        rs.register(new LoggingFeature(logger, Level.FINE, LoggingFeature.Verbosity.PAYLOAD_ANY, 10000));

        // Register the security filter.
        return rs.registerClasses(SecurityFilter.class);
    }

    private URI getServerURI() {
        if (secure) {
            return URI.create("https://" + address + ":" + port);
        }

        return URI.create("http://" + address + ":" + port);
    }

    public boolean start() throws ProcessingException, IOException {
        if (server == null) {
            try {
                if (secure) {
                    log.debug("RestServer: Creating secure server on {}", getServerURI());
                    server = GrizzlyHttpServerFactory.createHttpServer(getServerURI(), getResourceConfig(), true,
                        new SSLEngineConfigurator(sslContext).setNeedClientAuth(true).setClientMode(false),
                        false);
                    log.debug("RestServer: Created secure server on {}", getServerURI());
                } else {
                    log.debug("RestServer: Creating server on {}", getServerURI());
                    server = GrizzlyHttpServerFactory.createHttpServer(getServerURI(), getResourceConfig(), false);
                    log.debug("RestServer: Created server on {}", getServerURI());
                }
            } catch (Exception ex) {
                log.error("RestServer: Failed to create server", ex);
                return false;
            }

            NetworkListener listener = server.getListener("grizzly");
            server.getServerConfiguration()
                .setMaxBufferedPostSize(server.getServerConfiguration().getMaxBufferedPostSize()*10);

            if (Optional.ofNullable(Strings.emptyToNull(staticPath)).isPresent() &&
                Optional.ofNullable(Strings.emptyToNull(relativePath)).isPresent()) {
              StaticHttpHandler staticHttpHandler = new StaticHttpHandler(staticPath);
              server.getServerConfiguration().addHttpHandler(staticHttpHandler, relativePath);
              if (listener != null) {
                  listener.getFileCache().setSecondsMaxAge(fileCacheMaxAge);
              }
            }
        }

        if (!server.isStarted()) {
            try {
                log.debug("RestServer: starting server.");
                server.start();
            } catch (IOException ex) {
                log.error("Failed to start HTTP Server", ex);
                throw ex;
            }
        }

        return true;
    }

    public boolean isStarted() {
        if (server != null) {
            return server.isStarted();
        }

        return false;
    }

    public void stop() {
        if (server != null && server.isStarted()) {
            log.debug("RestServer: Stopping server.");
            server.shutdownNow();
        }
    }

    /**
     * @return the secure
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * @param secure the secure to set
     * @return
     */
    public RestServer setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    /**
     * @return the sslContext
     */
    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * @param sslContext the sslContext to set
     * @return
     */
    public RestServer setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address the address to set
     * @return
     */
    public RestServer setAddress(String address) throws IllegalArgumentException {
        // Validate address parameter is not empty.
        if (Optional.ofNullable(Strings.emptyToNull(address)).isPresent()) {
            this.address = address;
        } else {
            throw new IllegalArgumentException("RestServer: Must provide address");
        }
        return this;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @param port the port to set
     * @return
     */
    public RestServer setPort(String port) throws IllegalArgumentException {
        // Validate address parameter is not empty.
        if (Optional.ofNullable(Strings.emptyToNull(port)).isPresent()) {
            this.port = port;
        } else {
            throw new IllegalArgumentException("RestServer: Must provide port");
        }
        return this;
    }

    /**
     * @return the packages
     */
    public String getPackages() {
        return packages;
    }

    /**
     * @param packages the packages to set
     * @return
     */
    public RestServer setPackages(String packages) {
        // Validate address parameter is not empty.
        if (Optional.ofNullable(Strings.emptyToNull(packages)).isPresent()) {
            this.packages = packages;
        } else {
            throw new IllegalArgumentException("RestServer: Must provide packages");
        }
        return this;
    }

    public List<Class<?>> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    public RestServer addInterface(Class<?> intf) {
        interfaces.add(intf);
        return this;
    }

    public void clearInterfaces() {
        interfaces.clear();
    }

    public HttpServer getServer() {
        return server;
    }

    public RestServer setFileCacheMaxAge(int fileCacheMaxAge) {
        this.fileCacheMaxAge = fileCacheMaxAge;
        return this;
    }

    public RestServer setStaticPath(String path) {
        staticPath = Strings.emptyToNull(path);
        return this;
    }

    public String getStaticPath() {
        return staticPath;
    }

    public RestServer setRelativePath(String path) {
        relativePath = Strings.emptyToNull(path);
        return this;
    }

    public String getRelativePath() {
        return relativePath;
    }
}

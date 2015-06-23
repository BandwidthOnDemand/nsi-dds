/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.server;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.SSLContext;
import net.es.nsi.dds.authorization.SecurityFilter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RestServer {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Optional<HttpServer> server = Optional.absent();
    private Optional<SSLContext> sslContext = Optional.absent();
    private Optional<String> address = Optional.absent();
    private Optional<String> port = Optional.absent();
    private Optional<String> packages = Optional.absent();
    private Optional<String> staticPath = Optional.absent();
    private Optional<String> relativePath = Optional.absent();
    private boolean secure = false;
    private int fileCacheMaxAge = 3600;
    private final List<Class<?>> interfaces = new ArrayList<>();

    public RestServer(String address, String port, SSLContext sslContext) {
        log.debug("RestServer: creating secure HTTPS server.");

        this.address = Optional.fromNullable(Strings.emptyToNull(address));
        if (!this.address.isPresent()) {
            throw new IllegalArgumentException("RestServer: Must provide address");
        }

        this.port = Optional.fromNullable(Strings.emptyToNull(port));
        if (!this.port.isPresent()) {
            throw new IllegalArgumentException("RestServer: Must provide port");
        }

        this.sslContext = Optional.fromNullable(sslContext);
        if (!this.sslContext.isPresent()) {
            throw new IllegalArgumentException("RestServer: Must provide SslConfigurator");
        }

        secure = true;
    }

    public RestServer(String address, String port) {
        log.debug("RestServer: creating unsecure HTTP server.");

        this.address = Optional.fromNullable(Strings.emptyToNull(address));
        if (!this.address.isPresent()) {
            throw new IllegalArgumentException("RestServer: Must provide address");
        }

        this.port = Optional.fromNullable(Strings.emptyToNull(port));
        if (!this.port.isPresent()) {
            throw new IllegalArgumentException("RestServer: Must provide port");
        }

        secure = false;
    }

    private ResourceConfig getResourceConfig() {
        ResourceConfig rs = new ResourceConfig();

        if (packages.isPresent()) {
            log.debug("RestServer: adding packages " + packages.get());
            rs.packages(packages.get());
        }

        for (Class<?> intf : this.getInterfaces()) {
            log.debug("RestServer: adding interface " + intf.getCanonicalName());
            rs.register(intf);
        }

        return rs.registerClasses(SecurityFilter.class)
                .register(new MoxyXmlFeature())
                .register(new LoggingFilter(java.util.logging.Logger.getLogger(RestServer.class.getName()), true));
    }

    private URI getServerURI() {
        if (secure) {
            return URI.create("https://" + address.get() + ":" + port.get());
        }

        return URI.create("http://" + address.get() + ":" + port.get());
    }

    public boolean start() throws IOException {
        if (!server.isPresent()) {
            if (secure) {
                log.debug("RestServer: Creating secure server.");
                server = Optional.of(GrizzlyHttpServerFactory.createHttpServer(
                    getServerURI(), getResourceConfig(), true,
                    new SSLEngineConfigurator(sslContext.get())
                        .setNeedClientAuth(true).setClientMode(false)));
            }
            else {
                log.debug("RestServer: Creating server.");
                server = Optional.of(GrizzlyHttpServerFactory.createHttpServer(
                    getServerURI(), getResourceConfig()));
            }


            NetworkListener listener = server.get().getListener("grizzly");
            server.get().getServerConfiguration().setMaxBufferedPostSize(server.get().getServerConfiguration().getMaxBufferedPostSize()*10);

            if (staticPath.isPresent()) {
                StaticHttpHandler staticHttpHandler = new StaticHttpHandler(staticPath.get());
                server.get().getServerConfiguration().addHttpHandler(staticHttpHandler, relativePath.get());
                if (listener != null) {
                    listener.getFileCache().setSecondsMaxAge(fileCacheMaxAge);
                }
            }
        }

        if (!server.get().isStarted()) {
            try {
                log.debug("RestServer: starting server.");
                server.get().start();
            } catch (IOException ex) {
                log.error("Failed to start HTTP Server", ex);
                throw ex;
            }
        }


        return true;
    }

    public boolean isStarted() {
        if (server.isPresent()) {
            return server.get().isStarted();
        }

        return false;
    }

    public void stop() {
        if (server.isPresent() && server.get().isStarted()) {
            log.debug("RestServer: Stopping server.");
            server.get().shutdownNow();
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
        return sslContext.orNull();
    }

    /**
     * @param sslContext the sslContext to set
     * @return
     */
    public RestServer setSslContext(SSLContext sslContext) {
        this.sslContext = Optional.fromNullable(sslContext);
        return this;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address.get();
    }

    /**
     * @param address the address to set
     * @return
     */
    public RestServer setAddress(String address) throws IllegalArgumentException {
        this.address = Optional.fromNullable(Strings.emptyToNull(address));
        if (!this.address.isPresent()) {
            throw new IllegalArgumentException("RestServer: Must provide address");
        }
        return this;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port.get();
    }

    /**
     * @param port the port to set
     * @return
     */
    public RestServer setPort(String port) throws IllegalArgumentException {
        this.port = Optional.fromNullable(Strings.emptyToNull(port));
        if (!this.port.isPresent()) {
            throw new IllegalArgumentException("RestServer: Must provide port");
        }
        return this;
    }

    /**
     * @return the packages
     */
    public String getPackages() {
        return packages.orNull();
    }

    /**
     * @param packages the packages to set
     * @return
     */
    public RestServer setPackages(String packages) {
        this.packages = Optional.fromNullable(Strings.emptyToNull(packages));
        if (!this.packages.isPresent()) {
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
        return server.orNull();
    }

    public RestServer setFileCacheMaxAge(int fileCacheMaxAge) {
        this.fileCacheMaxAge = fileCacheMaxAge;
        return this;
    }

    public RestServer setStaticPath(String path) {
        staticPath = Optional.fromNullable(Strings.emptyToNull(path));
        return this;
    }

    public String getStaticPath() {
        return staticPath.orNull();
    }

    public RestServer setRelativePath(String path) {
        relativePath = Optional.fromNullable(Strings.emptyToNull(path));
        return this;
    }

    public String getRelativePath() {
        return relativePath.orNull();
    }
}

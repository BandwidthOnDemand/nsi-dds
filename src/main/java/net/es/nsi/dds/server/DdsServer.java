package net.es.nsi.dds.server;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import net.es.nsi.dds.api.DiscoveryService;
import net.es.nsi.dds.api.Portal;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.management.api.ManagementService;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.filter.EncodingFilter;

/**
 * Class implementing the NSI DDS protocol server.
 *
 * @author hacksaw
 */
public class DdsServer {
    private static final int FILE_CACHE_MAX_AGE = 3600;

    private final Logger log = LogManager.getLogger(getClass());
    private final HttpConfig http;
    private RestServer server = null;

    /**
     * Construct an NSI DDS server using the specified configuration.
     *
     * @param ddsConfig
     * @throws java.io.IOException
     * @throws java.security.KeyManagementException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyStoreException
     * @throws java.security.NoSuchProviderException
     * @throws java.security.cert.CertificateException
     * @throws java.security.UnrecoverableKeyException
     */
    public DdsServer(DdsConfiguration ddsConfig) throws IOException, KeyManagementException, NoSuchAlgorithmException,
            KeyStoreException, NoSuchProviderException, CertificateException, UnrecoverableKeyException {
        http = new HttpConfig(ddsConfig.getServerConfig());
    }

    /**
     * Get a reference to the singleton bean representing the NSI DDS server.
     *
     * @return
     */
    public static DdsServer getInstance() {
        DdsServer ddsProvider = SpringApplicationContext.getBean("ddsServer", DdsServer.class);
        return ddsProvider;
    }

    /**
     * Start the NSI DDS protocol server.
     *
     * @throws IllegalStateException
     * @throws IOException
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     */
    public void start() throws IllegalStateException, IOException, KeyManagementException, NoSuchAlgorithmException,
            NoSuchProviderException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        synchronized(this) {
            if (server == null) {
                if (http.isSecure()) {
                    // Start a HTTPS secure server.
                    server = new RestServer(http.getAddress(), http.getPort(), http.getHttpsContext().getSSLContext());

                }
                else {
                    // Start an insecure HTTP server.
                    server = new RestServer(http.getAddress(), http.getPort());
                }

               server.addInterface(EncodingFilter.class)
                      .addInterface(GZipEncoder.class)
                      .addInterface(DeflateEncoder.class)
                      .addInterface(DiscoveryService.class)
                      .addInterface(Portal.class)
                      .addInterface(ManagementService.class)
                      .setPackages(http.getPackageName())
                      .setFileCacheMaxAge(FILE_CACHE_MAX_AGE);

                if (http.getStaticPath() != null) {
                    server.setStaticPath(http.getStaticPath())
                          .setRelativePath(http.getRelativePath());
                }

                log.debug("DDSServer.start: Starting Grizzly on " + http.getUrl() + " for resources " + http.getPackageName());
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

    /**
     * Shutdown the NSI DDS server.
     * @throws IllegalStateException
     */
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

    /**
     * Return the package name of class implementing the NSI DDS server
     * @return
     */
    public String getPackageName() {
        return http.getPackageName();
    }

    /**
     * Get the HTTP(s) URL for the NSI DDS server.
     *
     * @return
     */
    public String getUrl() {
        return http.getUrl();
    }
}
package net.es.nsi.dds.client;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.net.ssl.HostnameVerifier;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.dds.config.http.HttpsConfig;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.spring.SpringApplicationContext;
import net.es.nsi.dds.util.NsiConstants;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The singleton class (bean) provides the DDS server's REST client for communication with remote DDS servers.
 *
 * @author hacksaw
 */
public class RestClient {
    private final static Logger log = LoggerFactory.getLogger(RestClient.class);
    private final Client client;

    // Time for idle data timeout.
    private final static String TCP_SO_TIMEOUT = "tcpSoTimeout";
    private final static int SO_TIMEOUT = 60 * 1000;

    // Time for the socket to connect.
    private final static String TCP_CONNECT_TIMEOUT = "tcpConnectTimeout";
    private final static int CONNECT_TIMEOUT = 20 * 1000;

    // Time to block for a socket from the connection manager.
    private final static String TCP_CONNECT_REQUEST_TIMEOUT = "tcpConnectRequestTimeout";
    private final static int CONNECT_REQUEST_TIMEOUT = 30 * 1000;

    // Connection provider pool configuration defaults.
    private final static int MAX_CONNECTION_PER_ROUTE = 5;
    private final static int MAX_CONNECTION_TOTAL = 50;

    /**
     * Default constructor uses default configuration values.
     */
    public RestClient() {
        ClientConfig clientConfig = configureClient(MAX_CONNECTION_PER_ROUTE, MAX_CONNECTION_TOTAL);
        client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
        client.property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_SERVER, "DEBUG");
    }

    /**
     * This constructor accepts a specific HttpsConfig.
     *
     * @param config The HttpsConfig to apply to the RestClient.
     *
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     */
    public RestClient(HttpsConfig config) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        ClientConfig clientConfig = configureSecureClient(config);
        client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
    }

    /**
     * This constructor accepts a full DDS configuration document.
     *
     * @param config The DDS configuration document.
     *
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     */
    public RestClient(DdsConfiguration config) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        HttpsConfig cf = config.getClientConfig();
        if (cf == null) {
            ClientConfig clientConfig = configureClient(MAX_CONNECTION_PER_ROUTE, MAX_CONNECTION_TOTAL);
            client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
        }
        else {
            ClientConfig clientConfig = configureSecureClient(cf);
            client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
        }
    }

    /**
     * We treat this RestClient as a singleton and this method provides the instance.
     *
     * @return instance of this singleton.
     */
    public static RestClient getInstance() {
        RestClient restClient = SpringApplicationContext.getBean("restClient", RestClient.class);
        return restClient;
    }


    /**
     * Configure the client for TLS communications.
     *
     * @param config The HttpsConfig object providing SLL/TLS configuration information.
     * @return a client configuration.
     */
    public static ClientConfig configureSecureClient(HttpsConfig config) {
        HostnameVerifier hostnameVerifier;
        if (config.isProduction()) {
            hostnameVerifier = new DefaultHostnameVerifier();
        }
        else {
            hostnameVerifier = new NoopHostnameVerifier();
        }

        final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new PlainConnectionSocketFactory())
                .register("https", new SSLConnectionSocketFactory(config.getSSLContext(), hostnameVerifier))
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        return getClientConfig(connectionManager, config.getMaxConnPerRoute(), config.getMaxConnTotal());
    }

    /**
     * Configure the client for insecure communications.
     *
     * @param maxPerRoute The max connections per destination.
     * @param maxTotal The max connection total across all destinations.
     * @return
     */
    public static ClientConfig configureClient(int maxPerRoute, int maxTotal) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        return getClientConfig(connectionManager, maxPerRoute, maxTotal);
    }

    /**
     * Configure the default client configuration.
     *
     * @return The default client configuration.
     */
    public static ClientConfig configureClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        return getClientConfig(connectionManager, MAX_CONNECTION_PER_ROUTE, MAX_CONNECTION_TOTAL);
    }

    /**
     * Creates a client configuration based on the provided configuration.
     *
     * @param connectionManager Connection manager used to configure the client configuration.
     * @param maxPerRoute  The max connections per destination.
     * @param maxTotal The max connection total across all destinations.
     * @return The new client configuration.
     */
    public static ClientConfig getClientConfig(PoolingHttpClientConnectionManager connectionManager,
                                               int maxPerRoute, int maxTotal) {
        ClientConfig clientConfig = new ClientConfig();

        // We want to use the Apache connector for chunk POST support.
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        connectionManager.setDefaultMaxPerRoute(maxPerRoute);
        connectionManager.setMaxTotal(maxTotal);
        connectionManager.closeIdleConnections(30, TimeUnit.SECONDS);
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);

        clientConfig.register(GZipEncoder.class);
        clientConfig.register(new MoxyXmlFeature());
        clientConfig.register(new LoggingFeature(java.util.logging.Logger.getGlobal(), Level.ALL,
                LoggingFeature.Verbosity.PAYLOAD_ANY, 10000));
        clientConfig.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);

        // Apache specific configuration.
        RequestConfig.Builder custom = RequestConfig.custom();
        custom.setExpectContinueEnabled(true);
        custom.setRelativeRedirectsAllowed(true);
        custom.setRedirectsEnabled(true);
        custom.setSocketTimeout(Integer.parseInt(System.getProperty(TCP_SO_TIMEOUT, Integer.toString(SO_TIMEOUT))));
        custom.setConnectTimeout(Integer.parseInt(System.getProperty(TCP_CONNECT_TIMEOUT, Integer.toString(CONNECT_TIMEOUT))));
        custom.setConnectionRequestTimeout(Integer.parseInt(System.getProperty(TCP_CONNECT_REQUEST_TIMEOUT, Integer.toString(CONNECT_REQUEST_TIMEOUT))));
        clientConfig.property(ApacheClientProperties.REQUEST_CONFIG, custom.build());

        return clientConfig;
    }

    /**
     * Getter returning the HTTP client.
     *
     * @return The HTTP client.
     */
    public Client get() {
        return client;
    }

    /**
     * Close the associated HTTP client.
     */
    public void close() {
        client.close();
    }

    /**
     * Filter to allow for an HTTP redirect on an HTTP operation.
     */
    private static class FollowRedirectFilter implements ClientResponseFilter
    {
        private final static Logger log = LoggerFactory.getLogger(FollowRedirectFilter.class);

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException
        {
            if (requestContext == null || responseContext == null || responseContext.getStatus() != Response.Status.FOUND.getStatusCode()) {
               return;
            }

            log.debug("Processing redirect for " + requestContext.getMethod() + " " + requestContext.getUri().toASCIIString() + " to " + responseContext.getLocation().toASCIIString());

            Client inClient = requestContext.getClient();
            Object entity = requestContext.getEntity();
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            String method = requestContext.getMethod();
            Response resp;
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                resp = inClient.target(responseContext.getLocation())
                        .request(requestContext.getMediaType())
                        .headers(headers)
                        .method(requestContext.getMethod(), Entity.entity(new GenericEntity<JAXBElement<?>>((JAXBElement<?>)entity) {}, NsiConstants.NSI_DDS_V1_XML));
            }
            else {
                resp = inClient.target(responseContext.getLocation())
                        .request(requestContext.getMediaType())
                        .headers(headers)
                        .method(requestContext.getMethod());
            }

            responseContext.setEntityStream((InputStream) resp.getEntity());
            responseContext.setStatusInfo(resp.getStatusInfo());
            responseContext.setStatus(resp.getStatus());
            responseContext.getHeaders().putAll(resp.getStringHeaders());

            log.debug("Processing redirect with result " + resp.getStatus());
        }
    }
}

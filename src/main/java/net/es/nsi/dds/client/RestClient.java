package net.es.nsi.dds.client;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
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
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
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
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
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

    public RestClient() {
        ClientConfig clientConfig = configureClient();
        client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
    }

    public RestClient(HttpsConfig config) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        ClientConfig clientConfig = configureSecureClient(config);
        client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
    }

    public RestClient(DdsConfiguration config) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        HttpsConfig cf = config.getClientConfig();
        if (cf == null) {
            ClientConfig clientConfig = configureClient();
            client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
        }
        else {
            ClientConfig clientConfig = configureSecureClient(cf);
            client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
        }
    }

    public static RestClient getInstance() {
        RestClient restClient = SpringApplicationContext.getBean("restClient", RestClient.class);
        return restClient;
    }

    public static ClientConfig configureSecureClient(HttpsConfig config) {
        HostnameVerifier hostnameVerifier;
        if (config.isProduction()) {
            hostnameVerifier = new DefaultHostnameVerifier();
        }
        else {
            hostnameVerifier = new NoopHostnameVerifier();
        }

        SSLContext sslContext = config.getSSLContext();
        LayeredConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        PlainConnectionSocketFactory socketFactory = PlainConnectionSocketFactory.getSocketFactory();

        final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", socketFactory)
                .register("https", sslSocketFactory)
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        return getClientConfig(connectionManager);
    }

    public static ClientConfig configureClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        return getClientConfig(connectionManager);
    }

    public static ClientConfig getClientConfig(PoolingHttpClientConnectionManager connectionManager) {
        ClientConfig clientConfig = new ClientConfig();

        // We want to use the Apache connector for chunk POST support.
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.setMaxTotal(80);
        connectionManager.closeIdleConnections(30, TimeUnit.SECONDS);
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);

        clientConfig.register(GZipEncoder.class);
        clientConfig.register(new MoxyXmlFeature());
        clientConfig.register(new LoggingFilter(java.util.logging.Logger.getGlobal(), true));
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

    public Client get() {
        return client;
    }

    public void close() {
        client.close();
    }

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
                resp = inClient.target(responseContext.getLocation()).request(requestContext.getMediaType()).headers(headers).method(requestContext.getMethod(), Entity.entity(new GenericEntity<JAXBElement<?>>((JAXBElement<?>)entity) {}, NsiConstants.NSI_DDS_V1_XML));
            }
            else {
                resp = inClient.target(responseContext.getLocation()).request(requestContext.getMediaType()).headers(headers).method(requestContext.getMethod());
            }

            responseContext.setEntityStream((InputStream) resp.getEntity());
            responseContext.setStatusInfo(resp.getStatusInfo());
            responseContext.setStatus(resp.getStatus());
            responseContext.getHeaders().putAll(resp.getStringHeaders());

            log.debug("Processing redirect with result " + resp.getStatus());
        }
    }
}

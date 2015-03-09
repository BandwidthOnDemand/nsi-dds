package net.es.nsi.dds.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
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
import net.es.nsi.dds.schema.NsiConstants;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.ResourceUtils;

/**
 *
 * @author hacksaw
 */
public class RestClient {
    private final static Logger log = LoggerFactory.getLogger(RestClient.class);
    private final Client client;

    public RestClient() {
        ClientConfig clientConfig = configureClient();
        client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
    }

    public RestClient(boolean secure) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        if (secure) {
            ClientConfig clientConfig = configureSecureClient();
            client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
        }
        else {
            ClientConfig clientConfig = configureClient();
            client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
        }
    }

    public static RestClient getInstance() {
        RestClient restClient = SpringApplicationContext.getBean("restClient", RestClient.class);
        return restClient;
    }

    public static ClientConfig configureSecureClient() {
        HostnameVerifier defaultHostnameVerifier = new DefaultHostnameVerifier();

        SSLContext sslContext = getSSLContext();
        LayeredConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext, defaultHostnameVerifier);

        final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
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
        clientConfig.property(ApacheClientProperties.REQUEST_CONFIG, custom.build());

        return clientConfig;
    }

    private final static String SSL_KEYSTORE = "javax.net.ssl.keyStore";
    private final static String SSL_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    private final static String SSL_TRUSTSTORE = "javax.net.ssl.trustStore";
    private final static String SSL_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";

    private static SSLContext getSSLContext() {
        String keyStore = System.getProperty(SSL_KEYSTORE, null);
        String keyStorePassword = System.getProperty(SSL_KEYSTORE_PASSWORD, null);
        String trustStore = System.getProperty(SSL_TRUSTSTORE, null);
        String trustStorePassword = System.getProperty(SSL_TRUSTSTORE_PASSWORD, null);

        if (keyStore == null) {
            return null;
        }

        String defaultType = KeyStore.getDefaultType();
        log.debug("Keystore default type " + defaultType);
/*
        try {
            KeyStore ks = createKeyManagerKeyStore("JKS", keyStore, keyStorePassword);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keyStorePassword == null ? null : keyStorePassword.toCharArray());


            //CustomKeyManager km = configureKeystore("JKS", keyStore, keyStorePassword);


            KeyStore ts = createKeyManagerKeyStore("JKS", trustStore, trustStorePassword);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
            tmf.init(ts);

            SSLContext sslContext = SSLContext.getInstance("SSL");
            //sslContext.init(createCustomKeyManangers(km), tmf.getTrustManagers(), null);
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext;
        }
        catch (Exception ex) {
            log.error("Failed to initialize k:q:qey and trust stores.", ex);
        }
*/
        SslConfigurator sslConfig = SslConfigurator.newInstance()
            .trustStoreFile(trustStore)
            .trustStorePassword(trustStorePassword)
            .keyStoreFile(keyStore)
            .keyPassword(keyStorePassword)
            .securityProtocol("TLS");

        return sslConfig.createSSLContext();
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

    private static KeyStore createKeyManagerKeyStore(String keyStoreType, String keyStore, String keyStorePassword) throws Exception {
        if (keyStoreType == null || keyStore == null) {
            return null;
        }

        KeyStore ks = KeyStore.getInstance(keyStoreType);
        try (InputStream is = resourceFromString(keyStore).getInputStream()) {
            ks.load(is, keyStorePassword == null ? null : keyStorePassword.toCharArray());
        }
        return ks;
    }

    public static Resource resourceFromString(String uri) throws MalformedURLException {
        Resource resource;
        File file = new File(uri);
        if (file.exists()) {
            resource = new FileSystemResource(uri);
        }
        else if (ResourceUtils.isUrl(uri)) {
            resource = new UrlResource(uri);
        }
        else {
            resource = new ClassPathResource(uri);
        }
        return resource;
    }
}

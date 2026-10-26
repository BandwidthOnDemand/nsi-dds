package net.es.nsi.dds.client;

import com.google.common.base.Throwables;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import net.es.nsi.dds.config.http.HttpsContext;
import net.es.nsi.dds.jaxb.configuration.ClientType;
import net.es.nsi.dds.jaxb.configuration.KeyStoreType;
import net.es.nsi.dds.jaxb.configuration.SecureType;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.glassfish.jersey.apache5.connector.Apache5ClientProperties;
import org.glassfish.jersey.apache5.connector.Apache5ConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Exercises the Apache 5 client configuration against a local HTTP and HTTPS server.
 */
public class RestClientTest {
    private static final String CONFIG_DIR = "src/test/resources/config/";
    private static final String BODY = "<nsi>hello</nsi>";

    private static HttpServer http;
    private static HttpsServer https;
    private static final Map<String, String> lastRequestHeader = new ConcurrentHashMap<>();
    private static volatile String lastRequestBody;
    private static final CountDownLatch release = new CountDownLatch(1);

    @BeforeClass
    public static void startServers() throws Exception {
        http = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        // One thread per exchange, so the held /slow request does not block the others.
        http.setExecutor(Executors.newCachedThreadPool());
        http.createContext("/gzip", RestClientTest::gzipResponse);
        http.createContext("/plain", exchange -> respond(exchange, 200, BODY));
        http.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/plain");
            respond(exchange, 302, "");
        });
        http.createContext("/echo", RestClientTest::echoRequest);
        http.createContext("/slow", exchange -> {
            try {
                release.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, BODY);
        });
        http.start();

        // The test server certificate is CN=nsi with no SAN, so "localhost" never matches it.
        HttpsContext.getInstance().load(secureType(true, "truststore.jks", "server.jks"));
        https = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        https.setHttpsConfigurator(new HttpsConfigurator(HttpsContext.getInstance().getSSLContext()));
        https.createContext("/plain", exchange -> respond(exchange, 200, BODY));
        https.start();
    }

    @AfterClass
    public static void stopServers() {
        release.countDown();
        http.stop(0);
        https.stop(0);
    }

    @Test
    public void defaultClientConfiguresPoolAndRequest() {
        ClientConfig config = RestClient.configureClient();

        assertTrue(config.getConnectorProvider() instanceof Apache5ConnectorProvider);

        PoolingHttpClientConnectionManager manager =
                (PoolingHttpClientConnectionManager) config.getProperty(Apache5ClientProperties.CONNECTION_MANAGER);
        assertEquals(10, manager.getDefaultMaxPerRoute());
        assertEquals(80, manager.getMaxTotal());

        RequestConfig request = (RequestConfig) config.getProperty(Apache5ClientProperties.REQUEST_CONFIG);
        assertFalse(request.isContentCompressionEnabled());
        assertTrue(request.isExpectContinueEnabled());
        assertTrue(request.isRedirectsEnabled());
        assertEquals(Timeout.ofSeconds(30), request.getConnectionRequestTimeout());
    }

    @Test
    public void clientTypeSetsPoolLimits() throws Exception {
        PoolingHttpClientConnectionManager manager = (PoolingHttpClientConnectionManager)
                RestClient.configureClient(clientType(3, false)).getProperty(Apache5ClientProperties.CONNECTION_MANAGER);
        assertEquals(3, manager.getDefaultMaxPerRoute());
        assertEquals(3, manager.getMaxTotal());
    }

    @Test
    public void connectionRequestTimeoutComesFromSystemProperty() {
        System.setProperty("tcpConnectRequestTimeout", "1234");
        try {
            RequestConfig request = (RequestConfig)
                    RestClient.configureClient().getProperty(Apache5ClientProperties.REQUEST_CONFIG);
            assertEquals(Timeout.ofMilliseconds(1234), request.getConnectionRequestTimeout());
        } finally {
            System.clearProperty("tcpConnectRequestTimeout");
        }
    }

    @Test
    public void socketTimeoutComesFromSystemProperty() {
        System.setProperty("tcpSoTimeout", "200");
        try (Client client = ClientBuilder.newClient(RestClient.configureClient());
             Response response = client.target(url("/slow")).request().get()) {
            fail("expected a read timeout, got HTTP " + response.getStatus());
        } catch (ProcessingException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().toLowerCase().contains("timed out"));
        } finally {
            System.clearProperty("tcpSoTimeout");
        }
    }

    @Test
    public void connectTimeoutComesFromSystemProperty() {
        System.setProperty("tcpConnectTimeout", "200");
        long start = System.currentTimeMillis();
        // A non-routable address never answers the SYN, so only the connect timeout can end the call.
        try (Client client = ClientBuilder.newClient(RestClient.configureClient());
             Response response = client.target("http://10.255.255.1/").request().get()) {
            fail("expected a connect timeout, got HTTP " + response.getStatus());
        } catch (ProcessingException ex) {
            long elapsed = System.currentTimeMillis() - start;
            assertTrue("took " + elapsed + " ms", elapsed < 5000);
        } finally {
            System.clearProperty("tcpConnectTimeout");
        }
    }

    @Test
    public void gzipResponseIsDecodedOnce() {
        try (Client client = ClientBuilder.newClient(RestClient.configureClient());
             Response response = client.target(url("/gzip")).request().get()) {
            assertEquals(200, response.getStatus());
            assertEquals(BODY, response.readEntity(String.class));
            assertTrue(lastRequestHeader.get("accept-encoding").contains("gzip"));
        }
    }

    @Test
    public void gzipRequestHeaderIsDroppedAndBodySentPlain() {
        // The actors set Content-Encoding: gzip on POSTs; Jersey 3 and 4 both drop it and send
        // the body unencoded. Pinned so a change on either side shows up here.
        try (Client client = ClientBuilder.newClient(RestClient.configureClient());
             Response response = client.target(url("/echo")).request()
                     .header(HttpHeaders.CONTENT_ENCODING, "gzip")
                     .post(Entity.entity(BODY, "application/xml"))) {
            assertEquals(200, response.getStatus());
            assertNull(lastRequestHeader.get("content-encoding"));
            assertEquals(BODY, lastRequestBody);
        }
    }

    @Test
    public void redirectIsFollowed() {
        try (Client client = ClientBuilder.newClient(RestClient.configureClient());
             Response response = client.target(url("/redirect")).request().get()) {
            assertEquals(200, response.getStatus());
            assertEquals(BODY, response.readEntity(String.class));
        }
    }

    @Test
    public void unreadResponsesReleaseConnectionsOnClose() throws Exception {
        System.setProperty("tcpConnectRequestTimeout", "2000");
        try (Client client = ClientBuilder.newClient(RestClient.configureClient(clientType(2, false)))) {
            // More requests than the pool holds: each close must return its connection.
            for (int i = 0; i < 10; i++) {
                try (Response response = client.target(url("/plain")).request().get()) {
                    assertEquals(200, response.getStatus());
                }
            }
        } finally {
            System.clearProperty("tcpConnectRequestTimeout");
        }
    }

    @Test
    public void productionClientRejectsHostnameMismatch() throws Exception {
        try (Client client = secureClient(true, "server.jks");
             Response response = client.target(httpsUrl()).request().get()) {
            fail("expected hostname verification to fail, got HTTP " + response.getStatus());
        } catch (ProcessingException ex) {
            assertTrue(String.valueOf(ex), hasCause(ex, SSLPeerUnverifiedException.class));
        }
    }

    @Test
    public void nonProductionClientSkipsHostnameCheck() throws Exception {
        try (Client client = secureClient(false, "server.jks");
             Response response = client.target(httpsUrl()).request().get()) {
            assertEquals(200, response.getStatus());
            assertEquals(BODY, response.readEntity(String.class));
        }
    }

    @Test
    public void nonProductionClientStillChecksTrust() throws Exception {
        // The trust store holds the client certificate only, so the server is untrusted.
        try (Client client = secureClient(false, "truststore.jks");
             Response response = client.target(httpsUrl()).request().get()) {
            fail("expected an untrusted certificate, got HTTP " + response.getStatus());
        } catch (ProcessingException ex) {
            assertTrue(String.valueOf(ex), hasCause(ex, SSLHandshakeException.class));
        }
    }

    private static Client secureClient(boolean production, String trustStore) throws Exception {
        HttpsContext.getInstance().load(secureType(production, trustStore, "client.jks"));
        return ClientBuilder.newClient(RestClient.configureClient(clientType(2, true)));
    }

    private static SecureType secureType(boolean production, String trustStore, String keyStore) {
        SecureType secure = new SecureType();
        secure.setProduction(production);
        secure.setKeyStore(store(keyStore));
        secure.setTrustStore(store(trustStore));
        return secure;
    }

    private static KeyStoreType store(String file) {
        KeyStoreType store = new KeyStoreType();
        store.setFile(CONFIG_DIR + file);
        store.setPassword("changeit");
        store.setType("JKS");
        return store;
    }

    private static ClientType clientType(int maxConnections, boolean secure) {
        ClientType type = new ClientType();
        type.setMaxConnPerRoute(maxConnections);
        type.setMaxConnTotal(maxConnections);
        type.setSecure(secure);
        return type;
    }

    private static void gzipResponse(HttpExchange exchange) throws IOException {
        recordRequest(exchange);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(BODY.getBytes(StandardCharsets.UTF_8));
        }
        exchange.getResponseHeaders().add(HttpHeaders.CONTENT_ENCODING, "gzip");
        respond(exchange, 200, bytes.toByteArray());
    }

    private static void echoRequest(HttpExchange exchange) throws IOException {
        recordRequest(exchange);
        lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        respond(exchange, 200, "");
    }

    private static void recordRequest(HttpExchange exchange) {
        lastRequestHeader.clear();
        exchange.getRequestHeaders().forEach((name, values) ->
                lastRequestHeader.put(name.toLowerCase(), String.join(",", values)));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        respond(exchange, status, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        return Throwables.getCausalChain(ex).stream().anyMatch(type::isInstance);
    }

    private static String url(String path) {
        return "http://localhost:" + http.getAddress().getPort() + path;
    }

    private static String httpsUrl() {
        return "https://localhost:" + https.getAddress().getPort() + "/plain";
    }
}

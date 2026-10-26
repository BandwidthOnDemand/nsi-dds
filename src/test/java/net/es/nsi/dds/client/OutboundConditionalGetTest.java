package net.es.nsi.dds.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.es.nsi.dds.actors.DdsActorSystem;
import net.es.nsi.dds.actors.RegistrationActor;
import net.es.nsi.dds.agole.AgoleTopologyReader;
import net.es.nsi.dds.config.ConfigurationManager;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.dao.RemoteSubscription;
import net.es.nsi.dds.dao.RemoteSubscriptionCache;
import net.es.nsi.dds.gangofthree.Gof3DiscoveryActor;
import net.es.nsi.dds.gangofthree.Gof3DiscoveryMsg;
import net.es.nsi.dds.jaxb.dds.SubscriptionType;
import net.es.nsi.dds.messages.RegistrationEvent;
import net.es.nsi.dds.test.TestConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The outbound callers that send If-Modified-Since: Gang of Three discovery, peer
 * subscription audit and A-GOLE topology reads.
 */
public class OutboundConditionalGetTest {
    private static final long LAST_MODIFIED = Instant.parse("2025-10-24T10:00:00Z").toEpochMilli();
    private static final String LAST_MODIFIED_HEADER = "Fri, 24 Oct 2025 10:00:00 GMT";

    private static TestConfig testConfig;
    private static HttpServer peer;
    private static ActorSystem actorSystem;
    private static final List<String> ifModifiedSince = new CopyOnWriteArrayList<>();

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        testConfig = new TestConfig();
        actorSystem = ConfigurationManager.INSTANCE.getApplicationContext()
                .getBean("ddsActorSystem", DdsActorSystem.class).getActorSystem();

        peer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        peer.createContext("/not-modified", exchange -> reply(exchange, 304, null));
        peer.createContext("/topology.xml", exchange -> reply(exchange, 200,
                Files.readAllBytes(Path.of("src/test/resources/config/www/topologies/netherlight.net:2013.xml"))));
        peer.start();
    }

    @AfterClass
    public static void oneTimeTearDown() throws InterruptedException {
        peer.stop(0);
        testConfig.shutdown();
    }

    @Before
    public void clear() {
        ifModifiedSince.clear();
    }

    @Test
    public void gof3DiscoverySendsNsaLastModified() throws Exception {
        Gof3DiscoveryMsg message = new Gof3DiscoveryMsg();
        message.setNsaURL(url("/not-modified"));
        message.setNsaLastModifiedTime(LAST_MODIFIED);
        message.addTopology(url("/not-modified"), LAST_MODIFIED);

        ActorRef actor = actorSystem.actorOf(Props.create(Gof3DiscoveryActor.class));
        try {
            Gof3DiscoveryMsg reply = (Gof3DiscoveryMsg) Patterns.ask(actor, message, java.time.Duration.ofSeconds(10))
                    .toCompletableFuture().get();
            // NSA document and then its topology: both conditional, both with the stored time.
            assertEquals(List.of(LAST_MODIFIED_HEADER, LAST_MODIFIED_HEADER), ifModifiedSince);
            assertEquals(LAST_MODIFIED, reply.getNsaLastModifiedTime());
        } finally {
            actorSystem.stop(actor);
        }
    }

    @Test
    public void registrationUpdateSendsSubscriptionLastModified() throws Exception {
        RemoteSubscriptionCache cache = RemoteSubscriptionCache.getInstance();
        RemoteSubscription remote = new RemoteSubscription();
        remote.setDdsURL(url(""));
        SubscriptionType subscription = new SubscriptionType();
        subscription.setHref(url("/not-modified"));
        remote.setSubscription(subscription);
        remote.setLastModified(new Date(LAST_MODIFIED));
        cache.add(remote);

        ActorRef actor = actorSystem.actorOf(Props.create(RegistrationActor.class,
                DdsConfiguration.getInstance(), cache));
        try {
            RegistrationEvent event = new RegistrationEvent();
            event.setEvent(RegistrationEvent.Event.Update);
            event.setUrl(url(""));
            actor.tell(event, ActorRef.noSender());

            awaitRequest();
            assertEquals(List.of(LAST_MODIFIED_HEADER), ifModifiedSince);
            // A 304 marks the audit successful without replacing the subscription.
            assertTrue(remote.getLastSuccessfulAudit().getTime() > 0);
        } finally {
            actorSystem.stop(actor);
            cache.remove(url(""));
        }
    }

    @Test
    public void agoleTopologyReaderSendsLastModifiedAndHonours304() throws Exception {
        AgoleTopologyReader reader = new AgoleTopologyReader(url("/not-modified"), LAST_MODIFIED);
        assertNull(reader.readNsaTopology());
        assertEquals(List.of(LAST_MODIFIED_HEADER), ifModifiedSince);
    }

    @Test
    public void agoleTopologyReaderStoresServerLastModified() throws Exception {
        AgoleTopologyReader reader = new AgoleTopologyReader(url("/topology.xml"), 0);
        assertNotNull(reader.readNsaTopology());
        assertEquals(List.of("Thu, 01 Jan 1970 00:00:00 GMT"), ifModifiedSince);
        assertEquals(LAST_MODIFIED, reader.getLastModifiedTime());
    }

    private static void reply(HttpExchange exchange, int status, byte[] body) throws IOException {
        ifModifiedSince.add(exchange.getRequestHeaders().getFirst(HttpHeaders.IF_MODIFIED_SINCE));
        exchange.getResponseHeaders().add(HttpHeaders.LAST_MODIFIED, LAST_MODIFIED_HEADER);
        exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "application/xml");
        exchange.sendResponseHeaders(status, body == null ? -1 : body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            if (body != null) {
                out.write(body);
            }
        }
    }

    private static void awaitRequest() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (ifModifiedSince.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
    }

    private static String url(String path) {
        return "http://localhost:" + peer.getAddress().getPort() + path;
    }
}

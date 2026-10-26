package net.es.nsi.dds.management;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.management.LogListType;
import net.es.nsi.dds.jaxb.management.ResourceListType;
import net.es.nsi.dds.jaxb.management.ResourceType;
import net.es.nsi.dds.management.logs.DdsLogger;
import net.es.nsi.dds.management.logs.DdsLogs;
import net.es.nsi.dds.test.TestConfig;
import net.es.nsi.dds.util.HttpDate;
import net.es.nsi.dds.util.NsiConstants;
import org.apache.hc.core5.net.URIBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static net.es.nsi.dds.test.DdsTestSupport.VIXEN_NSA;
import static net.es.nsi.dds.test.DdsTestSupport.encode;
import static net.es.nsi.dds.test.DdsTestSupport.entity;
import static net.es.nsi.dds.test.DdsTestSupport.freshDocument;
import static net.es.nsi.dds.test.DdsTestSupport.post;
import static net.es.nsi.dds.test.DdsTestSupport.subscribe;
import static net.es.nsi.dds.test.DdsTestSupport.subscriptionRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Date headers on writes and management logs, and the links built for management resources and the portal.
 */
public class DateHeaderAndLinkTest {
    private static TestConfig testConfig;
    private static WebTarget dds;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        testConfig = new TestConfig();
        dds = testConfig.getTarget().path("dds");
        post(dds.path("documents"), freshDocument("discovery_vixen_nsa.xml"));
    }

    @AfterClass
    public static void oneTimeTearDown() throws InterruptedException {
        testConfig.shutdown();
    }

    @Test
    public void logsFilterOnIfModifiedSince() {
        // The log queue starts empty in a fresh JVM; an empty list is always a 304.
        DdsLogger.getLogger().log(DdsLogs.DDS_SUBSCRIPTION_UPDATE_DETECTED, "DateHeaderAndLinkTest");

        try (Response response = logs(null)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertNotNull(HttpDate.parse(response.getHeaderString(HttpHeaders.LAST_MODIFIED)));
        }

        try (Response response = logs("Sat, 01 Jan 2000 00:00:00 GMT")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertFalse(response.readEntity(LogListType.class).getLog().isEmpty());
        }

        try (Response response = logs(HttpDate.format(System.currentTimeMillis() + 3_600_000))) {
            assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
            assertNotNull(response.getHeaderString(HttpHeaders.LAST_MODIFIED));
        }

        // Unparseable means no condition, not a 500.
        try (Response response = logs("not a date")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void writesReturnLastModified() throws Exception {
        String subscription = subscribe(dds);
        try (Response response = dds.path("subscriptions").path(subscription).request(NsiConstants.NSI_DDS_V1_XML)
                .put(entity(subscriptionRequest()))) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertLastModified(response);
        } finally {
            dds.path("subscriptions").path(subscription).request(NsiConstants.NSI_DDS_V1_XML).delete().close();
        }

        DocumentType document = freshDocument("discovery_theworkshop_nml.xml");
        post(dds.path("documents"), document);
        try (Response response = dds.path("documents").path(encode(document.getNsa())).path(encode(document.getType()))
                .path(encode(document.getId())).request(NsiConstants.NSI_DDS_V1_XML).delete()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertLastModified(response);
        }
    }

    @Test
    public void resourceLinksAppendPathsToTheRequestUrl() {
        WebTarget management = dds.path("management");
        try (Response response = management.request(MediaType.APPLICATION_XML).get()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            List<ResourceType> resources = response.readEntity(ResourceListType.class).getResource();
            String base = management.getUri().toASCIIString();

            assertEquals(base, href(resources, "self"));
            assertEquals(base + "/v1/logs", href(resources, "logs"));
            // concatPath drops the empty segment a leading "/" produces; no doubled slashes.
            resources.forEach(r -> assertFalse(r.getHref(), r.getHref().substring("http://".length()).contains("//")));
        }
    }

    @Test
    public void portalLinksEncodeQueryParameters() {
        String html;
        try (Response response = dds.path("portal").request(MediaType.TEXT_HTML).get()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            html = response.readEntity(String.class);
        }

        URI metadata = Pattern.compile("href=\"([^\"]*/portal/metadata\\?[^\"]*)\"").matcher(html).results()
                .map(m -> URI.create(m.group(1)))
                .filter(u -> VIXEN_NSA.equals(param(u, "nsa")) && NsiConstants.NSI_DOC_TYPE_NSA_V1.equals(param(u, "type")))
                .findFirst().orElseThrow(() -> new AssertionError("no vixen metadata link in:\n" + html));
        assertEquals("/dds/portal/metadata", metadata.getPath());
        assertEquals(VIXEN_NSA, param(metadata, "id"));
        // ':' and '+' are reserved in a query value, so URIBuilder must percent-encode them.
        assertTrue(metadata.getRawQuery(), metadata.getRawQuery().contains("%3A") && metadata.getRawQuery().contains("%2B"));

        // The link the portal hands out must resolve to the document.
        try (Response response = testConfig.getClient().target(metadata).request(MediaType.APPLICATION_XML).get()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }

    private static void assertLastModified(Response response) {
        String lastModified = response.getHeaderString(HttpHeaders.LAST_MODIFIED);
        Date parsed = HttpDate.parse(lastModified);
        assertNotNull(lastModified, parsed);
        assertEquals(lastModified, HttpDate.format(parsed));
    }

    private Response logs(String ifModifiedSince) {
        var request = dds.path("management").path("v1").path("logs").request(MediaType.APPLICATION_XML);
        if (ifModifiedSince != null) {
            request = request.header(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);
        }
        return request.get();
    }

    private static String href(List<ResourceType> resources, String id) {
        return resources.stream().filter(r -> id.equals(r.getId())).findFirst().orElseThrow().getHref();
    }

    private static String param(URI uri, String name) {
        return new URIBuilder(uri).getQueryParams().stream()
                .filter(p -> name.equals(p.getName())).findFirst().orElseThrow().getValue();
    }
}

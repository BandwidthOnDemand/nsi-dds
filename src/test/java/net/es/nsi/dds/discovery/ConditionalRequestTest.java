package net.es.nsi.dds.discovery;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.test.TestConfig;
import net.es.nsi.dds.util.HttpDate;
import net.es.nsi.dds.util.NsiConstants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static net.es.nsi.dds.test.DdsTestSupport.VIXEN_NSA;
import static net.es.nsi.dds.test.DdsTestSupport.encode;
import static net.es.nsi.dds.test.DdsTestSupport.freshDocument;
import static net.es.nsi.dds.test.DdsTestSupport.post;
import static net.es.nsi.dds.test.DdsTestSupport.subscribe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * If-Modified-Since parsing and Last-Modified formatting on every discovery endpoint.
 */
@RunWith(Parameterized.class)
public class ConditionalRequestTest {
    private static final String TYPE = NsiConstants.NSI_DOC_TYPE_NSA_V1;

    // 2000-01-01T00:00:00Z in the three HTTP date formats (RFC 1123, RFC 1036, asctime).
    private static final List<String> PAST_DATES = List.of(
            "Sat, 01 Jan 2000 00:00:00 GMT",
            "Saturday, 01-Jan-00 00:00:00 GMT",
            "Sat Jan  1 00:00:00 2000");

    private static TestConfig testConfig;
    private static WebTarget dds;
    private static String localType;
    private static String localId;
    private static String subscriptionId;

    @Parameters(name = "{0}")
    public static Collection<Object[]> endpoints() {
        return Arrays.asList(
            endpoint("all", t -> t, true),
            endpoint("documents", t -> t.path("documents"), false),
            endpoint("documents/{nsa}", t -> t.path("documents").path(encode(VIXEN_NSA)), false),
            endpoint("documents/{nsa}/{type}", t -> t.path("documents").path(encode(VIXEN_NSA)).path(encode(TYPE)), false),
            endpoint("documents/{nsa}/{type}/{id}", t -> t.path("documents").path(encode(VIXEN_NSA)).path(encode(TYPE)).path(encode(VIXEN_NSA)), true),
            endpoint("local", t -> t.path("local"), false),
            endpoint("local/{type}", t -> t.path("local").path(encode(localType)), false),
            endpoint("local/{type}/{id}", t -> t.path("local").path(encode(localType)).path(encode(localId)), true),
            endpoint("subscriptions", t -> t.path("subscriptions"), false),
            endpoint("subscriptions/{id}", t -> t.path("subscriptions").path(subscriptionId), true));
    }

    private static Object[] endpoint(String name, Function<WebTarget, WebTarget> path, boolean notModifiedWhenCurrent) {
        return new Object[] { name, path, notModifiedWhenCurrent };
    }

    private final Function<WebTarget, WebTarget> endpoint;
    private final boolean notModifiedWhenCurrent;

    public ConditionalRequestTest(String name, Function<WebTarget, WebTarget> endpoint, boolean notModifiedWhenCurrent) {
        this.endpoint = endpoint;
        this.notModifiedWhenCurrent = notModifiedWhenCurrent;
    }

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        DdsConfiguration ddsConfig = new DdsConfiguration();
        ddsConfig.setFilename(TestConfig.DEFAULT_DDS_FILE);
        ddsConfig.load();
        FileUtilities.deleteDirectory(new File(ddsConfig.getRepository()));

        testConfig = new TestConfig();
        dds = testConfig.getTarget().path("dds");

        // A remote document, and a second one under the local NSA id.
        post(dds.path("documents"), freshDocument("discovery_vixen_nsa.xml"));

        DocumentType local = freshDocument("vixen-status.xml");
        local.setNsa(DdsConfiguration.getInstance().getNsaId());
        localType = local.getType();
        localId = local.getId();
        post(dds.path("local"), local);

        subscriptionId = subscribe(dds);
    }

    @AfterClass
    public static void oneTimeTearDown() throws InterruptedException {
        try (Response response = dds.path("subscriptions").path(subscriptionId).request(NsiConstants.NSI_DDS_V1_XML).delete()) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        testConfig.shutdown();
    }

    @Test
    public void lastModifiedIsRfc1123() {
        try (Response response = get(null)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            // Formatting what we parsed gives the header back only if it was RFC 1123 to begin with.
            String lastModified = response.getHeaderString(HttpHeaders.LAST_MODIFIED);
            assertEquals(lastModified, HttpDate.format(HttpDate.parse(lastModified)));
        }
    }

    @Test
    public void pastDateReturnsTheResource() {
        PAST_DATES.forEach(date -> {
            try (Response response = get(date)) {
                assertEquals(date, Response.Status.OK.getStatusCode(), response.getStatus());
            }
        });
    }

    @Test
    public void currentDateReturnsNotModifiedOrEmpty() {
        String lastModified;
        try (Response response = get(null)) {
            lastModified = response.getHeaderString(HttpHeaders.LAST_MODIFIED);
        }

        try (Response response = get(lastModified)) {
            if (notModifiedWhenCurrent) {
                assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
            } else {
                // List endpoints answer 200 with no entries and no Last-Modified.
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                assertNull(response.getHeaderString(HttpHeaders.LAST_MODIFIED));
            }
        }
    }

    @Test
    public void unparseableDateIsIgnored() {
        try (Response response = get("not a date")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }

    private Response get(String ifModifiedSince) {
        var request = endpoint.apply(dds).request(NsiConstants.NSI_DDS_V1_XML);
        if (ifModifiedSince != null) {
            request = request.header(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);
        }
        return request.get();
    }
}

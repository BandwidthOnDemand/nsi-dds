package net.es.nsi.dds.discovery;

import java.net.URLDecoder;
import java.net.URLEncoder;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class TestURLEncode {
    private static final String TYPE_URL_RAW = "application/vnd.ogf.nsi.topology.v2+xml";
    private static final String TYPE_URL_ENCODED = "application%2Fvnd.ogf.nsi.topology.v2%2Bxml";

    private static final String NSA_URL_RAW = "urn:ogf:network:dev.automation.surf.net:2017:nsa";
    private static final String NSA_URL_ENCODED = "urn%3Aogf%3Anetwork%3Adev.automation.surf.net%3A2017%3Ansa";

    private static final String TOP_URL_RAW = "urn:ogf:network:dev.automation.surf.net:2017:development";
    private static final String TOP_URL_ENCODED = "urn%3Aogf%3Anetwork%3Adev.automation.surf.net%3A2017%3Adevelopment";

    @Test
    public void test() throws Exception {

        String url = URLEncoder.encode(TYPE_URL_RAW, "UTF-8");
        assertEquals(TYPE_URL_ENCODED, url);

        url = URLDecoder.decode(url, "UTF-8");
        assertEquals(TYPE_URL_RAW, url);

        url = URLEncoder.encode(NSA_URL_RAW, "UTF-8");
        assertEquals(NSA_URL_ENCODED, url);

        url = URLDecoder.decode(url, "UTF-8");
        assertEquals(NSA_URL_RAW, url);

        url = URLEncoder.encode(TOP_URL_RAW, "UTF-8");
        assertEquals(TOP_URL_ENCODED, url);

        url = URLDecoder.decode(url, "UTF-8");
        assertEquals(TOP_URL_RAW, url);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.discovery;

import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import net.es.nsi.dds.actors.DdsActorSystem;
import net.es.nsi.dds.spring.SpringContext;
import org.junit.Test;
import static org.junit.Assert.*;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author hacksaw
 */
public class TestURLEncode {
    private static final String CONFIG_PATH = "configdir";
    private final static String CONFIG_DIR = "src/test/resources/config/";
    private static final String DEFAULT_DDS_FILE = CONFIG_DIR + "dds.xml";
    private static final String DDS_CONFIG_FILE_ARGNAME = "ddsConfigFile";

    private static final String beanConfig = new StringBuilder(CONFIG_DIR).append("beans.xml").toString().replace("/", File.separator);

    @Test
    public void test() throws Exception {

        System.setProperty(CONFIG_PATH, CONFIG_DIR);
        System.setProperty(DDS_CONFIG_FILE_ARGNAME, DEFAULT_DDS_FILE);
        String url = "application/vnd.ogf.nsi.topology.v2+xml";
        System.out.println(url);
        url = URLEncoder.encode(url, "UTF-8");
        System.out.println(url);
        url = URLDecoder.decode(url, "UTF-8");
        System.out.println(url);

        // Get a reference to the topology provider through spring.
        SpringContext sc = SpringContext.getInstance();
        ApplicationContext context;
        try {
            context = sc.initContext(beanConfig);
        }
        catch (Exception ex) {
            System.err.println("TestConfig: initContext failed");
            ex.printStackTrace();
            return;
        }
        assertNotNull(context.getBean("remoteSubscriptionCache"));
        assertNotNull(context.getBean("ddsConfiguration"));
        assertNotNull(context.getBean("discoveryProvider"));
        DdsActorSystem actorSystem = (DdsActorSystem) context.getBean("ddsActorSystem");
        assertNotNull(actorSystem);
        actorSystem.getActorSystem().shutdown();
    }
}

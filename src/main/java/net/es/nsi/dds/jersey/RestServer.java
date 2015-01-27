/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.jersey;

import net.es.nsi.dds.api.DiscoveryService;
import net.es.nsi.dds.management.api.ManagementService;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;

/**
 *
 * @author hacksaw
 */
public class RestServer {
    public static ResourceConfig getConfig(String packageName) {
        return new ResourceConfig()
                .packages(packageName) // This seems to be broken when run outside of Jersey test.
                .register(EncodingFilter.class)
                .register(GZipEncoder.class)
                .register(DeflateEncoder.class)
                .register(DiscoveryService.class) // Remove this if packages gets fixed.
                .register(ManagementService.class) // Remove this if packages gets fixed.
                .register(new MoxyXmlFeature())
                .register(new MoxyJsonFeature())
                .registerInstances(new JsonMoxyConfigurationContextResolver());
    }
}

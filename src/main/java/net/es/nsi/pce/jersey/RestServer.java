/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.jersey;

import net.es.nsi.pce.discovery.api.DiscoveryService;
import net.es.nsi.pce.management.api.ManagementService;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author hacksaw
 */
public class RestServer {
    public static ResourceConfig getConfig(String packageName) {
        return new ResourceConfig()
                .packages(packageName) // This seems to be broken when run outside of Jersey test.
                .register(DiscoveryService.class) // Remove this if packages gets fixed.
                .register(ManagementService.class) // Remove this if packages gets fixed.
                .register(new MoxyXmlFeature())
                .register(new MoxyJsonFeature())
                .registerInstances(new JsonMoxyConfigurationContextResolver());
    }
}

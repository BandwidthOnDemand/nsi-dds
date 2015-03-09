/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.jersey;

import java.util.HashSet;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author hacksaw
 */
public class Utilities {
    public static boolean validMediaType(String mediaType) {
        HashSet<String> mediaTypes = new HashSet<String>() {
            private static final long serialVersionUID = 1L;
            {
                add(MediaType.APPLICATION_JSON);
                add(MediaType.APPLICATION_XML);
                add("application/vnd.net.es.dds.v1+json");
                add("application/vnd.net.es.dds.v1+xml");
            }
        };

        return mediaTypes.contains(mediaType);
    }
}

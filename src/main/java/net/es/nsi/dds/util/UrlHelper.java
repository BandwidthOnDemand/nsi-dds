/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.util;

import java.net.URI;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author hacksaw
 */
public class UrlHelper {
    private static final Logger log = LogManager.getLogger(UrlHelper.class);

    public static boolean isAbsolute(String uri) {
        try {
            final URI u = new URI(uri);
            if(u.isAbsolute())
            {
              return true;
            }
        } catch (Exception ex) {
            log.debug("isAbsolute: invalid URI " + uri);
        }

        return false;
    }
}

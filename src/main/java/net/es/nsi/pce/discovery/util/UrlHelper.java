/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.util;

import com.google.common.base.Optional;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class UrlHelper {
    private static final Logger log = LoggerFactory.getLogger(UrlHelper.class);

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

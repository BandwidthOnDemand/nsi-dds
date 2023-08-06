/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class UrlHelper {

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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.authorization;

import net.es.nsi.dds.spring.SpringApplicationContext;

/**
 *
 * @author hacksaw
 */
public interface AuthorizationProvider {
    boolean authorize(String dn, String operation, String resource);

    static AuthorizationProvider getInstance() {
        AuthorizationProvider provider = (AuthorizationProvider) SpringApplicationContext.getBean("authorizationProvider");
        return provider;
    }
}

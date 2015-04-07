/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.authorization;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.util.concurrent.ConcurrentHashMap;
import net.es.nsi.dds.api.jaxb.AccessControlPermission;
import net.es.nsi.dds.api.jaxb.AccessControlType;
import net.es.nsi.dds.api.jaxb.DistinguishedNameType;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements access control functionality on a list of approved certificate
 * DNs.
 * 
 * @author hacksaw
 */
public class AccessControlList {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final boolean enabled;
    
    private final ConcurrentHashMap<String, DistinguishedNameType> accessControlList;
    private final X500NameStyle x500NameStyle;
    
    public AccessControlList(AccessControlType ac) throws IllegalArgumentException {
        if (ac == null) {
            throw new IllegalArgumentException("Access control list must be provided");
        }
        
        x500NameStyle = ExtendedRFC4519Style.INSTANCE;
        accessControlList = new ConcurrentHashMap<>();
        
        enabled = ac.isEnabled();

        for (DistinguishedNameType dn : ac.getDn()) {
            try {
                X500Name name = new X500Name(dn.getValue());
                String key = x500NameStyle.toString(name);
                if (accessControlList.put(key, dn) != null) {
                    log.warn("AccessControlList: multiple DN entries for " + x500NameStyle.toString(name));
                }
            }
            catch (IllegalArgumentException ex) {
                log.error("AccessControlList: badly formatted DN " + dn.getValue());
                throw ex;
            }
        }
    }
    
    public boolean isAuthorized(String dn, String operation, String resource) {
        // If DN validation is enabled then we need to validate this user
        // exists and has permissions to do what they are requesting.
        if (enabled) {
            // If there is no DN we fail the authentication.
            if (Strings.isNullOrEmpty(dn)) {
                return false;
            }
            
            // Lookup the provided DN in our list of provisioned ones.
            X500Name name = new X500Name(dn);
            String key = x500NameStyle.toString(name);
            Optional<DistinguishedNameType> result = Optional.fromNullable(accessControlList.get(key));
            if (result.isPresent()) {
                // We found the DN so now we check the permissions.
                AccessControlPermission access = result.get().getAccess();
                AccessLevels accessLevel = AccessLevels.valueOf(access.value().toLowerCase());
                return accessLevel.isOperation(operation);
            }
            
            // We did not find a matching DN in our access control list.
            return false;
        }
        
        // Access control enforcment is disabled.
        return true;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}

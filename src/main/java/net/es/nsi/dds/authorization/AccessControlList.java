/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.authorization;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;
import net.es.nsi.dds.jaxb.configuration.AccessControlPermission;
import net.es.nsi.dds.jaxb.configuration.AccessControlType;
import net.es.nsi.dds.jaxb.configuration.DistinguishedNameType;
import net.es.nsi.dds.jaxb.configuration.RuleType;
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

    private final ConcurrentHashMap<String, RuleType> accessControlList;
    private final X500NameStyle x500NameStyle;

    public AccessControlList(AccessControlType ac) throws IllegalArgumentException {
        if (ac == null) {
            throw new IllegalArgumentException("Access control list must be provided");
        }

        x500NameStyle = ExtendedRFC4519Style.INSTANCE;
        accessControlList = new ConcurrentHashMap<>();
        enabled = ac.isEnabled();

        for (RuleType rule : ac.getRule()) {
            for (DistinguishedNameType dn : rule.getDn()) {
                try {
                    X500Name name = new X500Name(dn.getValue());
                    String key = x500NameStyle.toString(name);
                    if (accessControlList.put(key, rule) != null) {
                        log.warn("AccessControlList: multiple DN entries for " + x500NameStyle.toString(name));
                    }
                }
                catch (IllegalArgumentException ex) {
                    log.error("AccessControlList: badly formatted DN " + dn.getValue());
                    throw ex;
                }
            }
        }


    }

    /**
     * This method is called by the Grizzly security filter to determine if the
     * request operation should proceed or be rejected.  The contents of the
     * operation request is not inspected for operations like POST and PUT so
     * this will need to be done in the operation specific logic.
     *
     * @param dn The certificate subject DN of the requesting entity.
     * @param operation The HTTP operation being performed.
     * @param resource The URL being accessed.
     * @return true if the operation can proceed.
     */
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
            Optional<RuleType> result = Optional.fromNullable(accessControlList.get(key));
            if (result.isPresent()) {
                // We found the DN so now we check the operation permissions.
                AccessControlPermission access = result.get().getAccess();
                AccessLevels accessLevel = AccessLevels.valueOf(access.value().toLowerCase());

                // If the access level does not permit the operation we reject.
                if (!accessLevel.isOperation(operation)) {
                    return false;
                }

                // Here we can only filter out the obvious violations.  All
                // detailed decisions need to be performed in the operation
                // specific handlers.
                if (Strings.isNullOrEmpty(resource)) {
                    return false;
                }

                switch(accessLevel) {
                    case admin:
                        // Admin can read/write everything.
                        return true;

                    case read:
                        // Read is allowed to access everything.
                        return true;

                    case write:
                        // Writepermissions can only write their configured entries.
                        if (resource.contains("/subscriptions/")) {
                            // Deligate to operation handlers.
                            return true;
                        }

                        if (resource.contains("/documents/")) {
                            for (String nsaId : result.get().getNsaId()) {
                                try {
                                    String uri = URLEncoder.encode(nsaId.trim(), "UTF-8");
                                    if (resource.contains(uri)) {
                                        return true;
                                    }
                                } catch (UnsupportedEncodingException ex) {
                                    log.error("isAuthorized: failed to encode nsiId " + nsaId);
                                    return false;
                                }
                            }
                        }

                        return false;

                    default:
                        return false;
                }
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

package net.es.nsi.dds.authorization;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;
import net.es.nsi.dds.jaxb.configuration.AccessControlPermission;
import net.es.nsi.dds.jaxb.configuration.AccessControlType;
import net.es.nsi.dds.jaxb.configuration.RuleType;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameStyle;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Implements very basic access control functionality on a list of approved
 * certificate DNs.
 *
 * @author hacksaw
 */
public class AccessControlList {

  private final Logger log = LogManager.getLogger(getClass());
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
        try {
          X500Name name = new X500Name(rule.getDn().getValue());
          String key = x500NameStyle.toString(name);
          if (accessControlList.put(key, rule) != null) {
            log.warn("AccessControlList: multiple DN entries for " + x500NameStyle.toString(name));
          }

          log.debug("AccessControlList: converted {} to {}", rule.getDn().getValue(), key);
        } catch (IllegalArgumentException ex) {
          log.error("AccessControlList: badly formatted DN " + rule.getDn().getValue());
          throw ex;
        }
    }
  }

  /**
   * This method is called by the Grizzly security filter to determine if the
   * request operation should proceed or be rejected. The contents of the
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
    log.debug("isAuthorized: incoming {}, {} {}", dn, operation, resource);

    if (enabled) {
      // If there is no DN we fail the authentication.
      if (Strings.isNullOrEmpty(dn)) {
        log.debug("isAuthorized: failed, dn is empty");
        return false;
      }

      // Lookup the provided DN in our list of provisioned ones.
      X500Name name = new X500Name(dn);
      String key = x500NameStyle.toString(name);

      log.debug("isAuthorized: looking up rules for X500 name {}", key);

      Optional<RuleType> result = Optional.fromNullable(accessControlList.get(key));
      if (result.isPresent()) {
        // We found the DN so now we check the operation permissions.
        AccessControlPermission access = result.get().getAccess();
        AccessLevels accessLevel = AccessLevels.valueOf(access.value().toLowerCase());

        log.debug("isAuthorized: evaluating dn {}, permission {}, based on {}", key, accessLevel, access.value().toLowerCase());

        // If the access level does not permit the operation we reject.
        if (!accessLevel.isAllowed(operation)) {
          log.debug("isAuthorized: failed dn {}, permission {}, operation {}", key, accessLevel, operation);
          return false;
        }

        // Here we can only filter out the obvious violations.  All
        // detailed decisions need to be performed in the operation
        // specific handlers.
        if (Strings.isNullOrEmpty(resource)) {
          // In the future we may not require a reasource so may beed to change this.
          log.debug("isAuthorized: failed {}, {}", dn, operation);
          return false;
        }

        switch (accessLevel) {
          case peer:
            // A peer can create (post), read (get), modify (put), and delete (delete)
            // their subscriptions.  The specific service logic will need to validate
            // the entry being deleted is related to the requesting NSA.
            if (resource.contains("/subscriptions")) {
              // Deligate to operation handlers.
              log.debug("isAuthorized: authorized {}, {} {}", dn, operation, resource);
              return true;
            }

            // A peer can deliver (post) notifications for document changes.
            if (resource.contains("/notifications") && accessLevel.isPost(operation)) {
              // Deligate to operation handlers.
              log.debug("isAuthorized: authorized {}, {} {}", dn, operation, resource);
              return true;
            }

            // A peer can read (get) all documents.
            if ((resource.contains("/documents") || resource.contains("/local")) && accessLevel.isGet(operation)) {
              log.debug("isAuthorized: authorized {}, {} {}", dn, operation, resource);
              return true;
            }

            // But can only modify/delete it's own documents.
            if (resource.contains("/documents")) {
              for (String nsaId : result.get().getNsaId()) {
                try {
                  String uri = URLEncoder.encode(nsaId.trim(), "UTF-8");
                  if (resource.contains(uri)) {
                    log.debug("isAuthorized: authorized {}, {} {}", dn, operation, resource);
                    return true;
                  }
                } catch (UnsupportedEncodingException ex) {
                  log.error("isAuthorized: failed to encode nsiId " + nsaId);
                  return false;
                }
              }
            }

            log.debug("isAuthorized: failed {}, {}", dn, operation);
            return false;

          case admin:
            // Admin can read/write everything.
            return true;

          case read:
            // Read is allowed to access documents.
            if ((resource.contains("/documents") || resource.contains("/local")) && accessLevel.isGet(operation)) {
              log.debug("isAuthorized: authorized {}, {} {}", dn, operation, resource);
              return true;
            }

            log.error("isAuthorized: read authorization failed, {}, {} {}", dn, operation, resource);
            return false;

          case write:
            // Write permissions can only write their associated resources.

            // Subscription resource access control will be enforced in the
            // operation handlers.
            if (resource.contains("/subscriptions")) {
              // Deligate to operation handlers.
              log.debug("isAuthorized: authorized {}, {} {}", dn, operation, resource);
              return true;
            }

            // We only POST to notifications so access control will be enforced in the
            // operation handlers.
            if (resource.contains("/notifications")) {
              if (accessLevel.isPost(operation)) {
                // Deligate to operation handlers.
                log.debug("isAuthorized: authorized {}, {} {}", dn, operation, resource);
                return true;
              }

              log.error("isAuthorized: failed {}, {} {}", dn, operation, resource);
              return false;
            }

            // Do initial access control on the documents resource.
            if ((resource.contains("/documents") || resource.contains("/local"))) {
              // Everyone can GET the documents resource.
              if (accessLevel.isGet(operation)) {
                log.debug("isAuthorized: authorized {}, {} {}", dn, operation, resource);
                return true;
              }

              // A POST is done on the document root so no NSA id is present.
              if (accessLevel.isPost(operation) &&
                      (resource.endsWith("/documents") || resource.endsWith("/documents/"))) {
                // Make sure it is the root and not
                log.debug("isAuthorized: authorized {}, {} {}", dn, operation, resource);
                return true;
              }

              // For PUT, PATCH, and DELETE they need to modfy resources they own.
              for (String nsaId : result.get().getNsaId()) {
                try {
                  String uri = URLEncoder.encode(nsaId.trim(), "UTF-8");
                  if (resource.contains(uri)) {
                    log.debug("isAuthorized: authorized {}, {} {}", dn, operation, resource);
                    return true;
                  }
                } catch (UnsupportedEncodingException ex) {
                  log.error("isAuthorized: failed to encode nsiId " + nsaId);
                  return false;
                }
              }
            }

            if (resource.contains("/local")) {
              //
            }

            log.error("isAuthorized: failed rule check, {}, {}, {}", dn, operation, resource);
            return false;

          default:
            log.error("isAuthorized: failed invalid access level {}, {}", dn, operation);
            return false;
        }
      }

      // We did not find a matching DN in our access control list.
      log.error("isAuthorized: failed to find dn {}, {}", dn, operation);
      return false;
    }

    // Access control enforcment is disabled.
    log.debug("isAuthorized: Access control enforcment is disabled {}, {} {}", dn, operation, resource);
    return true;
  }

  /**
   * @return the enabled
   */
  public boolean isEnabled() {
    return enabled;
  }
}

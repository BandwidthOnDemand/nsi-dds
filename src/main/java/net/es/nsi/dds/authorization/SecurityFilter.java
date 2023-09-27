package net.es.nsi.dds.authorization;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.api.Exceptions;

/**
 * A security filter inserted in the Grizzly request sequence that will perform authorization on secure requests.
 *
 * @author hacksaw
 */
@Slf4j
@Provider
@Priority(Priorities.AUTHORIZATION)
public class SecurityFilter implements ContainerRequestFilter {
  private final static String SSL_CLIENT_VERIFY = "SSL_CLIENT_VERIFY";
  private final static String SSL_CLIENT_S_DN   = "SSL_CLIENT_S_DN";
  private final static String SSL_CLIENT_I_DN   = "SSL_CLIENT_I_DN";

  /**
   * This filter determines if the request is secured by SSL/TLS or was at one
   * point secure before going through a reverse proxy.  Basic authorization
   * will be performed on the request before handing to the container.
   *
   * SSL_CLIENT_VERIFY == SUCCESS
   * SSL_CLIENT_S_DN - client certificate subject DN.
   * SSL_CLIENT_I_DN - issuer DN of the client certificate.
   *
   * @param ctx
   * @throws WebApplicationException
   */
  @Override
  public void filter(ContainerRequestContext ctx) throws WebApplicationException {
    // Make sure we are passed a valid context.
    if (ctx == null) {
      throw Exceptions.internalServerErrorException("ContainerRequestContext", "null");
    }

    if (AuthorizationProvider.getInstance().isEnabled()) {
      String operation = ctx.getMethod();
      String resource = ctx.getUriInfo().getPath();

      // If this is a secure security context then we want to validate the client access.
      Optional<String> dn = Optional.empty();
      if (ctx.getSecurityContext().isSecure()) {
        // Extract what we need to do basic autherization enfocement from the client certificate.
        dn = Optional.ofNullable(ctx.getSecurityContext().getUserPrincipal().getName());
      } else {
        // The security context is not secure so see if DN is provided in HTTP header via a proxy.
        Optional<String> verified = Optional.ofNullable(ctx.getHeaders().getFirst(SSL_CLIENT_VERIFY));
        if (verified.isPresent() && "SUCCESS".equalsIgnoreCase(verified.get())) {
          String first = ctx.getHeaders().getFirst(SSL_CLIENT_S_DN);
          dn = Optional.ofNullable(first);
        }
      }

      // We have a DN present to evaluate authorization.
      if (dn.isPresent()) {
        log.debug("SecurityFilter: incoming {}, {} {}", dn.get(), operation, resource);

        // We inject the authorization provider via a bean so grab and evaluate.
        boolean result = AuthorizationProvider.getInstance().authorize(dn.get(), operation, resource);

        if (!result) {
          log.error("SecurityFilter: failed {}, {} {}", dn.get(), operation, resource);
          throw Exceptions.unauthorizedException("dn", dn.get());
        }

        log.debug("SecurityFilter: authorized {}, {} {}", dn.get(), operation, resource);
      } else {
        // We have no authorization information so reject this request but log for now.
        log.error("SecurityFilter: authorization enabled but no DN provided {} {}", operation, resource);
        throw Exceptions.unauthorizedException(resource, operation);
      }
    }
  }
}

package net.es.nsi.dds.authorization;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import net.es.nsi.dds.api.Exceptions;
import org.glassfish.grizzly.http.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A security filter inserted in the Grizzly request sequence that will perform authorization on secure requests.
 *
 * @author hacksaw
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public class SecurityFilter implements ContainerRequestFilter {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @javax.inject.Inject
  private javax.inject.Provider<org.glassfish.grizzly.http.server.Request> request;

  /**
   * This filter determines if the request is secured by SSL/TLS or was at one point secure before going through a
   * reverse proxy.
   *
   * SSL_CLIENT_VERIFY == SUCCESS SSL_CLIENT_S_DN - client certificate subject DN. SSL_CLIENT_I_DN - issuer DN of the
   * client certificate.
   *
   * @param filterContext
   * @throws WebApplicationException
   */
  @Override
  public void filter(ContainerRequestContext filterContext) throws WebApplicationException {

    // The variable request is injected and should hold the HTTP request
    // context but if not then request.
    if (request == null || request.get() == null) {
      filterContext.abortWith(Response.status(Status.BAD_REQUEST)
              .entity("Invalid request context").build());
      throw Exceptions.internalServerErrorException("unknown", "unknown");
    }

    Request get = request.get();

    // If this is a secure security context then we want to validate the client access.
    if (filterContext.getSecurityContext().isSecure()) {
      String dn = get.getUserPrincipal().getName();
      String operation = filterContext.getMethod();
      String resource = filterContext.getUriInfo().getPath();

      log.debug("SecurityFilter: incoming {}, {} {}", dn, operation, resource);

      // We inject the authorization provider via a bean so grab and evaluate.
      boolean result = AuthorizationProvider.getInstance().authorize(dn, operation, resource);

      if (!result) {
        log.error("SecurityFilter: failed {}, {} {}", dn, operation, resource);
        filterContext.abortWith(
                Response.status(Status.UNAUTHORIZED)
                        .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"DDS Document Space\"")
                        .entity("Authorized certificate required").build());
        throw Exceptions.unauthorizedException("dn", dn);
      }

      log.debug("SecurityFilter: authorized {}, {} {}", dn, operation, resource);
    } else {
      // The security context is not secure so see if DN is provided in HTTP header.
      get.getHeaders("SSL_CLIENT_VERIFY");

    }
  }
}

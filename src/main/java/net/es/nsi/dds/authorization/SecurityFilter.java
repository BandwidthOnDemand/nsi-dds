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
 * A security filter inserted in the Grizzly request sequence that will
 * perform authorization on secure requests.
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
     * This filter determines if the request is secured by SSL/TLS or was at
     * one point secure before going through a reverse proxy.
     *
     * SSL_CLIENT_VERIFY == SUCCESS
     * SSL_CLIENT_S_DN - client certificate subject DN.
     * SSL_CLIENT_I_DN - issuer DN of the client certificate.
     *
     * @param filterContext
     * @throws WebApplicationException
     */
    @Override
    public void filter(ContainerRequestContext filterContext) throws WebApplicationException {

        if (request == null || request.get() == null) {
            filterContext.abortWith(
                Response.status(Status.BAD_REQUEST)
                .entity("Invalid request context").build());
            throw Exceptions.internalServerErrorException("unknown", "unknown");
        }

        Request get = request.get();

        if (filterContext.getSecurityContext().isSecure()) {
            String dn = get.getUserPrincipal().getName();
            log.debug("SecurityFilter: " + dn);
            String operation = filterContext.getMethod();
            String resource = filterContext.getUriInfo().getPath();
            boolean result = AuthorizationProvider.getInstance().authorize(dn, operation, resource);

            if (!result) {
                filterContext.abortWith(
                    Response.status(Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"DDS Document Space\"")
                    .entity("Authorized certificate required").build());
                throw Exceptions.unauthorizedException("dn", dn);
            }
        }
        else {
            get.getHeaders("SSL_CLIENT_VERIFY");

        }
    }
}

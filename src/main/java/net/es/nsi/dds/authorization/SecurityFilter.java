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

    @Override
    public void filter(ContainerRequestContext filterContext) throws WebApplicationException {
        if (filterContext.getSecurityContext().isSecure()
                && request != null && request.get() != null) {
            String dn = request.get().getUserPrincipal().getName();
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
    }
}

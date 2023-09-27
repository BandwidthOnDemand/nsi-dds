package net.es.nsi.dds.management.api;

import com.google.common.base.Strings;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.api.DiscoveryError;
import net.es.nsi.dds.api.Error;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.jaxb.ManagementParser;
import net.es.nsi.dds.jaxb.dds.ErrorType;
import net.es.nsi.dds.jaxb.management.*;
import net.es.nsi.dds.management.logs.DdsErrors;
import net.es.nsi.dds.management.logs.DdsLogger;
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.util.UrlTransform;
import net.es.nsi.dds.util.XmlUtilities;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.URIBuilder;

/**
 * The implementation class for the REST-based management interface.
 *
 * @author hacksaw
 */
@Slf4j
@Path("/dds/management")
public class ManagementService {
    private final DdsLogger ddsLogger = DdsLogger.getLogger();
    private final ObjectFactory managementFactory = new ObjectFactory();
    private final net.es.nsi.dds.jaxb.dds.ObjectFactory ddsFactory = new net.es.nsi.dds.jaxb.dds.ObjectFactory();
    private UrlTransform utilities;

    @PostConstruct
    public void init() throws Exception {
        utilities = new UrlTransform(DdsConfiguration.getInstance().getUrlTransform());
    }

    /**
     * Returns a list of available DDS service API resource URLs.
     *
     * Operation: GET /dds/management/v1
     *
     * @return A RESTful response.
     * @throws DatatypeConfigurationException
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_XHTML_XML, MediaType.APPLICATION_JSON,
        NsiConstants.NSI_DDS_V1_XML, NsiConstants.NSI_DDS_V1_JSON })
    public Response getResources(@Context UriInfo info) {

        String location = "";
        try {
            // We need the request URL (transformed) to build fully qualified resource URLs.
            location = utilities.getPath(info.getAbsolutePath().toASCIIString()).build().toASCIIString();
            log.info("[ManagementService] GET operation = {}", location);

            // Build the results object.
            ResourceListType resources = managementFactory.createResourceListType();

            // Add a self entry.
            ResourceType self = managementFactory.createResourceType();
            self.setId("self");
            self.setVersion("v1");
            self.setDefault(true);
            self.setHref(location);
            resources.getResource().add(self);

            // Now add all the resource annotated interfaces.
            Method[] methods = ManagementService.class.getMethods();
            for (Method m : methods) {
                if (m.isAnnotationPresent(ResourceAnnotation.class)) {
                    ResourceAnnotation ra = m.getAnnotation(ResourceAnnotation.class);
                    Path pathAnnotation = m.getAnnotation(Path.class);
                    if (ra == null || pathAnnotation == null) {
                        continue;
                    }

                    // Add an entry for this annotated resource.
                    ResourceType resource = managementFactory.createResourceType();
                    resource.setId(ra.name());
                    resource.setVersion(ra.version());
                    resource.setDefault(ra._default());
                    resource.setHref(concatPath(location, pathAnnotation.value()));
                    resources.getResource().add(resource);
                }
            }
            return Response
                .ok(new GenericEntity<JAXBElement<ResourceListType>>(managementFactory.createResources(resources)){})
                .header(HttpHeaders.CONTENT_LOCATION, location)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate")
                .header("Pragma", "no-cache")
                .build();
        } catch (SecurityException | URISyntaxException ex) {
            LogType logError = ddsLogger.error(DdsErrors.MANAGEMENT_INTERNAL_ERROR, location, ex.getMessage());
            Error error = ddsLogger.logTypeToError(logError);
            log.error("[ManagementService] getResources returning error:\n{}", error.toString());
            return Response.serverError()
                .entity(new GenericEntity<JAXBElement<ErrorType>>(error.getJAXBElement()) {}).build();
        }
    }

    @GET
    @Path("/v1/ping")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_XHTML_XML, MediaType.APPLICATION_JSON,
        NsiConstants.NSI_DDS_V1_XML, NsiConstants.NSI_DDS_V1_JSON })
    @ResourceAnnotation(name = "ping", version = "1.0", _default = true)
    public Response ping(@Context UriInfo info) {
        final URI location = info.getAbsolutePath();
        log.info("[ManagementService] Ping! = {}", location);

        PingType ping = managementFactory.createPingType();
        ping.setTime(XmlUtilities.xmlGregorianCalendar());

        return Response.ok(new GenericEntity<JAXBElement<PingType>>(managementFactory.createPing(ping)) {})
            .header(HttpHeaders.CONTENT_LOCATION, location.toASCIIString())
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate")
            .header("Pragma", "no-cache")
            .build();
    }

    @GET
    @Path("/v1/health")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_XHTML_XML, MediaType.APPLICATION_JSON,
        NsiConstants.NSI_DDS_V1_XML, NsiConstants.NSI_DDS_V1_JSON })
    @ResourceAnnotation(name = "health", version = "1.0", _default = true)
    public Response health(@Context UriInfo info) {
        final URI location = info.getAbsolutePath();
        log.info("[ManagementService] health check = {}", location);

        HealthStatusType status = managementFactory.createHealthStatusType();
        status.setStatus(HealthStatus.UP);

        return Response.ok(new GenericEntity<JAXBElement<HealthStatusType>>(managementFactory.createHealth(status)) {})
            .header(HttpHeaders.CONTENT_LOCATION, location.toASCIIString())
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate")
            .header("Pragma", "no-cache")
            .build();
    }

    /**
     * Get this DDS instance version information.
     *
     * @return
     */
    @GET
    @Path("/v1/version")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_XHTML_XML, MediaType.APPLICATION_JSON,
        NsiConstants.NSI_DDS_V1_XML, NsiConstants.NSI_DDS_V1_JSON })
    @ResourceAnnotation(name = "version", version = "1.0", _default = true)
    public Response version(@Context UriInfo info) throws DatatypeConfigurationException {
        final Properties properties = new Properties();

        // Load the properties file containing our $project.version from maven.
        String location = "";
        try {
            location = utilities.getPath(info.getAbsolutePath().toASCIIString()).build().toASCIIString();
            log.info("[ManagementService] GET version = {}", location);
            properties.load(this.getClass().getClassLoader().getResourceAsStream("version.properties"));
            properties.load(this.getClass().getClassLoader().getResourceAsStream("git.properties"));
        }
        catch (IllegalArgumentException | IOException | NullPointerException | URISyntaxException ex) {
            LogType logError = ddsLogger.error(DdsErrors.MANAGEMENT_INTERNAL_ERROR, location, ex.getMessage());
            Error error = ddsLogger.logTypeToError(logError);
            return Response.serverError()
                .entity(new GenericEntity<JAXBElement<ErrorType>>(error.getJAXBElement()) {}).build();
        }

        log.debug("ManagementService.version: loaded properties {}", properties.getProperty("git.commit.id"));

        VersionType result = managementFactory.createVersionType();
        for (Object key : properties.keySet()) {
            String type = (String) key;
            String value = properties.getProperty(type);

            AttributeType attribute = managementFactory.createAttributeType();
            attribute.setType(type);
            attribute.setValue(value);
            result.getAttribute().add(attribute);
        }

        try {
            if (log.isDebugEnabled()) {
                String s = ManagementParser.getInstance().xmlFormatter(result);
                log.debug("ManagementService.version: built response:\n{}", s);
            }
            return Response.ok()
                .entity(new GenericEntity<JAXBElement<VersionType>>(managementFactory.createVersion(result)) {})
                .build();
        } catch (JAXBException ex) {
            LogType logError = ddsLogger.error(DdsErrors.MANAGEMENT_INTERNAL_ERROR, location, ex.getMessage());
            Error error = ddsLogger.logTypeToError(logError);
            return Response.serverError()
                .entity(new GenericEntity<JAXBElement<ErrorType>>(error.getJAXBElement()) {}).build();
        }
    }

    /**
     * Retrieve a list of logs matching the specified criteria.  All parameters
     * are optional.
     *
     * @param ifModifiedSince Logs that have occurred since this time.
     * @param type The type of log to retrieve.
     * @param code The code of the log to retrieve.  Can only be supplied when there is a type supplied.
     * @param label The character string label of the logs to retrieve.
     * @param audit Retrieve all logs for the identified audit run.
     * @return The list of logs matching the criteria.
     * @throws Exception If there is an internal server error.
     */
    @GET
    @Path("/v1/logs")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_XHTML_XML, MediaType.APPLICATION_JSON,
                    NsiConstants.NSI_DDS_V1_XML, NsiConstants.NSI_DDS_V1_JSON })
    @ResourceAnnotation(name = "logs", version = "1.0", _default = true)
    public Response getLogs(@HeaderParam("If-Modified-Since") String ifModifiedSince,
            @QueryParam("type") String type, /* One of "Log" or "Error". */
            @QueryParam("code") String code, /* Will convert to an integer. */
            @QueryParam("label") String label,
            @QueryParam("audit") String audit) throws Exception {

        // Get the overall topology provider status.
        LogListType topologyLogs = managementFactory.createLogListType();
        Collection<LogType> logs = ddsLogger.getLogs();
        topologyLogs.getLog().addAll(logs);

        // TODO: Linear searches through thousands of logs will get slow.  Fix
        // if it becomes a problem.
        if (!Strings.isNullOrEmpty(type)) {
            if (!LogEnumType.LOG.value().equalsIgnoreCase(type) &&
                    !LogEnumType.ERROR.value().equalsIgnoreCase(type)) {
                LogType logError = ddsLogger.error(DdsErrors.MANAGEMENT_BAD_REQUEST, type, "Invalid log type");
                Error error = ddsLogger.logTypeToError(logError);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new GenericEntity<JAXBElement<ErrorType>>(error.getJAXBElement()) {}).build();
            }

            int codeInt = -1;
            if (code != null && !code.isEmpty()) {
                try {
                    codeInt = Integer.parseInt(code);
                }
                catch (NumberFormatException ne) {
                    LogType logError = ddsLogger.error(DdsErrors.MANAGEMENT_BAD_REQUEST, type, "Invalid code value");
                    Error error = ddsLogger.logTypeToError(logError);
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new GenericEntity<JAXBElement<ErrorType>>(error.getJAXBElement()) {}).build();
                }
            }

            for (Iterator<LogType> iter = topologyLogs.getLog().iterator(); iter.hasNext();) {
                LogType result = iter.next();
                if (!result.getType().value().equalsIgnoreCase(type)) {
                    iter.remove();
                }
                else if (codeInt > -1 && codeInt != result.getCode()) {
                    iter.remove();
                }
            }
        }
        else if (!Strings.isNullOrEmpty(code)) {
            LogType logError = ddsLogger.error(DdsErrors.MANAGEMENT_BAD_REQUEST, code,
                "Code query parameter must be paired with a type parameter");
            Error error = ddsLogger.logTypeToError(logError);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new GenericEntity<JAXBElement<ErrorType>>(error.getJAXBElement()) {}).build();
        }

        if (!Strings.isNullOrEmpty(label)) {
            topologyLogs.getLog().removeIf(result -> !result.getLabel().equalsIgnoreCase(label));
        }

        if (audit != null && !audit.isEmpty()) {
            for (Iterator<LogType> iter = topologyLogs.getLog().iterator(); iter.hasNext();) {
                LogType result = iter.next();
                XMLGregorianCalendar auditDate = result.getAudit();
                if (auditDate != null) {
                    if (!auditDate.toXMLFormat().equalsIgnoreCase(audit)) {
                        iter.remove();
                    }
                }
            }
        }

        String date = DateUtils.formatDate(new Date(ddsLogger.getLastlogTime()), DateUtils.PATTERN_RFC1123);

        // Now filter by the If-Modified-Since header.
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);

            topologyLogs.getLog().removeIf(result -> !(modified.compare(result.getDate()) == DatatypeConstants.LESSER));

            // If no serviceDomain then return a 304 to indicate no modifications.
            if (topologyLogs.getLog().isEmpty()) {
                // Send back a 304
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        return Response.ok().header("Last-Modified", date)
            .entity(new GenericEntity<JAXBElement<LogListType>>(managementFactory.createLogs(topologyLogs)) {}).build();
    }

    /**
     * Get a specific log entry.
     * @param id The identifier of the log.
     * @return The log if it exists.
     * @throws Exception If there is an internal server error.
     */
    @GET
    @Path("/v1/logs/{id}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_XHTML_XML, MediaType.APPLICATION_JSON,
        NsiConstants.NSI_DDS_V1_XML, NsiConstants.NSI_DDS_V1_JSON })
    @ResourceAnnotation(name = "log", version = "1.0", _default = true)
    public Response getLog(
            @PathParam("id") String id) throws Exception {

        // Verify we have the service Id from the request path.  Not sure if
        // this would ever happen.
        if (Strings.isNullOrEmpty(id)) {
            LogType logError = ddsLogger.error(DdsErrors.MANAGEMENT_BAD_REQUEST, id,
                "Log identifier must be specified in path");
            Error error = ddsLogger.logTypeToError(logError);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new GenericEntity<JAXBElement<ErrorType>>(error.getJAXBElement()) {}).build();
        }

        // Try to locate the requested Network.
        LogType result = ddsLogger.getLog(id);
        if (result == null) {
            LogType logError = ddsLogger.error(DdsErrors.MANAGEMENT_RESOURCE_NOT_FOUND, id);
            Error error = ddsLogger.logTypeToError(logError);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new GenericEntity<JAXBElement<ErrorType>>(error.getJAXBElement()) {}).build();
        }

        // Just a 200 response.
        return Response.ok()
            .entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(result)) {}).build();
    }

    private String concatPath(String root, String path) throws URISyntaxException {
        URIBuilder uri = new URIBuilder(root);
        List<String> pathSegments = uri.getPathSegments();
        if (!Strings.isNullOrEmpty(path)) {
            List<String> split = Arrays.stream(path.split("/"))
                .dropWhile(""::equalsIgnoreCase).toList();
            pathSegments.addAll(split);
        }

        uri.setPathSegments(pathSegments);

        return uri.build().toASCIIString();
    }
}

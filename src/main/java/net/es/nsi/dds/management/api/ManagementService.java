package net.es.nsi.dds.management.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Properties;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.jaxb.management.AttributeType;
import net.es.nsi.dds.jaxb.management.LogEnumType;
import net.es.nsi.dds.jaxb.management.LogListType;
import net.es.nsi.dds.jaxb.management.LogType;
import net.es.nsi.dds.jaxb.management.ObjectFactory;
import net.es.nsi.dds.jaxb.management.VersionType;
import net.es.nsi.dds.management.logs.DdsErrors;
import net.es.nsi.dds.management.logs.DdsLogger;
import net.es.nsi.dds.util.NsiConstants;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation class for the REST-based management interface.
 *
 * @author hacksaw
 */
@Path("/dds/management")
public class ManagementService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DdsLogger ddsLogger = DdsLogger.getLogger();
    private final ObjectFactory managementFactory = new ObjectFactory();

    /**
     * Get this DDS instance version information.
     *
     * @return
     */
    @GET
    @Path("/version")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response version() {
        log.debug("version: PING!");

        final Properties properties = new Properties();

        // Load the properties file containing our $project.version from maven.
        try {
            properties.load(this.getClass().getClassLoader().getResourceAsStream("version.properties"));
            properties.load(this.getClass().getClassLoader().getResourceAsStream("git.properties"));
        }
        catch (IllegalArgumentException | IOException | NullPointerException ex) {
            log.error("version: Failed to load properties file", ex);
            return Response.serverError().build();
        }

        VersionType result = managementFactory.createVersionType();
        for (Object key : properties.keySet()) {
            String type = (String) key;
            String value = properties.getProperty(type);

            AttributeType attribute = managementFactory.createAttributeType();
            attribute.setType(type);
            attribute.setValue(value);
            result.getAttribute().add(attribute);
        }

        return Response.ok().entity(new GenericEntity<JAXBElement<VersionType>>(managementFactory.createVersion(result)) {}).build();
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
    @Path("/logs")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.dds.v1+json", "application/vnd.net.es.dds.v1+xml" })
    public Response getLogs(@HeaderParam("If-Modified-Since") String ifModifiedSince,
            @QueryParam("type") String type, /* One of "Log" or "Error". */
            @QueryParam("code") String code, /* Will convert to an integer. */
            @QueryParam("label") String label,
            @QueryParam("audit") String audit) throws Exception {

        // Get the overall topology provider status.
        LogListType topologylogs = managementFactory.createLogListType();
        Collection<LogType> logs = ddsLogger.getLogs();
        topologylogs.getLog().addAll(logs);

        // TODO: Linear searches through thousands of logs will get slow.  Fix
        // if it becomes a problem.
        if (type != null && !type.isEmpty()) {
            if (!LogEnumType.LOG.value().equalsIgnoreCase(type) &&
                    !LogEnumType.ERROR.value().equalsIgnoreCase(type)) {
                LogType error = ddsLogger.error(DdsErrors.MANAGEMENT_BAD_REQUEST, type, "Invalid log type");
                return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();
            }

            int codeInt = -1;
            if (code != null && !code.isEmpty()) {
                try {
                    codeInt = Integer.parseInt(code);
                }
                catch (NumberFormatException ne) {
                    LogType error = ddsLogger.error(DdsErrors.MANAGEMENT_BAD_REQUEST, type, "Invalid code value");
                    return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();
                }
            }

            for (Iterator<LogType> iter = topologylogs.getLog().iterator(); iter.hasNext();) {
                LogType result = iter.next();
                if (!result.getType().value().equalsIgnoreCase(type)) {
                    iter.remove();
                }
                else if (codeInt > -1 && codeInt != result.getCode()) {
                    iter.remove();
                }
            }
        }
        else if (code != null && !code.isEmpty()) {
            LogType error = ddsLogger.error(DdsErrors.MANAGEMENT_BAD_REQUEST, code, "Code query parameter must be paired with a type parameter");
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();
        }

        if (label != null && !label.isEmpty()) {
            for (Iterator<LogType> iter = topologylogs.getLog().iterator(); iter.hasNext();) {
                LogType result = iter.next();
                if (!result.getLabel().equalsIgnoreCase(label)) {
                    iter.remove();
                }
            }
        }

        if (audit != null && !audit.isEmpty()) {
            for (Iterator<LogType> iter = topologylogs.getLog().iterator(); iter.hasNext();) {
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

            for (Iterator<LogType> iter = topologylogs.getLog().iterator(); iter.hasNext();) {
                LogType result = iter.next();
                if (!(modified.compare(result.getDate()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }

            // If no serviceDomain then return a 304 to indicate no modifications.
            if (topologylogs.getLog().isEmpty()) {
                // Send back a 304
                return Response.notModified().header("Last-Modified", date).build();
            }
        }

        return Response.ok().header("Last-Modified", date).entity(new GenericEntity<JAXBElement<LogListType>>(managementFactory.createLogs(topologylogs)) {}).build();
    }

    /**
     * Get a specific log entry.
     * @param id The identifier of the log.
     * @param audit
     * @return The log if it exists.
     * @throws Exception If there is an internal server error.
     */
    @GET
    @Path("/logs/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/vnd.net.es.dds.v1+json", "application/vnd.net.es.dds.v1+xml" })
    public Response getLog(
            @PathParam("id") String id) throws Exception {

        // Verify we have the service Id from the request path.  Not sure if
        // this would ever happen.
        if (id == null || id.isEmpty()) {
            LogType error = ddsLogger.error(DdsErrors.MANAGEMENT_BAD_REQUEST, id, "Log identifier must be specified in path");
            return Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();
        }

        // Try to locate the requested Network.
        LogType result = ddsLogger.getLog(id);
        if (result == null) {
            LogType error = ddsLogger.error(DdsErrors.MANAGEMENT_RESOURCE_NOT_FOUND, id);
            return Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(error)) {}).build();
        }

        // Just a 200 response.
        return Response.ok().entity(new GenericEntity<JAXBElement<LogType>>(managementFactory.createLog(result)) {}).build();
    }
}

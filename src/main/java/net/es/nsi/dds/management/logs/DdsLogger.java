package net.es.nsi.dds.management.logs;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.api.DiscoveryError;
import net.es.nsi.dds.api.Error;
import net.es.nsi.dds.jaxb.management.LogEnumType;
import net.es.nsi.dds.jaxb.management.LogType;
import net.es.nsi.dds.jaxb.management.ObjectFactory;
import net.es.nsi.dds.util.SequenceGenerator;
import net.es.nsi.dds.util.XmlUtilities;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class DdsLogger {
    private static final String NSI_ROOT_LOGS = "/logs/";
    private static final int MAX_LOG_SIZE = 2000;

    private final ObjectFactory logFactory = new ObjectFactory();

    private long lastlogTime = 0;

    private XMLGregorianCalendar auditTimeStamp = null;

    private final Map<String, LogType> logsMap = new ConcurrentHashMap<>();
    private final AbstractQueue<LogType> logsQueue = new ConcurrentLinkedQueue<>();

    /**
     * Private constructor prevents instantiation from other classes.
     */
    private DdsLogger() {
        auditTimeStamp = XmlUtilities.xmlGregorianCalendar();
    }

    /**
     * @return the lastlogTime
     */
    public long getLastlogTime() {
        return lastlogTime;
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class SingletonHolder {
        public static final DdsLogger INSTANCE = new DdsLogger();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An NsiTopologyLogger object.
     */
    public static DdsLogger getInstance() {
            return SingletonHolder.INSTANCE;
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An NmlParser object of the NSAType.
     */
    public static DdsLogger getLogger() {
            return SingletonHolder.INSTANCE;
    }

    /**
     * Allocate a new unique log identifier.
     */
    public synchronized String createId() {
        return Long.toString(SequenceGenerator.INSTANCE.getNext());
    }

    public void setAuditTimeStamp() {
        try {
            auditTimeStamp = XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis());
        }
        catch (DatatypeConfigurationException ex) {
            // Ignore for now.
        }
    }

    public void clearAuditTimeStamp() {
        auditTimeStamp = null;
    }

    public void setAuditTimeStamp(long time) {
        if (time == 0) {
            auditTimeStamp = null;
        }
        else {
            try {
                auditTimeStamp = XmlUtilities.longToXMLGregorianCalendar(time);
            }
            catch (DatatypeConfigurationException ex) {
                // Ignore for now.
            }
        }
    }

    /**
     * Create a new log resource and populate the attributes.
     *
     * @return new LogType with shell attributes populated.
     */
    private LogType createEntry() {
        long time = System.currentTimeMillis();
        lastlogTime = time;
        LogType entry = logFactory.createLogType();
        entry.setId(createId());
        entry.setHref(NSI_ROOT_LOGS + entry.getId());

        try {
            entry.setDate(XmlUtilities.longToXMLGregorianCalendar(time));
        } catch (DatatypeConfigurationException ignore) {
            // Ignore for now.
        }

        return entry;
    }

    private void logEntry(LogType entry) {
        logsMap.put(entry.getId(), entry);
        logsQueue.add(entry);

        if (logsQueue.size() >= MAX_LOG_SIZE) {
            LogType out = logsQueue.remove();
            logsMap.remove(out.getId());
        }
    }

    /**
     * Create a topology error with the provided error information.
     *
     * @param tLog The type of error being generated.
     * @param resource The resource the error is impacting.
     * @param description A description of the log.
     * @return new error fully populated.
     * @throws DatatypeConfigurationException if there is an error converting data.
     */
    public LogType log(DdsLogs tLog, String resource, String description) {
        LogType l = createEntry();
        l.setType(LogEnumType.LOG);
        l.setCode(tLog.getCode());
        l.setLabel(tLog.getLabel());
        l.setDescription(description);
        l.setResource(resource);
        log.info(createLog(l));
        return l;
    }

    /**
     * Create a topology error with the provided error information.
     *
     * @param tLog The type of error being generated.
     * @param resource The resource the error is impacting.
     * @return new error fully populated.
     * @throws DatatypeConfigurationException if there is an error converting data.
     */
    public LogType log(DdsLogs tLog, String resource) {
        LogType l = createEntry();
        l.setType(LogEnumType.LOG);
        l.setCode(tLog.getCode());
        l.setLabel(tLog.getLabel());
        l.setDescription(String.format(tLog.getDescription(), resource));
        l.setResource(resource);
        log.info(createLog(l));
        return l;
    }

    public LogType error(DdsErrors tError, String resource) {
        LogType error = createEntry();
        error.setType(LogEnumType.ERROR);
        error.setCode(tError.getCode());
        error.setLabel(tError.getLabel());
        error.setDescription(String.format(tError.getDescription(), resource));
        error.setResource(resource);
        log.error(createLog(error));
        return error;
    }

    public LogType error(DdsErrors tError, String primaryResource, String secondaryResource) {
        LogType error = createEntry();
        error.setType(LogEnumType.ERROR);
        error.setCode(tError.getCode());
        error.setLabel(tError.getLabel());
        error.setDescription(String.format(tError.getDescription(), secondaryResource));
        error.setResource(primaryResource);
        log.error(createLog(error));
        return error;
    }

    public Error logTypeToError(LogType lt) {
        return new Error.Builder(lt.getCode(), lt.getLabel(), lt.getDescription(),
            lt.getResource(), lt.getId(), lt.getDate()).build();
    }

    private DdsErrors lastError;
    private String lastRootResource;
    private int count = 0;

    public void errorSummary(DdsErrors tError, String rootResource, String primaryResource, String secondaryResource) {
        if (tError == lastError && rootResource.contains(lastRootResource)) {
            count++;
            return;
        }
        else {
            if (count > 1) {
                LogType error = createEntry();
                error.setType(LogEnumType.ERROR);
                error.setCode(lastError.getCode());
                error.setLabel(lastError.getLabel());
                error.setDescription(String.format(lastError.getDescription(), "Repeated " + count + " times"));
                error.setResource(lastRootResource);
                log.error(createLog(error));
            }

            lastError = tError;
            lastRootResource = rootResource;
            count = 1;
        }

        LogType error = createEntry();
        error.setType(LogEnumType.ERROR);
        error.setCode(tError.getCode());
        error.setLabel(tError.getLabel());
        error.setDescription(String.format(tError.getDescription(), secondaryResource));
        error.setResource(primaryResource);
        log.error(createLog(error));
    }

    public void errorSummary(DdsErrors tError, String rootResource, String primaryResource) {
        if (tError == lastError && rootResource.contains(lastRootResource)) {
            count++;
            return;
        }
        else {
            if (count > 1) {
                LogType error = createEntry();
                error.setType(LogEnumType.ERROR);
                error.setCode(lastError.getCode());
                error.setLabel(lastError.getLabel());
                error.setDescription(lastError.getDescription() + "(Repeated " + count + " times)");
                error.setResource(lastRootResource);
                log.error(createLog(error));
            }

            lastError = tError;
            lastRootResource = rootResource;
            count = 1;
        }

        LogType error = createEntry();
        error.setType(LogEnumType.ERROR);
        error.setCode(tError.getCode());
        error.setLabel(tError.getLabel());
        error.setDescription(tError.getDescription());
        error.setResource(primaryResource);
        log.error(createLog(error));
    }

    public LogType logAudit(DdsLogs tLog, String resource, String description) {
        LogType l = createEntry();
        l.setType(LogEnumType.LOG);
        l.setAudit(auditTimeStamp);
        l.setCode(tLog.getCode());
        l.setLabel(tLog.getLabel());
        l.setDescription(description);
        l.setResource(resource);
        log.info(createLog(l));
        return l;
    }

    public LogType logAudit(DdsLogs tLog, String resource) {
        LogType l = createEntry();
        l.setType(LogEnumType.LOG);
        l.setAudit(auditTimeStamp);
        l.setCode(tLog.getCode());
        l.setLabel(tLog.getLabel());
        l.setDescription(tLog.getDescription());
        l.setResource(resource);
        log.info(createLog(l));
        return l;
    }

    public LogType errorAudit(DdsErrors tError, String resource) {
        LogType error = createEntry();
        error.setType(LogEnumType.ERROR);
        error.setAudit(auditTimeStamp);
        error.setCode(tError.getCode());
        error.setLabel(tError.getLabel());
        error.setDescription(tError.getDescription());
        error.setResource(resource);
        log.error(createLog(error));
        return error;
    }

    public LogType errorAudit(DdsErrors tError, String primaryResource, String secondaryResource) {
        LogType error = createEntry();
        error.setType(LogEnumType.ERROR);
        error.setAudit(auditTimeStamp);
        error.setCode(tError.getCode());
        error.setLabel(tError.getLabel());
        error.setDescription(String.format(tError.getDescription(), secondaryResource));
        error.setResource(primaryResource);
        log.error(createLog(error));
        return error;
    }

    /**
     * @return the topologyLogs
     */
    public Map<String, LogType> getLogMap() {
        return Collections.unmodifiableMap(logsMap);
    }

    /**
     * @return the topologyLogs
     */
    public Collection<LogType> getLogs() {
        return Collections.unmodifiableCollection(logsQueue);
    }

    /**
     * @return the topologyLog
     */
    public LogType getLog(String id) {
        return logsMap.get(id);
    }

    private String createLog(LogType l) {
        logEntry(l);
        return logToString(l);
    }

    private final static String LOG_FORMAT = "code: %d, label: %s, resource: %s, description: %s";
    public String logToString(LogType logType) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        formatter.format(LOG_FORMAT, logType.getCode(), logType.getLabel(), logType.getResource(), logType.getDescription());

        if (logType.getAudit() != null) {
            sb.append(", audit: ");
            sb.append(logType.getAudit().toString());
        }
        return sb.toString();
    }
}

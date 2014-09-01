/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.management.logs;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.management.jaxb.LogEnumType;
import net.es.nsi.dds.management.jaxb.LogType;
import net.es.nsi.dds.management.jaxb.ObjectFactory;
import net.es.nsi.dds.schema.XmlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class PceLogger {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String NSI_ROOT_LOGS = "/logs/";
    private static final int MAX_LOG_SIZE = 2000;

    private ObjectFactory logFactory = new ObjectFactory();

    private long logId = 0;
    private long subLogId = 0;

    private long lastlogTime = 0;

    private XMLGregorianCalendar auditTimeStamp = null;

    private Map<String, LogType> logsMap = new ConcurrentHashMap<>();
    private AbstractQueue<LogType> logsQueue = new ConcurrentLinkedQueue<>();

    /**
     * Private constructor prevents instantiation from other classes.
     */
    private PceLogger() {
        try {
            auditTimeStamp = XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis());
        }
        catch (DatatypeConfigurationException ex) {
            // Ignore for now.
        }
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
        public static final PceLogger INSTANCE = new PceLogger();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An NsiTopologyLogger object.
     */
    public static PceLogger getInstance() {
            return SingletonHolder.INSTANCE;
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An NmlParser object of the NSAType.
     */
    public static PceLogger getLogger() {
            return SingletonHolder.INSTANCE;
    }

    /**
     * Allocate a new unique log identifier.
     */
    private synchronized String createId() {
        long newErrorId = System.currentTimeMillis();
        if (newErrorId != logId) {
            logId = newErrorId;
            subLogId = 0;
        }
        else {
            subLogId++;
        }

        String id = String.format("%d%02d", logId, subLogId);

        return id;
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
        } catch (DatatypeConfigurationException ex) {
            // Ignore for now.
        }

        logsMap.put(entry.getId(), entry);
        logsQueue.add(entry);

        if (logsQueue.size() >= MAX_LOG_SIZE) {
            LogType out = logsQueue.remove();
            logsMap.remove(out.getId());
        }

        return entry;
    }

    /**
     * Create a topology error with the provided error information.
     *
     * @param tError The type of error being generated.
     * @param resource The resource the error is impacting.
     * @param description A description of the log.
     * @return new error fully populated.
     * @throws DatatypeConfigurationException if there is an error converting data.
     */
    public LogType log(PceLogs tLog, String resource, String description) {
        LogType log = createEntry();
        log.setType(LogEnumType.LOG);
        log.setCode(tLog.getCode());
        log.setLabel(tLog.getLabel());
        log.setDescription(description);
        log.setResource(resource);
        logLog(log);
        return log;
    }

    /**
     * Create a topology error with the provided error information.
     *
     * @param tError The type of error being generated.
     * @param resource The resource the error is impacting.
     * @return new error fully populated.
     * @throws DatatypeConfigurationException if there is an error converting data.
     */
    public LogType log(PceLogs tLog, String resource) {
        LogType log = createEntry();
        log.setType(LogEnumType.LOG);
        log.setCode(tLog.getCode());
        log.setLabel(tLog.getLabel());
        log.setDescription(tLog.getDescription());
        log.setResource(resource);
        logLog(log);
        return log;
    }

    public LogType error(PceErrors tError, String resource) {
        LogType error = createEntry();
        error.setType(LogEnumType.ERROR);
        error.setCode(tError.getCode());
        error.setLabel(tError.getLabel());
        error.setDescription(tError.getDescription());
        error.setResource(resource);
        logError(error);
        return error;
    }

    public LogType error(PceErrors tError, String primaryResource, String secondaryResource) {
        LogType error = createEntry();
        error.setType(LogEnumType.ERROR);
        error.setCode(tError.getCode());
        error.setLabel(tError.getLabel());
        error.setDescription(String.format(tError.getDescription(), secondaryResource));
        error.setResource(primaryResource);
        logError(error);
        return error;
    }

    private PceErrors lastError;
    private String lastRootResource;
    private int count = 0;

    public void errorSummary(PceErrors tError, String rootResource, String primaryResource, String secondaryResource) {
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
                logError(error);
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
        logError(error);
    }

    public void errorSummary(PceErrors tError, String rootResource, String primaryResource) {
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
                logError(error);
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
        logError(error);
    }

    public LogType logAudit(PceLogs tLog, String resource, String description) {
        LogType log = createEntry();
        log.setType(LogEnumType.LOG);
        log.setAudit(auditTimeStamp);
        log.setCode(tLog.getCode());
        log.setLabel(tLog.getLabel());
        log.setDescription(description);
        log.setResource(resource);
        logLog(log);
        return log;
    }

    public LogType logAudit(PceLogs tLog, String resource) {
        LogType log = createEntry();
        log.setType(LogEnumType.LOG);
        log.setAudit(auditTimeStamp);
        log.setCode(tLog.getCode());
        log.setLabel(tLog.getLabel());
        log.setDescription(tLog.getDescription());
        log.setResource(resource);
        logLog(log);
        return log;
    }

    public LogType errorAudit(PceErrors tError, String resource) {
        LogType error = createEntry();
        error.setType(LogEnumType.ERROR);
        error.setAudit(auditTimeStamp);
        error.setCode(tError.getCode());
        error.setLabel(tError.getLabel());
        error.setDescription(tError.getDescription());
        error.setResource(resource);
        logError(error);
        return error;
    }

    public LogType errorAudit(PceErrors tError, String primaryResource, String secondaryResource) {
        LogType error = createEntry();
        error.setType(LogEnumType.ERROR);
        error.setAudit(auditTimeStamp);
        error.setCode(tError.getCode());
        error.setLabel(tError.getLabel());
        error.setDescription(String.format(tError.getDescription(), secondaryResource));
        error.setResource(primaryResource);
        logError(error);
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

    private void logError(LogType tError) {
        StringBuilder sb = new StringBuilder("code: ");
        sb.append(tError.getCode());
        sb.append(", label: ");
        sb.append(tError.getLabel());
        sb.append(", resource: ");
        sb.append(tError.getResource());
        sb.append(", description: ");
        sb.append(tError.getDescription());

        if (tError.getAudit() != null) {
            sb.append(", audit: ");
            sb.append(tError.getAudit().toString());
        }
        logger.error(sb.toString());
    }

    private void logLog(LogType tLog) {
        StringBuilder sb = new StringBuilder("code: ");
        sb.append(tLog.getCode());
        sb.append(", label: ");
        sb.append(tLog.getLabel());
        sb.append(", resource: ");
        sb.append(tLog.getResource());
        sb.append(", description: ");
        sb.append(tLog.getDescription());

        if (tLog.getAudit() != null) {
            sb.append(", audit: ");
            sb.append(tLog.getAudit().toString());
        }
        logger.info(sb.toString());
    }
}

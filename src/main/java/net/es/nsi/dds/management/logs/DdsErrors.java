package net.es.nsi.dds.management.logs;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the error values for the PCE logging system.
 *
 * @author hacksaw
 */
public enum DdsErrors {
    // Error relating to topology configuration.
    DDS_CONFIGURATION(1000, "DDS_CONFIGURATION", "Discovery configuration error."),
    DDS_CONFIGURATION_INVALID(1001, "DDS_CONFIGURATION_INVALID", "The discovery configuration file contains invalid values."),
    DDS_CONFIGURATION_INVALID_FILENAME(1002, "DDS_CONFIGURATION_INVALID_FILENAME", "Discovery configuration file not found (%s)."),
    DDS_CONFIGURATION_INVALID_XML(1003, "DDS_CONFIGURATION_INVALID_XML", "Discovery configuration file contains invalid XML (%s)."),
    DDS_CONFIGURATION_INVALID_PARAMETER(1004, "DDS_CONFIGURATION_INVALID_PARAMETER", "Value missing or invalid (%s)"),
    DDS_CONFIGURATION_CANNOT_CREATE_DIRECTORY(1005, "DDS_CONFIGURATION_CANNOT_CREATE_DIRECTORY", "Cannot create directory (%s)"),
    DDS_CONFIGURATION_INVALID_LOCAL_DDS_URL(1006, "DDS_CONFIGURATION_INVALID_LOCAL_DDS_URL", "Invalid local notification callback URL."),

    DDS_SUBSCRIPTION_ADD_FAILED(6001, "DDS_SUBSCRIPTION_ADD_FAILED", "Subscription registration failed."),
    DDS_SUBSCRIPTION_ADD_FAILED_DETAILED(6002, "DDS_SUBSCRIPTION_ADD_FAILED_DETAILED", "Subscription registration failed (%s)."),
    DDS_SUBSCRIPTION_DELETE_FAILED(6003, "DDS_SUBSCRIPTION_DELETE_FAILED", "Subscription delete failed."),
    DDS_SUBSCRIPTION_DELETE_FAILED_DETAILED(6004, "DDS_SUBSCRIPTION_DELETE_FAILED_DETAILED", "Subscription delete failed (%s)."),
    DDS_SUBSCRIPTION_GET_FAILED(6005, "DDS_SUBSCRIPTION_GET_FAILED", "Subscription get failed."),
    DDS_SUBSCRIPTION_GET_FAILED_DETAILED(6006, "DDS_SUBSCRIPTION_GET_FAILED_DETAILED", "Subscription get failed (%s)."),
    DDS_SUBSCRIPTION_NOT_FOUND(6007, "DDS_SUBSCRIPTION_NOT_FOUND", "Subscription not found (%s)."),

    DDS_NOTIFICATION_SUBSCRIPTION_PARSE_ERROR(7001, "DDS_NOTIFICATION_SUBSCRIPTION_PARSE_ERROR", "Unable to parse incoming subscription (%s)."),
    DDS_NOTIFICATION_SUBSCRIPTION_NOT_FOUND(7002, "DDS_NOTIFICATION_SUBSCRIPTION_NOT_FOUND", "Subscription for incoming notification not found (%s)."),
    DDS_NOTIFICATION_PROCESSING_ERROR(7003, "DDS_NOTIFICATION_PROCESSING_ERROR", "Unable to process incoming subscription (%s)."),

    // Topology audit errors - specifically around the discovery of topology from NSA.
    AUDIT(2000, "AUDIT", "The topology audit failed."),
    AUDIT_FORCED(2001, "AUDIT_FORCED", "A user forced topology audit failed (%s)."),
    AUDIT_MANIFEST(2002, "AUDIT_MANIFEST", "Manifest audit failed for (%s)."),
    AUDIT_MANIFEST_FILE(2003, "AUDIT_MANIFEST_FILE", "Manifest audit failed due to missing file (%s)."),
    AUDIT_MANIFEST_COMMS(2004, "AUDIT_MANIFEST_COMMS", "Manifest audit failed due to a communication error (%s)."),
    AUDIT_MANIFEST_XML_PARSE(2005, "AUDIT_MANIFEST_XML_PARSE", "Manifest audit failed to parse XML (%s)."),
    AUDIT_MANIFEST_MISSING_ISREFERENCE(2006, "AUDIT_MANIFEST_MISSING_ISREFERENCE", "Manifest audit missing isReference for (%s)."),
    AUDIT_NSA_COMMS(2007, "AUDIT_NSA_COMMS", "NSA audit failed due to a communication error (%s)."),
    AUDIT_NSA_XML_PARSE(2008, "AUDIT_NSA_XML_PARSE", "NSA audit failed do to XML parse error (%s)."),

    // Management interface errors.
    MANAGEMENT(5000, "MANAGEMENT", "Management error."),
    MANAGEMENT_RESOURCE_NOT_FOUND(5001, "MANAGEMENT_RESOURCE_NOT_FOUND", "The requested resource was not found."),
    MANAGEMENT_BAD_REQUEST(5002, "MANAGEMENT_BAD_REQUEST", "The request was invalid (%s)."),
    MANAGEMENT_TIMER_MODIFICATION(5003, "MANAGEMENT_TIMER_MODIFICATION", "The requested timer could not be modified (%s)."),
    MANAGEMENT_INTERNAL_ERROR(5004, "MANAGEMENT_INTERNAL_ERROR", "Internal server error encountered (%s)."),

    // Mark the end.
    END(9000, "", "");

    private final int code;
    private final String label;
    private final String description;

    /**
     * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
     */
    private static Map<Integer, DdsErrors> codeToStatusMapping;

    private DdsErrors(int code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public static DdsErrors getStatus(int i) {
        if (codeToStatusMapping == null) {
            initMapping();
        }
        return codeToStatusMapping.get(i);
    }

    private static void initMapping() {
        codeToStatusMapping = new HashMap<>();
        for (DdsErrors s : values()) {
            codeToStatusMapping.put(s.code, s);
        }
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TopologyProviderStatus");
        sb.append("{ code=").append(code);
        sb.append(", label='").append(label).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(" }");
        return sb.toString();
    }
}

package net.es.nsi.pce.management.logs;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the error values for the PCE logging system.
 *
 * @author hacksaw
 */
public enum PceErrors {
    // Error relating to topology configuration.
    CONFIGURATION(1000, "CONFIGURATION", "Topology configuration error."),
    CONFIGURATION_INVALID(1001, "CONFIGURATION_INVALID", "The topology configuration file contains invalid values."),
    CONFIGURATION_INVALID_FILENAME(1002, "CONFIGURATION_INVALID_FILENAME", "Topology configuration file not found (%s)."),
    CONFIGURATION_INVALID_XML(1003, "CONFIGURATION_INVALID_XML", "Topology configuration file contains invalid XML (%s)."),
    CONFIGURATION_MISSING_BASE_URL(1004, "CONFIGURATION_MISSING_BASE_URL", "Topology base URL was not provided."),
    CONFIGURATION_INVALID_AUDIT_INTERVAL(1005, "CONFIGURATION_INVALID_AUDIT_INTERVAL", "Value missing or invalid (%s) so using default."),
    CONFIGURATION_MISSING_SERVICETYPE(1006, "CONFIGURATION_MISSING_SERVICETYPE", "Value missing so using default (%s)."),
    CONFIGURATION_MISSING_DDS_URL(1004, "CONFIGURATION_MISSING_DDS_URL", "DDS URL endpoint was not provided."),

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

    // Topology audit errors - specifically around the discovery of topology from NSA.
    AUDIT_NSA_DOCUMENT(2100, "AUDIT_NSA_DOCUMENT", "The NSA document audit failed."),
    AUDIT_NSA_DOCUMENT_FORCED(2101, "AUDIT_NSA_DOCUMENT_FORCED", "A user forced audit failed (%s)."),
    AUDIT_NSA_DOCUMENT_FAILED(2102, "AUDIT_NSA_DOCUMENT_FAILED", "Audit failed for (%s)."),
    AUDIT_DDS_COMMS(2104, "AUDIT_DDS_COMMS", "Audit failed due to a DDS communication error (%s)."),
    AUDIT_NSA_DOCUMENT_XML_PARSE(2105, "AUDIT_NSA_DOCUMENT_XML_PARSE", "Audit failed to parse NSA Document XML (%s)."),

    // Unidirectional STP errors.
    STP(3000, "STP", "Unidirectional STP error."),
    STP_MULTIPLE_REMOTE_REFERNCES(3002, "STP_MULTIPLE_REMOTE_REFERNCES", "Unidirectional STP has multiple isAlias relationships (%s)."),
    STP_INVALID_REMOTE_REFERNCE(3003, "STP_INVALID_REMOTE_REFERNCES", "STP has a remote reference (isAlias) that references an STP not within topology (%s)."),
    STP_OUTBOUND_REFERNCE_MISMATCH(3004, "STP_OUTBOUND_REFERNCE_MISMATCH", "Outbound STP is not connected to an inbound STP (%s)."),
    STP_INBOUND_REFERNCE_MISMATCH(3005, "STP_INBOUND_REFERNCE_MISMATCH", "Inbound STP is not connected to an outbound STP (%s)."),
    STP_REMOTE_REFERNCE_MISMATCH(3006, "STP_REMOTE_REFERNCE_MISMATCH", "STP has remote reference but remote STP's reference does not match (%s)."),

    // Bidirectional STP errors.
    BIDIRECTIONAL_STP(4000, "BIDIRECTIONAL_STP", "Bidirectional STP error."),
    BIDIRECTIONAL_STP_MISSING_INBOUND_STP(4001, "BIDIRECTIONAL_STP_MISSING_INBOUND_STP", "Bidirectional STP missing inbound unidirectional STP so dropping from topology!"),
    BIDIRECTIONAL_STP_MISSING_OUTBOUND_STP(4002, "BIDIRECTIONAL_STP_MISSING_OUTBOUND_STP", "Bidirectional STP missing outbound unidirectional STP so dropping from topology!"),
    BIDIRECTIONAL_STP_INBOUND_REFERNCE_MISMATCH(4003, "BIDIRECTIONAL_STP_INBOUND_REFERNCE_MISMATCH", "Bidirectional STP has remote reference but remote STP's reference does not match."),
    BIDIRECTIONAL_STP_OUTBOUND_REFERNCE_MISMATCH(4004, "BIDIRECTIONAL_STP_OUTBOUND_REFERNCE_MISMATCH", "Bidirectional STP has invalid outbound STP reference."),
    BIDIRECTIONAL_STP_INVALID_INBOUND_STP(4005, "BIDIRECTIONAL_STP_INVALID_INBOUND_STP", "Bidirectional STP has inbound STP not within topology."),
    BIDIRECTIONAL_STP_INVALID_OUTBOUND_STP(4006, "BIDIRECTIONAL_STP_INVALID_OUTBOUND_STP", "Bidirectional STP has outbound STP not within topology."),
    BIDIRECTIONAL_STP_REMOTE_REFERNCE_MISMATCH(4007, "BIDIRECTIONAL_STP_REMOTE_REFERNCE_MISMATCH", "Bidirectional STP has remote reference but remote STP's reference does not match (%s)."),
    BIDIRECTIONAL_STP_INVALID_MEMEBER_VALUE(4008, "BIDIRECTIONAL_STP_INVALID_MEMEBER_VALUE", "Bidirectional STP has an invalid member value (%s)."),
    BIDIRECTIONAL_STP_LABEL_RANGE_MISMATCH(4009,"BIDIRECTIONAL_STP_LABEL_RANGE_MISMATCH", "Bidirectional STP contains unidirectional STP with differing label ranges (%s)."),

    // Management interface errors.
    MANAGEMENT(5000, "MANAGEMENT", "Management error."),
    MANAGEMENT_RESOURCE_NOT_FOUND(5001, "MANAGEMENT_RESOURCE_NOT_FOUND", "The requested resource was not found."),
    MANAGEMENT_BAD_REQUEST(5002, "MANAGEMENT_BAD_REQUEST", "The request was invalid (%s)."),
    MANAGEMENT_TIMER_MODIFICATION(5003, "MANAGEMENT_TIMER_MODIFICATION", "The requested timer could not be modified (%s)."),

    // Error relating to topology configuration.
    DISCOVERY_CONFIGURATION(6000, "DISCOVERY_CONFIGURATION", "Discovery configuration error."),
    DISCOVERY_CONFIGURATION_INVALID(6001, "DISCOVERY_CONFIGURATION_INVALID", "The discovery configuration file contains invalid values."),
    DISCOVERY_CONFIGURATION_INVALID_FILENAME(6002, "DISCOVERY_CONFIGURATION_INVALID_FILENAME", "Discovery configuration file not found (%s)."),
    DISCOVERY_CONFIGURATION_INVALID_XML(6003, "DISCOVERY_CONFIGURATION_INVALID_XML", "Discovery configuration file contains invalid XML (%s)."),
    DISCOVERY_CONFIGURATION_INVALID_PARAMETER(6004, "DISCOVERY_CONFIGURATION_INVALID_PARAMETER", "Value missing or invalid (%s)"),
    DISCOVERY_CONFIGURATION_CANNOT_CREATE_DIRECTORY(6005, "DISCOVERY_CONFIGURATION_CANNOT_CREATE_DIRECTORY", "Cannot create directory (%s)"),

    // Mark the end.
    END(9000, "", "");

    private int code;
    private String label;
    private String description;

    /**
     * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
     */
    private static Map<Integer, PceErrors> codeToStatusMapping;

    private PceErrors(int code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public static PceErrors getStatus(int i) {
        if (codeToStatusMapping == null) {
            initMapping();
        }
        return codeToStatusMapping.get(i);
    }

    private static void initMapping() {
        codeToStatusMapping = new HashMap<>();
        for (PceErrors s : values()) {
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

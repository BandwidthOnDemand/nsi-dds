/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.management.logs;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hacksaw
 */
public enum DdsLogs {
    AUDIT_START(1001, "AUDIT_START", "Audit has started."),
    AUDIT_SUCCESSFUL(1002, "AUDIT_SUCCESSFUL", "Audit has completed successfully."),
    AUDIT_PARTIAL(1003, "AUDIT_PARTIAL", "Audit completed partially successful."),
    AUDIT_MANIFEST_START(1004, "AUDIT_MANIFEST_START", "Manifest audit has started (%s)."),
    AUDIT_MANIFEST_SUCCESSFUL(1005, "AUDIT_MANIFEST_SUCCESSFUL", "Manifest audit completed successfully (%s)."),
    AUDIT_NSA_START(1006, "AUDIT_NSA_START", "NSA audit has started (%s)."),
    AUDIT_NSA_SUCCESSFUL(1007, "AUDIT_NSA_SUCCESSFUL", "NSA audit completed successfully (%s)."),
    AUDIT_USER(1008, "AUDIT_USER", "User initiated audit requested."),
    AUDIT_HAULTED(1009, "AUDIT_HAULTED", "User requested suspension of audit."),
    AUDIT_SCHEDULED(1010, "AUDIT_SCHEDULED", "User requested a schedule of an audit."),

    DDS_SUBSCRIPTION_CREATED(6001, "DDS_SUBSCRIPTION_CREATED", "Subscription created."),
    DDS_SUBSCRIPTION_DELETED(6002, "DDS_SUBSCRIPTION_DELETED", "Subscription deleted."),
    DDS_SUBSCRIPTION_UPDATE_DETECTED(6003, "DDS_SUBSCRIPTION_UPDATE_DETECTED", "Subscription update detected."),

    END(9000, "", "");

    private int code;
    private String label;
    private String description;

    /**
     * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
     */
    private static Map<Integer, DdsLogs> codeToStatusMapping;

    private DdsLogs(int code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public static DdsLogs getStatus(int i) {
        if (codeToStatusMapping == null) {
            initMapping();
        }
        return codeToStatusMapping.get(i);
    }

    private static void initMapping() {
        codeToStatusMapping = new HashMap<>();
        for (DdsLogs s : values()) {
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

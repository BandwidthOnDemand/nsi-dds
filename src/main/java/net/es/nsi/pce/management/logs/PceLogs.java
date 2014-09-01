/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.management.logs;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hacksaw
 */
public enum PceLogs {
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
    
    STP_NO_REMOTE_REFERNCE(3001, "STP_NO_REMOTE_REFERNCE", "Unidirectional STP has zero isAlias relationships."),
    
    END(9000, "", "");
    
    private int code;
    private String label;
    private String description;
 
    /**
     * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
     */
    private static Map<Integer, PceLogs> codeToStatusMapping;
 
    private PceLogs(int code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }
 
    public static PceLogs getStatus(int i) {
        if (codeToStatusMapping == null) {
            initMapping();
        }
        return codeToStatusMapping.get(i);
    }
 
    private static void initMapping() {
        codeToStatusMapping = new HashMap<>();
        for (PceLogs s : values()) {
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

package net.es.nsi.dds.authorization;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This class attempts to provide facilities for modeling REST operation access
 * levels for DDS authorization enforcement.
 *
 * @author hacksaw
 */
public enum AccessLevels {
    read(new HashSet<>(Arrays.asList("GET"))),
    write(new HashSet<>(Arrays.asList("GET", "POST", "PUT", "DELETE"))),
    admin(new HashSet<>(Arrays.asList("GET", "POST", "PUT", "DELETE"))),
    peer(new HashSet<>(Arrays.asList("GET", "POST", "PUT", "DELETE")));

    private final Set operations;

    private AccessLevels(final Set<String> operations) {
        this.operations = operations;
    }

    public boolean isAllowed(String operation) {
        return operations.contains(operation.toUpperCase());
    }

    public Set<String> getOperations() { return operations; }

    public boolean isGet(String operation) {
      return "GET".equalsIgnoreCase(operation);
    }

    public boolean isPost(String operation) {
      return "POST".equalsIgnoreCase(operation);
    }

    public boolean isPut(String operation) {
      return "PUT".equalsIgnoreCase(operation);
    }

    public boolean isDelete(String operation) {
      return "DELETE".equalsIgnoreCase(operation);
    }
}

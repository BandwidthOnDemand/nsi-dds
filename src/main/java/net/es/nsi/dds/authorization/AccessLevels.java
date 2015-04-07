package net.es.nsi.dds.authorization;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author hacksaw
 */
public enum AccessLevels {
    read(new HashSet<>(Arrays.asList("GET"))),
    write(new HashSet<>(Arrays.asList("GET", "POST", "PUT", "DELETE"))),
    admin(new HashSet<>(Arrays.asList("GET", "POST", "PUT", "DELETE")));
    
    private final Set operations;

    private AccessLevels(final Set<String> operations) {
        this.operations = operations;
    }
    
    public boolean isOperation(String operation) {
        return operations.contains(operation.toUpperCase());
    }

    public Set<String> getOperations() { return operations; }

}

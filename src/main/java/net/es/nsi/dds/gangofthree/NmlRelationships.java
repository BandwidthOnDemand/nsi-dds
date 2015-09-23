package net.es.nsi.dds.gangofthree;

/**
 *
 * @author hacksaw
 */
public class NmlRelationships {
    // Topology relationship types.
    public final static String hasInboundPort = "http://schemas.ogf.org/nml/2013/05/base#hasInboundPort";
    public final static String hasOutboundPort = "http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort";
    public final static String hasService = "http://schemas.ogf.org/nml/2013/05/base#hasService";
    public final static String isAlias = "http://schemas.ogf.org/nml/2013/05/base#isAlias";
    public final static String providesLink = "http://schemas.ogf.org/nml/2013/05/base#providesLink";

    public static boolean hasInboundPort(String type) {
        return NmlRelationships.hasInboundPort.equalsIgnoreCase(type);
    }

    public static boolean hasOutboundPort(String type) {
        return NmlRelationships.hasOutboundPort.equalsIgnoreCase(type);
    }

    public static boolean hasService(String type) {
        return NmlRelationships.hasService.equalsIgnoreCase(type);
    }

    public static boolean isAlias(String type) {
        return NmlRelationships.isAlias.equalsIgnoreCase(type);
    }

    public static boolean providesLink(String type) {
        return NmlRelationships.providesLink.equalsIgnoreCase(type);
    }
}

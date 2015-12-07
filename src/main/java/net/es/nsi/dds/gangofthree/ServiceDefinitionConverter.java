package net.es.nsi.dds.gangofthree;

import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.jaxb.nml.NmlNetworkObject;
import net.es.nsi.dds.jaxb.nml.NmlSwitchingServiceType;
import net.es.nsi.dds.jaxb.nml.NmlTopologyRelationType;
import net.es.nsi.dds.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.jaxb.nml.ServiceDefinitionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class ServiceDefinitionConverter {

    private static final String OldServiceType1 = "http://services.ogf.org/nsi/2013/07/definitions/EVTS.A-GOLE";
    private static final String OldServiceType2 = "http://services.ogf.org/nsi/2013/07/descriptions/EVTS.A-GOLE";
    private static final String OldServiceType3 = "http://services.ogf.org/nsi/2013/12/definitions/EVTS.A-GOLE";
    private static final String NewServiceType = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";

    private final static Logger log = LoggerFactory.getLogger(ServiceDefinitionConverter.class);

    public static boolean convert(NmlTopologyType nml) {
        boolean modified;

        // Check the ServiceDefinition for the old serviceType.
        modified = convertServiceType(nml.getAny());

        // Check the SwitchingServices for a ServiceDefinition with the old serviceType.
        for (NmlTopologyRelationType relation : nml.getRelation()) {
            if (NmlRelationships.hasService(relation.getType())) {
                for (NmlNetworkObject service : relation.getService()) {
                    // We want the SwitchingService.
                    if (service instanceof NmlSwitchingServiceType) {
                        NmlSwitchingServiceType ss = (NmlSwitchingServiceType) service;
                        log.debug("Converting SwitchingService type for id=" + ss.getId());
                        boolean mod = convertServiceType(ss.getAny());
                        modified = modified || mod;
                    }
                }
            }
        }

        // Increment the version to indicate a change.
        if (modified) {
            XMLGregorianCalendar version = nml.getVersion();
            log.debug("Modified id=" + nml.getId() + ", version=" + version);

            version.setSecond(version.getSecond() + 5);
            nml.setVersion(version);
            log.debug("Set new version id=" + nml.getId() + ", version=" + nml.getVersion());
        }

        return modified;
    }

    private static boolean convertServiceType(List<Object> any) {
        boolean modified = false;
        for (Object object : any) {
            if (object instanceof JAXBElement) {
                JAXBElement<?> jaxb = (JAXBElement) object;
                if (jaxb.getValue() instanceof ServiceDefinitionType) {
                    ServiceDefinitionType serviceDefinition = (ServiceDefinitionType) jaxb.getValue();
                    String serviceType = serviceDefinition.getServiceType();
                    if (serviceType != null) {
                        serviceType = serviceType.trim();
                        if (serviceType.equalsIgnoreCase(OldServiceType1) ||
                                serviceType.equalsIgnoreCase(OldServiceType2) ||
                                serviceType.equalsIgnoreCase(OldServiceType3)) {
                            log.debug("Converting service type for id=" + serviceDefinition.getId());
                            serviceDefinition.setServiceType(NewServiceType);
                            modified = true;
                        }
                    }
                }
            }
        }

        return modified;
    }
}

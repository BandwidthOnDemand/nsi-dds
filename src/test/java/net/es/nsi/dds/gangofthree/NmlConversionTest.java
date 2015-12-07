package net.es.nsi.dds.gangofthree;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.jaxb.NmlParser;
import net.es.nsi.dds.jaxb.nml.NmlNetworkObject;
import net.es.nsi.dds.jaxb.nml.NmlSwitchingServiceType;
import net.es.nsi.dds.jaxb.nml.NmlTopologyRelationType;
import net.es.nsi.dds.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.jaxb.nml.ServiceDefinitionType;
import net.es.nsi.dds.util.Log4jHelper;
import org.apache.log4j.xml.DOMConfigurator;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class NmlConversionTest {
    private Logger log;

    @Before
    public void setUp() throws IllegalArgumentException, JAXBException, FileNotFoundException, NullPointerException, IOException {
        // Load and watch the log4j configuration file for changes.
        DOMConfigurator.configureAndWatch(Log4jHelper.getLog4jConfig("src/test/resources/config/"), 45 * 1000);
        log = LoggerFactory.getLogger(NmlConversionTest.class);

    }

    @Test
    public void serviceTypeInSD() throws JAXBException, IOException {
        log.debug("serviceTypeInSD: start");
        convertTest("src/test/resources/nml/parse/tests/serviceTypeInSD.xml");
        log.debug("serviceTypeInSD: end");
    }

    @Test
    public void serviceTypeInSDandSS() throws JAXBException, IOException {
        log.debug("serviceTypeInSDandSS: start");
        convertTest("src/test/resources/nml/parse/tests/serviceTypeInSS.xml");
        log.debug("serviceTypeInSDandSS: end");
    }

    private void convertTest(String file) throws JAXBException, IOException {
        NmlTopologyType nml = NmlParser.getInstance().readTopology(file);

        assertTrue(ServiceDefinitionConverter.convert(nml));

        assertTrue(verifyServiceType(nml.getAny()));

        // Check the SwitchingServices for a ServiceDefinition with the old serviceType.
        for (NmlTopologyRelationType relation : nml.getRelation()) {
            if (NmlRelationships.hasService(relation.getType())) {
                for (NmlNetworkObject service : relation.getService()) {
                    // We want the SwitchingService.
                    if (service instanceof NmlSwitchingServiceType) {
                        NmlSwitchingServiceType ss = (NmlSwitchingServiceType) service;
                        log.debug("Verifying SwitchingService type for id=" + ss.getId());
                        assertTrue(verifyServiceType(ss.getAny()));
                    }
                }
            }
        }
    }

    private static final String OldServiceType1 = "http://services.ogf.org/nsi/2013/07/definitions/EVTS.A-GOLE";
    private static final String OldServiceType2 = "http://services.ogf.org/nsi/2013/07/descriptions/EVTS.A-GOLE";
    private static final String OldServiceType3 = "http://services.ogf.org/nsi/2013/12/definitions/EVTS.A-GOLE";
    private static final String NewServiceType = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";

    private boolean verifyServiceType(List<Object> any) {
        for (Object object : any) {
            if (object instanceof JAXBElement) {
                JAXBElement<?> jaxb = (JAXBElement) object;
                if (jaxb.getValue() instanceof ServiceDefinitionType) {
                    ServiceDefinitionType serviceDefinition = (ServiceDefinitionType) jaxb.getValue();
                    String serviceType = serviceDefinition.getServiceType();
                    log.debug("Checking serviceDefinition id=" + serviceDefinition.getId() + ", serviceType=" + serviceType);
                    if (serviceType != null) {
                        serviceType = serviceType.trim();
                        if (serviceType.equalsIgnoreCase(OldServiceType1) ||
                                serviceType.equalsIgnoreCase(OldServiceType2) ||
                                serviceType.equalsIgnoreCase(OldServiceType3)) {
                            log.debug("Old service type exists for id=" + serviceDefinition.getId());
                            return false;
                        }
                        else if (serviceType.equalsIgnoreCase(NewServiceType)) {
                            log.debug("New service type exists for id=" + serviceDefinition.getId());
                            return true;
                        }
                    }
                }
            }
        }

        return true;
    }
}

package net.es.nsi.dds.gangofthree;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.jaxb.NmlParser;
import net.es.nsi.dds.jaxb.nml.NmlNetworkObject;
import net.es.nsi.dds.jaxb.nml.NmlSwitchingServiceType;
import net.es.nsi.dds.jaxb.nml.NmlTopologyRelationType;
import net.es.nsi.dds.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.jaxb.nml.ServiceDefinitionType;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class NmlConversionTest {
  @Test
  public void serviceTypeInSD() throws JAXBException, IOException {
    log.debug("*************************************** serviceTypeInSD: start");
    convertTest("src/test/resources/nml/parse/tests/serviceTypeInSD.xml");
    log.debug("*************************************** serviceTypeInSD: end");
  }

  @Test
  public void serviceTypeInSDandSS() throws JAXBException, IOException {
    log.debug("*************************************** serviceTypeInSDandSS: start");
    convertTest("src/test/resources/nml/parse/tests/serviceTypeInSS.xml");
    log.debug("*************************************** serviceTypeInSDandSS: end");
  }

  @Test
  public void serviceTypeInSDWithBoundaryTime() throws JAXBException, IOException {
    log.debug("*************************************** serviceTypeInSDWithBoundaryTime: start");
    convertTest("src/test/resources/nml/parse/tests/serviceTypeInSDWithBoundaryTime.xml");
    log.debug("*************************************** serviceTypeInSDWithBoundaryTime: end");
  }

  @Test
  public void nmlDocumentWithEthernetElements() throws JAXBException, IOException {
    log.debug("*************************************** nmlDocumentWithEthernetElements: start");

    NmlEthernet nml = new NmlEthernet();
    nml.setMaximumReservableCapacity(Optional.of(10000000000L));
    nml.setMinimumReservableCapacity(Optional.of(0L));
    nml.setCapacity(Optional.of(10000000000L));
    nml.setGranularity(Optional.of(1000000L));
    validateEthernetAttributes("src/test/resources/nml/parse/tests/nmlDocumentWithEthernetElements.xml",
            "urn:ogf:network:netlab.es.net:2013::netlab-mx960-rt2:xe-0_0_0:+:out", nml);

    NmlEthernet nmlEsnet = new NmlEthernet();
    nmlEsnet.setMaximumReservableCapacity(Optional.of(10000000000L));
    nmlEsnet.setMinimumReservableCapacity(Optional.of(1000000L));
    nmlEsnet.setCapacity(Optional.of(10000000000L));
    nmlEsnet.setGranularity(Optional.of(1000000L));
    validateEthernetAttributes("src/test/resources/nml/parse/tests/ESnet-topology.xml",
            "urn:ogf:network:es.net:2013::fnal-mr2:xe-2_2_0:+:out", nmlEsnet);
    log.debug("*************************************** nmlDocumentWithEthernetElements: end");
  }

  private void validateEthernetAttributes(String file, String port, NmlEthernet attrs) throws JAXBException, IOException {
    NmlTopologyType nml = NmlParser.getInstance().readTopology(file);
    nml.getRelation().stream()
            .filter((r) -> "http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort".equalsIgnoreCase(r.getType()))
            .forEach((NmlTopologyRelationType r) -> {
              if (!r.getPort().isEmpty()) {
                r.getPort().stream().filter((p) -> (p.getId().equalsIgnoreCase(port)))
                        .findFirst().ifPresent((p) -> {
                          log.debug("Parsing Port ANY elements, siz = " + p.getAny().size());
                          NmlEthernet nmlEthernet = new NmlEthernet(p.getAny());
                          assertTrue(nmlEthernet.equals(attrs));
                        });
              }

              if (!r.getPortGroup().isEmpty()) {
                r.getPortGroup().stream().filter((p) -> (p.getId().equalsIgnoreCase(port)))
                        .findFirst().ifPresent((p) -> {
                          log.debug("Parsing PortGroup ANY elements, size = " + p.getAny().size());
                          NmlEthernet nmlEthernet = new NmlEthernet(p.getAny());
                          assertTrue(nmlEthernet.equals(attrs));
                        });
              }
            });
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
            if (serviceType.equalsIgnoreCase(OldServiceType1)
                    || serviceType.equalsIgnoreCase(OldServiceType2)
                    || serviceType.equalsIgnoreCase(OldServiceType3)) {
              log.debug("Old service type exists for id=" + serviceDefinition.getId());
              return false;
            } else if (serviceType.equalsIgnoreCase(NewServiceType)) {
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

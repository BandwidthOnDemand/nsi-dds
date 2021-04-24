/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.nsi.dds.gangofthree;

import jakarta.xml.bind.JAXBElement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import net.es.nsi.dds.jaxb.nml.NmlLabelGroupType;
import net.es.nsi.dds.jaxb.nml.NmlLabelType;

/**
 *
 * @author hacksaw
 */
@lombok.Data
public class NmlEthernet {

  public final static String ETHERNET_NAMESPACE = "http://schemas.ogf.org/nml/2012/10/ethernet";
  public final static String GRANULARITY = "granularity";
  public final static String MAXIMUMRESERVABLECAPACITY = "maximumReservableCapacity";
  public final static String MINIMUMRESERVABLECAPACITY = "minimumReservableCapacity";
  public final static String INTERFACEMTU = "interfaceMTU";
  public final static String ETHLABEL = "label";
  public final static String CAPACITY = "capacity";
  public final static String ENCODING = "encoding";

  public final static QName GRANULARITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "granularity");
  public final static QName MAXIMUMRESERVABLECAPACITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "maximumReservableCapacity");
  public final static QName INTERFACEMTU_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "interfaceMTU");
  public final static QName ETHLABEL_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "label");
  public final static QName CAPACITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "capacity");
  public final static QName ENCODING_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "encoding");
  public final static QName MINIMUMRESERVABLECAPACITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "minimumReservableCapacity");

  public final static String VLAN_LABEL = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";
  public final static String VLAN = "vlan";

  // Ethernet port metrics.
  @lombok.Builder.Default
  private Optional<Long> granularity = Optional.empty();
  @lombok.Builder.Default
  private Optional<Long> maximumReservableCapacity = Optional.empty();
  @lombok.Builder.Default
  private Optional<Long> minimumReservableCapacity = Optional.empty();
  @lombok.Builder.Default
  private Optional<Integer> interfaceMTU = Optional.empty();
  @lombok.Builder.Default
  private Optional<Long> capacity = Optional.empty();

  public NmlEthernet() {
  }

  public NmlEthernet(List<Object> obj) {
    for (Object any : obj) {
      if (any instanceof JAXBElement) {
        JAXBElement<?> element = (JAXBElement<?>) any;
        if (isEthernetNamespace(element)) {
          switch (getElementName(element)) {
            case NmlEthernet.GRANULARITY:
              this.granularity = Optional.ofNullable((Long) element.getValue());
              break;
            case NmlEthernet.CAPACITY:
              this.capacity = Optional.ofNullable((Long) element.getValue());
              break;
            case NmlEthernet.INTERFACEMTU:
              this.interfaceMTU = Optional.ofNullable((Integer) element.getValue());
              break;
            case NmlEthernet.MAXIMUMRESERVABLECAPACITY:
              this.maximumReservableCapacity = Optional.ofNullable((Long) element.getValue());
              break;
            case NmlEthernet.MINIMUMRESERVABLECAPACITY:
              this.minimumReservableCapacity = Optional.ofNullable((Long) element.getValue());
              break;
            default:
              break;
          }
        }
      }
    }
  }

  public static boolean isEthernetNamespace(String namespace) {
    return ETHERNET_NAMESPACE.equalsIgnoreCase(namespace);
  }

  public static boolean isEthernetNamespace(JAXBElement<?> jaxb) {
    return isEthernetNamespace(jaxb.getName().getNamespaceURI());
  }

  public static String getElementName(JAXBElement<?> jaxb) {
    return jaxb.getName().getLocalPart();
  }

  public static boolean isVlanLabel(Optional<String> label) {
    if (!label.isPresent()) {
      return false;
    }
    return VLAN_LABEL.equalsIgnoreCase(label.get());
  }

  public static Set<NmlLabelType> labelGroupToLabels(NmlLabelGroupType labelGroup) {

    Optional<String> labelType = Optional.ofNullable(labelGroup.getLabeltype());
    if (!isVlanLabel(labelType)) {
      throw new IllegalArgumentException("Invalid vlan label: " + labelGroup.getLabeltype());
    }

    Set<NmlLabelType> labels = new LinkedHashSet<>();

    // Split the vlan first by comma, then by hyphen.
    Pattern pattern = Pattern.compile(",");
    String[] comma = pattern.split(labelGroup.getValue());
    for (int i = 0; i < comma.length; i++) {
      // Now by hyphen.
      pattern = Pattern.compile("-");
      String[] hyphen = pattern.split(comma[i]);

      // Just a single vlan.
      if (hyphen.length == 1) {
        NmlLabelType label = new NmlLabelType();
        label.setLabeltype(labelGroup.getLabeltype());
        label.setValue(hyphen[0]);
        labels.add(label);
      } // Two vlans in a range.
      else if (hyphen.length > 1 && hyphen.length < 3) {
        int min = Integer.parseInt(hyphen[0]);
        int max = Integer.parseInt(hyphen[1]);
        for (int j = min; j < max + 1; j++) {
          NmlLabelType label = new NmlLabelType();
          label.setLabeltype(labelGroup.getLabeltype());
          label.setValue(Integer.toString(j));
          labels.add(label);
        }
      } // This is unsupported.
      else {
        throw new IllegalArgumentException("Invalid vlan string format: " + labelGroup.getValue());
      }
    }
    return labels;
  }

  @Override
  public boolean equals(Object o) {
    // Self check.
    if (this == o) {
      return true;
    }

    // Null check
    if (o == null) {
      return false;
    }

    // Yype check and cast.
    if (getClass() != o.getClass()) {
      return false;
    }

    NmlEthernet that = getClass().cast(o);

    // Verify we have the same field present.
    if (this.capacity.isPresent() != that.capacity.isPresent() ||
            this.granularity.isPresent() != that.granularity.isPresent() ||
            this.interfaceMTU.isPresent() != that.interfaceMTU.isPresent() ||
            this.maximumReservableCapacity.isPresent() != that.maximumReservableCapacity.isPresent() ||
            this.minimumReservableCapacity.isPresent() != that.minimumReservableCapacity.isPresent()) {
      return false;
    }

    // Field comparison.
    if (this.capacity.isPresent() && !this.capacity.get().equals(that.capacity.orElse(null))) {
      return false;
    }

    if (this.granularity.isPresent() && !this.granularity.get().equals(that.granularity.orElse(null))) {
      return false;
    }

    if (this.interfaceMTU.isPresent() && !this.interfaceMTU.get().equals(that.interfaceMTU.orElse(null))) {
      return false;
    }

    if (this.maximumReservableCapacity.isPresent()
            && !this.maximumReservableCapacity.get().equals(that.maximumReservableCapacity.orElse(null))) {
      return false;
    }

    if (this.minimumReservableCapacity.isPresent()
            && !this.minimumReservableCapacity.get().equals(that.minimumReservableCapacity.orElse(null))) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 89 * hash + Objects.hashCode(this.granularity);
    hash = 89 * hash + Objects.hashCode(this.maximumReservableCapacity);
    hash = 89 * hash + Objects.hashCode(this.minimumReservableCapacity);
    hash = 89 * hash + Objects.hashCode(this.interfaceMTU);
    hash = 89 * hash + Objects.hashCode(this.capacity);
    return hash;
  }
}

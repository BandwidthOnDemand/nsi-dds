package net.es.nsi.dds.api;

import jakarta.xml.bind.JAXBElement;
import net.es.nsi.dds.jaxb.dds.ErrorType;
import net.es.nsi.dds.util.SequenceGenerator;
import net.es.nsi.dds.util.XmlUtilities;

import javax.xml.datatype.XMLGregorianCalendar;

public class Error {
  private final net.es.nsi.dds.jaxb.dds.ObjectFactory ddsFactory = new net.es.nsi.dds.jaxb.dds.ObjectFactory();
  private final ErrorType error;

  private static final SequenceGenerator generator = SequenceGenerator.INSTANCE;

  private Error(Builder builder) {
    this.error = builder.error;
  }

  public ErrorType getErrorType() {
    return error;
  }

  public JAXBElement<ErrorType> getJAXBElement() {
    return ddsFactory.createError(error);
  }

  @Override
  public String toString() {
    return XmlUtilities.jaxbToXml(ErrorType.class, ddsFactory.createError(error));
  }

  public static class Builder {
    private final net.es.nsi.dds.jaxb.dds.ObjectFactory ddsFactory = new net.es.nsi.dds.jaxb.dds.ObjectFactory();
    private final ErrorType error = ddsFactory.createErrorType();

    public Builder(int code, String label, String description, String resource, String id, XMLGregorianCalendar date) {
      error.setCode(code);
      error.setLabel(label);
      error.setDescription(description);
      error.setResource(resource);
      error.setId(id);
      error.setDate(date);
    }

    public Builder(int code, String label, String description, String resource) {
      error.setCode(code);
      error.setLabel(label);
      error.setDescription(description);
      error.setResource(resource);
      error.setId(Long.toString(generator.getNext()));
      error.setDate(XmlUtilities.xmlGregorianCalendar());
    }

    public Builder() {
      error.setId(Long.toString(generator.getNext()));
      error.setDate(XmlUtilities.xmlGregorianCalendar());
    }

    public Builder code(int code) {
      error.setCode(code);
      return this;
    }

    public Builder label(String label) {
      error.setLabel(label);
      return this;
    }

    public Builder description(String description) {
      error.setDescription(description);
      return this;
    }

    public Builder resource(String resource) {
      error.setResource(resource);
      return this;
    }

    public Builder id(String id) {
      error.setId(id);
      return this;
    }

    public Builder date(XMLGregorianCalendar date) {
      error.setDate(date);
      return this;
    }

    public Error build() {
      return new Error(this);
    }
  }
}

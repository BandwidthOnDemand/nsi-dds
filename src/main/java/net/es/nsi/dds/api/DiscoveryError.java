package net.es.nsi.dds.api;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.ErrorType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.util.XmlUtilities;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Defines the error values for the DDS logging system.
 *
 * @author hacksaw
 */
@Slf4j
public enum DiscoveryError {
  // Authentication/authorization errors.
  UNAUTHORIZED(401, "UNAUTHORIZED", "Supplied credentials did not have needed level of authorization (%s)."),

  // Message content errors.
  MISSING_PARAMETER(400, "MISSING_PARAMETER", "Missing parameter (%s)."),
  NOT_FOUND(404, "NOT_FOUND", "Requested resources was not found (%s)."),

  INVALID_PARAMETER(4001, "INVALID_PARAMETER", "Invalid parameter (%s)."),
  UNSUPPORTED_PARAMETER(4002, "UNSUPPORTED_PARAMETER", "Parameter provided contains an unsupported value which MUST be processed (%s)."),
  INVALID_XML(4003, "INVALID_XML", "Request contained invalid XML (%s)."),

  // Document specific errors.
  DOCUMENT_DOES_NOT_EXIST(4040, "DOCUMENT_DOES_NOT_EXIST", "The requested document does not exist (%s)."),
  DOCUMENT_EXISTS(409, "DOCUMENT_EXISTS", "There is already a registered document under provided id (%s)."),
  DOCUMENT_INVALID(4004, "DOCUMENT_INVALID", "There was a problem with the document that prevents storage (%s)."),
  DOCUMENT_VERSION(4005, "DOCUMENT_VERSION", "The document version was older than the current document (%s)."),

  // Subscription related errors.
  SUBSCRIPTION_DOES_NOT_EXIST(120, "SUBCRIPTION_DOES_NOT_EXIST", "Requested subscription identifier does not exist (%s)."),

  // Implementation related issues.
  INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "There was an internal server processing error (%s)."),
  NOT_IMPLEMENTED(501, "NOT_IMPLEMENTED", "Parameter is for a feature that has not been implemented (%s)."),
  VERSION_NOT_SUPPORTED(505, "VERSION_NOT_SUPPORTED", "The service version requested is not supported. (%s)."),

  // Mark the end.
  END(9999, "END", "END");

  private final int code;
  private final String label;
  private final String description;

  /**
   * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
   */
  private static Map<Integer, DiscoveryError> codeToStatusMapping;

  private static final ObjectFactory factory = new ObjectFactory();

  private DiscoveryError(int code, String label, String description) {
    this.code = code;
    this.label = label;
    this.description = description;
  }

  private static void initMapping() {
    codeToStatusMapping = new HashMap<>();
    for (DiscoveryError s : values()) {
      codeToStatusMapping.put(s.code, s);
    }
  }

  public static DiscoveryError getStatus(int i) {
    if (codeToStatusMapping == null) {
      initMapping();
    }
    return codeToStatusMapping.get(i);
  }

  public static ErrorType getErrorType(DiscoveryError error, String resource, String info) {
    return new Error.Builder()
        .code(error.getCode())
        .label(error.getLabel())
        .resource(resource)
        .description(String.format(error.getDescription(), info))
        .build().getErrorType();
  }

  public static Error getError(DiscoveryError error, String id, XMLGregorianCalendar date,
                               String resource, String info) {
    return new Error.Builder(error.getCode(), error.getLabel(), String.format(error.getDescription(), info),
        resource, id, date).build();
  }

  public static ErrorType getErrorType(String xml) {
    try {
      return DdsParser.getInstance().xml2Error(xml);
    } catch (JAXBException ex) {
      return getErrorType(INTERNAL_SERVER_ERROR, "JAXB", xml);
    }
  }

  public static String getErrorXml(DiscoveryError error, String resource, String info) {
    try {
      ErrorType fp = getErrorType(error, resource, info);
      JAXBElement<ErrorType> errorElement = factory.createError(fp);
      return DdsParser.getInstance().jaxb2Xml(errorElement);
    } catch (JAXBException ex) {
      log.error("getErrorString: could not generate xml", ex);
      return null;
    }
  }

  public static String getErrorXml(ErrorType error) throws JAXBException {
    JAXBElement<ErrorType> errorElement = factory.createError(error);
    return DdsParser.getInstance().jaxb2Xml(errorElement);
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
    sb.append("DiscoveryError ");
    sb.append("{ code=").append(code);
    sb.append(", label='").append(label).append('\'');
    sb.append(", description='").append(description).append('\'');
    sb.append(" }");
    return sb.toString();
  }
}

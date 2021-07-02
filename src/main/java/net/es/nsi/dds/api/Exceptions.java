/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.ErrorType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.provider.InvalidVersionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author hacksaw
 */
public class Exceptions {
  private static final Logger log = LogManager.getLogger(Exceptions.class);
  private static final ObjectFactory factory = new ObjectFactory();

  private static String format(ErrorType error) {
    String errstr = "failed to format ErrorType";
    try {
      errstr = DdsParser.getInstance().error2Xml(error);
    } catch (JAXBException | IOException ex) {
      log.error(errstr);
    }

    return errstr;
  }

  public static WebApplicationException internalServerErrorException(String resource, String parameter) {
    ErrorType error = DiscoveryError.getErrorType(DiscoveryError.INTERNAL_SERVER_ERROR, resource, parameter);
    Response ex = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(format(error)).build();
    return new WebApplicationException(ex);
  }

  public static WebApplicationException unauthorizedException(String resource, String parameter) {
    ErrorType error = DiscoveryError.getErrorType(DiscoveryError.UNAUTHORIZED, resource, parameter);
    Response ex = Response.status(Response.Status.UNAUTHORIZED).entity(format(error)).build();
    return new WebApplicationException(ex);
  }

  public static WebApplicationException missingParameterException(String resource, String parameter) {
    ErrorType error = DiscoveryError.getErrorType(DiscoveryError.MISSING_PARAMETER, resource, parameter);
    Response ex = Response.status(Response.Status.BAD_REQUEST).entity(format(error)).build();
    return new WebApplicationException(ex);
  }

  public static WebApplicationException invalidXmlException(String resource, String parameter) {
    ErrorType error = DiscoveryError.getErrorType(DiscoveryError.MISSING_PARAMETER, resource, parameter);
    Response ex = Response.status(Response.Status.BAD_REQUEST).entity(format(error)).build();
    return new WebApplicationException(ex);
  }

  public static WebApplicationException illegalArgumentException(DiscoveryError errorEnum, String resource, String parameter) {
    ErrorType error = DiscoveryError.getErrorType(errorEnum, resource, parameter);
    Response ex = Response.status(Response.Status.BAD_REQUEST).entity(format(error)).build();
    return new WebApplicationException(ex);
  }

  public static WebApplicationException doesNotExistException(DiscoveryError errorEnum, String resource, String id) {
    ErrorType error = DiscoveryError.getErrorType(errorEnum, id, resource);
    Response ex = Response.status(Response.Status.NOT_FOUND).entity(format(error)).build();
    return new WebApplicationException(ex);
  }

  public static WebApplicationException resourceExistsException(DiscoveryError errorEnum, String resource, String parameter) {
    ErrorType error = DiscoveryError.getErrorType(errorEnum, resource, parameter);
    Response ex = Response.status(Response.Status.CONFLICT).entity(format(error)).build();
    return new WebApplicationException(ex);
  }

  public static InvalidVersionException invalidVersionException(DiscoveryError errorEnum, String resource, XMLGregorianCalendar request, XMLGregorianCalendar actual) {
    ErrorType error = DiscoveryError.getErrorType(errorEnum, resource, "request=" + request.toString() + ", actual=" + actual.toString());
    Response ex = Response.status(Response.Status.BAD_REQUEST).entity(format(error)).build();
    return new InvalidVersionException(ex, request, actual);
  }
}

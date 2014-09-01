/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.jersey.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 *
 * @author hacksaw
 */
public class CustomNotFoundException extends WebApplicationException {
 
  /**
  * Create a HTTP 404 (Not Found) exception.
  */
  public CustomNotFoundException() {
    super(Response.status(Response.Status.NOT_FOUND).build());
  }
 
  /**
  * Create a HTTP 404 (Not Found) exception.
  * @param message the String that is the entity of the 404 response.
  */
  public CustomNotFoundException(String message) {
    super(Response.status(Response.Status.NOT_FOUND).
    entity(message).type("text/plain").build());
  }
}

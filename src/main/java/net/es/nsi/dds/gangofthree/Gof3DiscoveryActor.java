package net.es.nsi.dds.gangofthree;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.dds.client.RestClient;
import net.es.nsi.dds.jaxb.NmlParser;
import net.es.nsi.dds.jaxb.NsaParser;
import net.es.nsi.dds.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.jaxb.nsa.InterfaceType;
import net.es.nsi.dds.jaxb.nsa.NsaType;
import net.es.nsi.dds.lib.DocHelper;
import net.es.nsi.dds.messages.Message;
import net.es.nsi.dds.messages.StartMsg;
import net.es.nsi.dds.messages.TimerMsg;
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.util.XmlUtilities;
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ChunkedInput;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class Gof3DiscoveryActor extends UntypedAbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  private Client client;

  /**
   *
   */
  @Override
  public void preStart() {
    client = RestClient.getInstance().get();
  }

  /**
   *
   * @param msg
   */
  @Override
  public void onReceive(Object msg) {
    log.debug("[Gof3DiscoveryActor] onReceive {}", Message.getDebug(msg));

    if (msg instanceof Gof3DiscoveryMsg) {
      Gof3DiscoveryMsg message = (Gof3DiscoveryMsg) msg;

      // Read the NSA discovery document.
      if (!discoverNSA(message)) {
        // No update so return.
        log.debug("[Gof3DiscoveryActor] no new NSA document {}", message.getNsaURL());
        return;
      }

      // Read the associated Topology document.
      for (Map.Entry<String, Long> entry : message.getTopology().entrySet()) {
        Long result = discoverTopology(message.getNsaId(), entry.getKey(), entry.getValue());
        if (result != null) {
          message.setTopologyLastModified(entry.getKey(), result);
        }
      }

      // Send an updated discovery message back to the router.
      getSender().tell(message, getSelf());
    } else {
      log.error("[Gof3DiscoveryActor] unhandled message {}", Message.getDebug(msg));
      unhandled(msg);
    }

    log.debug("[Gof3DiscoveryActor] onReceive done.");
  }

  /**
   * Performs a Gang of Three NSA discovery by reading an NSA description file from a pre-defined URL.
   *
   * @param message Discovery instructions for the target NSA.
   * @return Returns true if the NSA document was successfully discovered, false otherwise.
   */
  private boolean discoverNSA(Gof3DiscoveryMsg message) {
    log.debug("discoverNSA: nsa= {}, lastModifiedTime= {}",
            message.getNsaURL(), new Date(message.getNsaLastModifiedTime()));

    long time = message.getNsaLastModifiedTime();
    Response response = null;
    try {
      WebTarget nsaTarget = client.target(message.getNsaURL());

      log.debug("discoverNSA: querying with If-Modified-Since: " + DateUtils.formatDate(new Date(time)));

      // Some NSA do not support the NSI_NSA_V1 application string so
      // accept any encoding for the document.
      response = nsaTarget.request("*/*") // NsiConstants.NSI_NSA_V1
              .header("If-Modified-Since", DateUtils.formatDate(new Date(time), DateUtils.PATTERN_RFC1123))
              .get();

      Date lastModified = response.getLastModified();
      if (lastModified != null) {
        log.debug("discoverNSA: time=" + new Date(time) + ", lastModified=" + new Date(lastModified.getTime()));
        time = lastModified.getTime();
      } else {
        log.debug("discoverNSA: lastModified returned null");
      }

      log.debug("discoverNSA: response status " + response.getStatus());

      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        // Read the NSA description into a string buffer to avoid parsing
        // errors on goofy characters.
        StringBuilder result = new StringBuilder();
        try (ChunkedInput<String> chunkedInput = response.readEntity(new GenericType<ChunkedInput<String>>() {})) {
          String chunk;
          while ((chunk = chunkedInput.read()) != null) {
            result.append(chunk);
          }
        }

        // Now parse the string into an NSA object.
        NsaType nsa = NsaParser.getInstance().xml2Jaxb(NsaType.class, result.toString());

        // We have a document to process.
        if (nsa == null) {
          // Clear the topology URL and lastModified dates for this
          // error.
          log.error("discoverNSA: NSA document is empty from endpoint " + message.getNsaURL());
          message.setNsaLastModifiedTime(0L);
          message.clearTopology();
          return false;
        }

        message.setNsaId(nsa.getId());

        // Find the topology endpoint for the next step.
        Map<String, Long> oldTopologyRefs = new HashMap<>(message.getTopology());
        for (InterfaceType inf : nsa.getInterface()) {
          if (NsiConstants.NSI_TOPOLOGY_V2.equalsIgnoreCase(inf.getType().trim())) {
            String url = inf.getHref().trim();
            Long previous = oldTopologyRefs.remove(url);
            if (previous == null) {
              message.addTopology(url, 0L);
            }
          }
        }

        // Now we clean up the topology URL that are no longer in the
        // NSA description document.
        for (String url : oldTopologyRefs.keySet()) {
          message.removeTopologyURL(url);
        }

        XMLGregorianCalendar cal = XmlUtilities.longToXMLGregorianCalendar(time);

        if (!DocHelper.addNsaDocument(nsa, cal)) {
          log.debug("discoverNSA: addNsaDocument() returned false " + message.getNsaURL());
          return false;
        }
      } else if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
        // We did not get an updated document.
        log.debug("discoverNSA: NSA document not modified " + message.getNsaURL());
      } else {
        log.error("discoverNSA: get of NSA document failed " + response.getStatus() + ", url=" + message.getNsaURL());
        // TODO: Should we clear the topology URL and lastModified
        // dates for errors and route back?
        return false;
      }
    } catch (IllegalStateException ex) {
      log.error("discoverNSA: failed to retrieve NSA document from endpoint {}, ex = {}", message.getNsaURL(), ex);
      return false;
    } catch (JAXBException ex) {
      log.error("discoverNSA: invalid document returned from endpoint {}, ex = {}", message.getNsaURL(), ex);
      return false;
    } catch (DatatypeConfigurationException ex) {
      log.error("discoverNSA: NSA document failed to create lastModified {}, ex = {}", message.getNsaURL(), ex);
      return false;
    } catch (IllegalArgumentException ex) {
      log.error("discoverNSA: Failed to create NSA description document for {}, ex = {} ", message.getNsaURL(), ex);
    } catch (ProcessingException ex) {
      log.error("discoverNSA: SSL exception retrieving NSA description document for {}, ex = {} ",
              message.getNsaURL(), ex);
      return false;
    } finally {
      if (response != null) {
        // Close the response to avoid leaking.
        log.debug("discoverNSA: closing response.");
        response.close();
      } else {
        log.debug("discoverNSA: not closing response.");
      }
    }

    message.setNsaLastModifiedTime(time);

    log.debug("discoverNSA: exiting for nsa=" + message.getNsaURL() + " with lastModifiedTime=" + new Date(time));

    return true;
  }

  /**
   *
   */
  private Long discoverTopology(String nsaId, String url, Long lastModifiedTime) {
    log.debug("discoverTopology: topology=" + url);

    //Client client = ClientBuilder.newClient(clientConfig);
    long time = lastModifiedTime;
    Response response = null;
    try {
      WebTarget topologyTarget = client.target(url);
      response = topologyTarget.request("*/*") // MediaTypes.NSI_TOPOLOGY_V2
              .header("If-Modified-Since", DateUtils.formatDate(new Date(time), DateUtils.PATTERN_RFC1123))
              .get();

      log.debug("discoverNSA: response status " + response.getStatus());

      Date lastModified = response.getLastModified();
      if (lastModified != null) {
        log.debug("discoverTopology: time=" + new Date(time) + ", lastModified=" + new Date(lastModified.getTime()));
        time = lastModified.getTime();
      } else {
        log.debug("discoverTopology: lastModified is null time=" + new Date(time));
      }

      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        // Read the NML topology into a string buffer to avoid parsing
        // errors on goofy characters.
        StringBuilder result = new StringBuilder();
        try (ChunkedInput<String> chunkedInput = response.readEntity(new GenericType<ChunkedInput<String>>() {})) {
          String chunk;
          while ((chunk = chunkedInput.read()) != null) {
            result.append(chunk);
          }
        }

        // Now parse the string into an NML Topology object.
        NmlTopologyType nml = NmlParser.getInstance().xml2Jaxb(NmlTopologyType.class, result.toString());

        if (nml != null) {
          // Temporary fix to update out any old serviceType definitions.
          ServiceDefinitionConverter.convert(nml);

          // Add the document.
          XMLGregorianCalendar cal;
          cal = XmlUtilities.longToXMLGregorianCalendar(time);

          if (DocHelper.addTopologyDocument(nml, cal, nsaId) == false) {
            log.debug("discoverTopology: addTopologyDocument() returned false " + url);
          }
        } else {
          // TODO: Should we clear the topology URL and lastModified
          // dates for errors and route back?
          log.error("discoverTopology: Topology document is empty from endpoint " + url);
        }
      } else if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
        // We did not get an updated document.
        log.debug("discoverTopology: Topology document not modified (" + response.getLastModified() + ") for topology=" + url);
      } else {
        log.error("discoverTopology: get of Topology document failed " + response.getStatus() + ", topology=" + url);
      }
    } catch (IllegalStateException ex) {
      // TODO: Should we clear the topology URL and lastModified
      // dates for errors and route back?
      log.error("discoverTopology: failed to retrieve Topology document from endpoint " + url);
      time = 0L;
    } catch (JAXBException ex) {
      log.error("discoverTopology: invalid document returned from endpoint " + url);
      time = 0L;
    } catch (DatatypeConfigurationException ex) {
      log.error("discoverTopology: Topology document failed to create lastModified " + url);
      time = 0L;
    } catch (IllegalArgumentException ex) {
      log.error("discoverTopology: Failed to convert NML topology document from endpoint " + url);
      time = 0L;
    } finally {
      if (response != null) {
        // Close the response to avoid leaking.
        log.debug("discoverTopology: closing response.");
        response.close();
      } else {
        log.debug("discoverTopology: not closing response.");
      }
    }

    log.debug("discoverTopology: exiting for topology=" + url + " with lastModifiedTime=" + new Date(time));
    return time;
  }
}

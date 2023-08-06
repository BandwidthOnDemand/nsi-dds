package net.es.nsi.dds.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import javax.xml.transform.TransformerException;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.config.ConfigurationManager;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.dao.RemoteSubscription;
import net.es.nsi.dds.dao.RemoteSubscriptionCache;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.DomParser;
import net.es.nsi.dds.jaxb.configuration.PeerURLType;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.lib.Decoder;
import net.es.nsi.dds.provider.DiscoveryProvider;
import net.es.nsi.dds.provider.Document;
import net.es.nsi.dds.provider.Subscription;
import net.es.nsi.dds.util.NsiConstants;
import org.apache.http.client.utils.URIBuilder;

/**
 * This is an absolute hack.  I couldn't get the Jersey JSP configuration to
 * work with Grizzly.
 *
 * @author hacksaw
 */
@Slf4j
@Path("/dds/portal")
public class Portal {
    @GET
    @Produces({ MediaType.TEXT_HTML })
    public Response portal() throws Exception {

        final String header = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "<title>DDS Portal</title>\n" +
            "<style>\n" +
            "table, th, td {\n" +
            "    border: 1px solid black;\n" +
            "    border-collapse: collapse;\n" +
            "}\n" +
            "th, td {\n" +
            "    padding: 5px;\n" +
            "    text-align: left;\n" +
            "}\n" +
            "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<h1>Document Distribution Service</h1>\n";

        final String serverInformationOpen =
                "<h2>Server Configuation</h2>\n" +
                "<p>";

        final String peerTableOpen =
            "<table border=\"1\" style=\"width:100%\">\n" +
            "  <tr>\n" +
            "    <th>Peer Type</th>\n" +
            "    <th>Remote URL</th>\n" +
            "  </tr>";

        final String peerTableClose = "</table>\n";

        final String serverInformationClose =
                "</p>";

        final String subcriptionTableOpen =
            "<h2>Subscriptions</h2>\n" +
            "<p>" +
            "<table border=\"1\" style=\"width:100%\">\n" +
            "  <tr>\n" +
            "    <th>subscriptionId</th>\n" +
            "    <th>Version</th>\n" +
            "    <th>requesterId</th>\n" +
            "    <th>Callback</th>\n" +
            "  </tr>";
        final String subcriptionTableClose = "</table></p>\n";


        final String remoteSubcriptionTableOpen =
            "<h2>My Subscriptions</h2>\n" +
            "<p>" +
            "<table border=\"1\" style=\"width:100%\">\n" +
            "  <tr>\n" +
            "    <th>subscriptionId</th>\n" +
            "    <th>Version</th>\n" +
            "    <th>Created</th>\n" +
            "    <th>LastSuccessfulAudit</th>\n" +
            "    <th>Remote DDS URL</th>\n" +
            "  </tr>";
        final String remoteSubcriptionTableClose = "</table></p>\n";

        final String documentTableOpen =
            "<h2>Documents</h2>\n" +
            "<p>" +
            "<table border=\"1\" style=\"width:100%\">\n" +
            "  <tr>\n" +
            "    <th>Owning nsaId</th>\n" +
            "    <th>Document type</th>\n" +
            "    <th>Document id</th>\n" +
            "    <th>Operation</th>\n" +
            "  </tr>";

        final String documentTableClose = "</table></p>\n";

        final String footer =
            "</body>\n" +
            "</html>";

        // Get a handle to the DDS configuration information.
        DdsConfiguration config = (DdsConfiguration) ConfigurationManager.INSTANCE.getApplicationContext().getBean("ddsConfiguration");
        RemoteSubscriptionCache cache = (RemoteSubscriptionCache) ConfigurationManager.INSTANCE.getApplicationContext().getBean("remoteSubscriptionCache");
        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();

        // Include the page header.
        StringBuilder sb = new StringBuilder(header);

        // Now the server information.
        sb.append(serverInformationOpen);
        sb.append("<b>nsaId:</b> "); sb.append(config.getNsaId()); sb.append("</br>");
        sb.append("<b>baseURL:</b> "); sb.append(config.getBaseURL()); sb.append("</br>");
        sb.append(peerTableOpen);
        for (PeerURLType peer : config.getDiscoveryURL()) {
            sb.append("<tr>\n");
            sb.append("<td>"); sb.append(peer.getType()); sb.append("</td>");
            sb.append("<td>"); sb.append(peer.getValue()); sb.append("</td>");
            sb.append("<tr>\n");
        }
        sb.append(peerTableClose);
        sb.append(serverInformationClose);

        // Now for the dynamic subscriptions.
        sb.append(subcriptionTableOpen);
        for (Subscription subscription : discoveryProvider.getSubscriptions()) {
            sb.append("<tr>\n");
            sb.append("<td><a href=\"");
                sb.append(subscription.getSubscription().getHref());
                sb.append("\">");
                sb.append(subscription.getSubscription().getId());
                sb.append("</a></td>");
            sb.append("<td>"); sb.append(subscription.getSubscription().getVersion()); sb.append("</td>");
            sb.append("<td>"); sb.append(subscription.getSubscription().getRequesterId()); sb.append("</td>");
            sb.append("<td>"); sb.append(subscription.getSubscription().getCallback()); sb.append("</td>");
            sb.append("<tr>\n");
        }
        sb.append(subcriptionTableClose);

        if (cache != null) {
          sb.append(remoteSubcriptionTableOpen);
          for (RemoteSubscription rm : cache.values()) {
            sb.append("<tr>\n");
            sb.append("<td><a href=\"");
                sb.append(rm.getSubscription().getHref());
                sb.append("\">");
                sb.append(rm.getSubscription().getId());
                sb.append("</a></td>");
            sb.append("<td>"); sb.append(rm.getSubscription().getVersion()); sb.append("</td>");
            sb.append("<td>"); sb.append(rm.getCreated()); sb.append("</td>");
            sb.append("<td>"); sb.append(rm.getLastSuccessfulAudit()); sb.append("</td>");
            sb.append("<td>"); sb.append(rm.getDdsURL()); sb.append("</td>");
            sb.append("<tr>\n");

          }
          sb.append(remoteSubcriptionTableClose);
        }

        // Finally we add the document information held within DDS.
        sb.append(documentTableOpen);

        Collection<Document> documents = discoveryProvider.getDocuments(null, null, null, null);
        for (Document document : documents) {
            DocumentType documentSummary = document.getDocumentSummary();
            String metadata = getOperationURL("metadata", documentSummary.getNsa(), documentSummary.getType(), documentSummary.getId());
            String contents = getOperationURL("contents", documentSummary.getNsa(), documentSummary.getType(), documentSummary.getId());

            sb.append("<tr>\n");
            sb.append("<td>"); sb.append(documentSummary.getNsa()); sb.append("</td>");
            sb.append("<td>"); sb.append(documentSummary.getType()); sb.append("</td>");
            sb.append("<td>"); sb.append(documentSummary.getId()); sb.append("</td>");
            sb.append("<td>");
            sb.append("<a href=\""); sb.append(metadata); sb.append("\">meta-data</a>, ");
            sb.append("<a href=\""); sb.append(contents); sb.append("\">contents</a>"); sb.append("</td>");
            sb.append("<tr>\n");
        }

        sb.append(documentTableClose);
        sb.append(footer);

        return Response.ok().entity(sb.toString()).build();
    }

    @GET
    @Path("/metadata")
    @Produces({ MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    public Response metadata(
            @QueryParam("nsa") String nsa,
            @QueryParam("type") String type,
            @QueryParam("id") String id) {
        // Lookup the target document.
        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        Document document = discoveryProvider.getDocument(nsa, type, id, null);
        if (document == null || document.getDocument() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            String xml = DdsParser.getInstance().xmlFormatter(document.getDocument());
            return Response.ok().header("Content-Type", MediaType.APPLICATION_XML).entity(xml).build();
        } catch (JAXBException ex) {
            return Response.serverError().entity("An internal error has occured: " + ex.getLocalizedMessage()).build();
        }
    }

    @GET
    @Path("/contents")
    @Produces({ MediaType.APPLICATION_XML })
    public Response contents(
            @QueryParam("nsa") String nsa,
            @QueryParam("type") String type,
            @QueryParam("id") String id) throws Exception {
        DiscoveryProvider discoveryProvider = ConfigurationManager.INSTANCE.getDiscoveryProvider();
        Document document = discoveryProvider.getDocument(nsa, type, id, null);
        if (document == null || document.getDocument() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            DocumentType doc = document.getDocument();

            String result;
            String encoding = MediaType.APPLICATION_XML;
            if (NsiConstants.NSI_DOC_TYPE_NSA_V1.equalsIgnoreCase(doc.getType()) ||
                    NsiConstants.NSI_DOC_TYPE_TOPOLOGY_V2.equalsIgnoreCase(doc.getType())) {
                // This is a known XML type so treat is as such.
                org.w3c.dom.Document dom = Decoder.decode2Dom(
                        doc.getContent().getContentTransferEncoding(),
                        doc.getContent().getContentType(),
                        doc.getContent().getValue());
                result = DomParser.prettyPrint(dom);
            }
            else {
                // Unknown type so decode and send as text.
                result = Decoder.decode2String(
                        doc.getContent().getContentTransferEncoding(),
                        doc.getContent().getContentType(),
                        doc.getContent().getValue());
                encoding = MediaType.TEXT_PLAIN;
            }

            return Response.ok().header("Content-Type", encoding).entity(result).build();
        } catch (IOException | TransformerException ex) {
            return Response.serverError().entity("An internal error has occured: " + ex.getLocalizedMessage()).build();
        }
    }

    private String getOperationURL(String operation, String nsa, String type, String id) throws URISyntaxException, MalformedURLException {
        DdsConfiguration config = (DdsConfiguration) ConfigurationManager.INSTANCE.getApplicationContext().getBean("ddsConfiguration");
        URL url = new URL(config.getBaseURL());
        URI uri = new URIBuilder(url.toURI())
            .setPath(url.getPath() + "/portal/" + operation)
            .setParameter("nsa", nsa)
            .setParameter("type", type)
            .setParameter("id", id)
            .build();
        return uri.toString();
    }
}

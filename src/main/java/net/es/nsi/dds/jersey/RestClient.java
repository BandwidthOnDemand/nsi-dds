package net.es.nsi.dds.jersey;

import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.dds.schema.NsiConstants;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RestClient {
    private final Client client;

    public RestClient() {
        ClientConfig clientConfig = configureClient();
        client = ClientBuilder.newClient(clientConfig);
    }

    public static RestClient getInstance() {
        RestClient restClient = SpringApplicationContext.getBean("restClient", RestClient.class);
        return restClient;
    }

    public static ClientConfig configureClient() {
        ClientConfig clientConfig = new ClientConfig();

        // Values are in milliseconds
        //clientConfig.property(ClientProperties.READ_TIMEOUT, 2000);
        //clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 1000);

        // We want to use the Apache connector for chunk POST support.
        clientConfig.connectorProvider(new ApacheConnectorProvider());

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.setMaxTotal(80);
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);

        // Configure the JerseyTest client for communciations with PCE.
        clientConfig.register(new MoxyXmlFeature());
        clientConfig.register(new MoxyJsonFeature());
        clientConfig.register(new LoggingFilter(java.util.logging.Logger.getGlobal(), true));
        clientConfig.register(FollowRedirectFilter.class);
        clientConfig.property(MarshallerProperties.NAMESPACE_PREFIX_MAPPER, Utilities.getNameSpace());
        clientConfig.property(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@");
        clientConfig.property(MarshallerProperties.JSON_NAMESPACE_SEPARATOR, '.');
        //clientConfig.property(DefaultApacheHttpClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE, 0);
        return clientConfig;
    }

    public Client get() {
        return client;
    }

    public void close() {
        client.close();
    }

    private static class FollowRedirectFilter implements ClientResponseFilter
    {
        private final static Logger log = LoggerFactory.getLogger(FollowRedirectFilter.class);

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException
        {
            if (requestContext == null || responseContext == null || responseContext.getStatus() != Response.Status.FOUND.getStatusCode()) {
               return;
            }

            log.debug("Processing redirect for " + requestContext.getMethod() + " " + requestContext.getUri().toASCIIString() + " to " + responseContext.getLocation().toASCIIString());

            Client inClient = requestContext.getClient();
            Object entity = requestContext.getEntity();
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            String method = requestContext.getMethod();
            Response resp;
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                resp = inClient.target(responseContext.getLocation()).request(requestContext.getMediaType()).headers(headers).method(requestContext.getMethod(), Entity.entity(new GenericEntity<JAXBElement<?>>((JAXBElement<?>)entity) {}, NsiConstants.NSI_DDS_V1_XML));
            }
            else {
                resp = inClient.target(responseContext.getLocation()).request(requestContext.getMediaType()).headers(headers).method(requestContext.getMethod());
            }

            responseContext.setEntityStream((InputStream) resp.getEntity());
            responseContext.setStatusInfo(resp.getStatusInfo());
            responseContext.setStatus(resp.getStatus());
            responseContext.getHeaders().putAll(resp.getStringHeaders());

            log.debug("Processing redirect with result " + resp.getStatus());
        }
    }
}

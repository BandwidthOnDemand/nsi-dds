/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.jaxb.dds.DocumentListType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.client.ClientConfig;

/**
 *
 * @author hacksaw
 */
public class Main {
    private final static HttpConfig testServer = new HttpConfig("localhost", "9800", "net.es.nsi.dds.client");
    private final static ObjectFactory factory = new ObjectFactory();

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void main(String[] args) throws Exception {
        ClientConfig clientConfig = RestClient.configureClient();
        Client client = ClientBuilder.newClient(clientConfig);

        WebTarget webGet = client.target("http://localhost:8401/dds");
        Response response = webGet.request(MediaType.APPLICATION_JSON).get();

        System.out.println("Get result " + response.getStatus());
        if (Response.Status.OK.getStatusCode()!= response.getStatus()) {
            System.err.println("Read failed.");
        }

        final ChunkedInput<DocumentListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentListType>>() {});
        DocumentListType chunk;
        DocumentListType documents = null;
        while ((chunk = chunkedInput.read()) != null) {
            System.out.println("Chunk received...");
            documents = chunk;
        }
        response.close();


        // Configure the local test client callback server.
        TestServer.INSTANCE.start(testServer);

    }
}

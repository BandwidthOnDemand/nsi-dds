/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.dds.api.jaxb.ObjectFactory;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.jersey.RestClient;
import org.glassfish.jersey.client.ClientConfig;

/**
 *
 * @author hacksaw
 */
public class Main {
    private final static HttpConfig testServer = new HttpConfig() {
        { setUrl("http://localhost:9800/"); setPackageName("net.es.nsi.dds.client"); }
    };

    private final static String callbackURL = testServer.getUrl() + "dds";

    private final static ObjectFactory factory = new ObjectFactory();
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void main(String[] args) throws Exception {
        ClientConfig clientConfig = new ClientConfig();
        RestClient.configureClient(clientConfig);
        Client client = ClientBuilder.newClient(clientConfig);

        WebTarget webGet = client.target("http://localhost:8401/dds");
        Response response = webGet.request(MediaType.APPLICATION_JSON).get();

        System.out.println("Get result " + response.getStatus());

        // Configure the local test client callback server.
        TestServer.INSTANCE.start(testServer);
    }
}

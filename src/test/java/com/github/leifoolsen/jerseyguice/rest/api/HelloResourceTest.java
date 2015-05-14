package com.github.leifoolsen.jerseyguice.rest.api;

import com.github.leifoolsen.jerseyguice.domain.HelloBean;
import com.github.leifoolsen.jerseyguice.main.JettyMain;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HelloResourceTest {
    private static final int PORT = 8080;

    private static Server server;
    private static WebTarget target;

    @BeforeClass
    public static void setUp() throws Exception {
        // Start the server
        server  = JettyMain.startJetty(PORT);

        // create the client
        Client c = ClientBuilder.newClient();
        target = c.target(server.getURI()).path("api");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        JettyMain.stopJetty(server);
    }

    @Test
    public void sayHelloToGuice() {
        final Response response = target
                .path("say")
                .path("hello")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        HelloBean hello = response.readEntity(HelloBean.class);
        assertNotNull(hello);
        assertEquals("Hello from Guice injected service!", hello.say());
    }

    @Test
    public void getApplicationWadl() throws Exception {
        final Response response = target
                .path("application.wadl")
                .request(MediaType.APPLICATION_XML)
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String wadl = response.readEntity(String.class);
        assertTrue(wadl.length() > 0);
    }
}

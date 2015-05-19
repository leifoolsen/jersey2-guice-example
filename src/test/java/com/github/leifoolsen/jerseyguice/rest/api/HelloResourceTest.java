package com.github.leifoolsen.jerseyguice.rest.api;

import com.github.leifoolsen.jerseyguice.domain.HelloBean;
import com.github.leifoolsen.jerseyguice.main.JettyRunner;
import com.github.leifoolsen.jerseyguice.rest.application.ApplicationConfig;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class HelloResourceTest {
    private static final int PORT = 8080;

    private static Server server;
    private static WebTarget target;

    @BeforeClass
    public static void setUp() throws Exception {
        // Start the server
        server = JettyRunner.start("/", PORT);

        // create the client
        Client c = ClientBuilder.newClient();
        target = c.target(server.getURI()).path(ApplicationConfig.APPLICATION_PATH);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        JettyRunner.stop(server);
    }

    @Test
    public void sayHelloToGuice() {
        final Response response = target
                .path("say")
                .path("hello")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        HelloBean hello = response.readEntity(HelloBean.class);
        assertNotNull(hello);
        assertThat(hello.say(), equalTo("Hello from Guice injected service!"));
    }

    @Test
    public void getApplicationWadl() throws Exception {
        final Response response = target
                .path("application.wadl")
                .request(MediaType.APPLICATION_XML)
                .get();

        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        String wadl = response.readEntity(String.class);
        assertThat(wadl.length(), greaterThan(0));
    }
}

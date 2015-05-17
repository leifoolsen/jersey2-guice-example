package com.github.leifoolsen.jerseyguice.rest.api;

import com.github.leifoolsen.jerseyguice.domain.HelloBean;
import com.github.leifoolsen.jerseyguice.service.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Singleton
@Path("say")
@Produces(MediaType.APPLICATION_JSON)
public class HelloResource {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private HelloService helloService;

    @Inject
    public HelloResource(HelloService helloService) {
        this.helloService = helloService;
        logger.debug("{} created with with Guice injected service", HelloResource.class.getSimpleName());
    }

    @GET
    @Path("hello")
    public HelloBean sayHello() {
        return helloService.sayHello();
    }

}

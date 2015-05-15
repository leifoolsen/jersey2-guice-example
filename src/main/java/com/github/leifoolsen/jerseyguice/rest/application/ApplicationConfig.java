package com.github.leifoolsen.jerseyguice.rest.application;

import com.github.leifoolsen.jerseyguice.service.HelloGuiceModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.ApplicationPath;

@WebServlet(loadOnStartup = 1)
@ApplicationPath("/api/*")
public class ApplicationConfig extends ResourceConfig {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String APPLICATION_PATH;

    static {
        String appPath = "";
        if(ApplicationConfig.class.isAnnotationPresent(ApplicationPath.class)) {
            // Remove '/*' from @ApplicationPath, e.g:  "/api/*" -> /api
            appPath = ApplicationConfig.class.getAnnotation(ApplicationPath.class).value();
            appPath = appPath.substring(0, appPath.endsWith("/*")
                    ? appPath.lastIndexOf("/*") : appPath.length()-1);
        }
        APPLICATION_PATH = appPath;
    }

    @Inject
    public ApplicationConfig(ServiceLocator serviceLocator) {

        // Jersey uses java.util.logging. Bridge to slf4j
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // Scans during deployment for JAX-RS components in packages
        packages("com.github.leifoolsen.jerseyguice.rest");


        // Enable LoggingFilter & output entity.
        //registerInstances(new LoggingFilter(java.util.logging.Logger.getLogger(this.getClass().getName()), true));

        // Guice
        Injector injector = Guice.createInjector(new HelloGuiceModule());

        // Guice HK2 bridge
        // See e.g. https://github.com/t-tang/jetty-jersey-HK2-Guice-boilerplate
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
        GuiceIntoHK2Bridge bridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
        bridge.bridgeGuiceInjector(injector);

        logger.debug("'{}' initialized", getClass().getName());
    }
}


/*
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet();
        classes.add(HelloResource.class);

        return classes;
    }
}
*/

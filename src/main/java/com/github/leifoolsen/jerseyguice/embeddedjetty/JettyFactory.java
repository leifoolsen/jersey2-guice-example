package com.github.leifoolsen.jerseyguice.embeddedjetty;

import com.github.leifoolsen.jerseyguice.util.FileUtil;
import com.github.leifoolsen.jerseyguice.util.SneakyThrow;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class JettyFactory {
    private static final Logger logger = LoggerFactory.getLogger(JettyFactory.class);

    private static final int MIN_THREADS = 8;
    private static final int MAX_THREADS = 200;
    private static final boolean DAEMON = false;
    private static final String DAEMON_NAME = null;
    private static final String HOST = "localhost";
    private static final int IDLE_TIMEOUT = 5000;
    private static final boolean ENABLE_DIRECTORY_LISTING = true;
    private static final String CONTEXT_PATH = "/";
    private static final String RESOURCE_BASE = "webapp";

    private JettyFactory() {}

    public static Server createServer(final int port) {


        // Create server
        QueuedThreadPool threadPool = new QueuedThreadPool(MIN_THREADS, MAX_THREADS);
        threadPool.setDaemon(DAEMON);
        if(DAEMON_NAME != null) threadPool.setName(DAEMON_NAME);


        final Server server = new Server(threadPool);

        // Configuration classes. This gives support for multiple features.
        // The annotationConfiguration is required to support annotations like @WebServlet
        // See: http://www.eclipse.org/jetty/documentation/current/configuring-webapps.html
        // See: http://www.eclipse.org/jetty/documentation/current/using-annotations-embedded.html
        try {
            Class.forName("org.eclipse.jetty.annotations.AnnotationConfiguration");
            Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(server);
            classlist.addBefore(
                    "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",    // Processes a WEB-INF/web.xml file
                    "org.eclipse.jetty.webapp.WebInfConfiguration",         // Extracts war, orders jars and defines classpath
                    "org.eclipse.jetty.annotations.AnnotationConfiguration" // Scan container and web app jars looking for @WebServlet, @WebFilter, @WebListener etc
            );
            logger.info("Annotation processing is enabled.");
        }
        catch (ClassNotFoundException e) {
            logger.info("Annotation processing is not enabled, missing dependency on jetty-annotations.");
        }

        // Connector
        final ServerConnector connector = new ServerConnector(server);
        connector.setHost(HOST);
        connector.setPort(port);
        connector.setIdleTimeout(IDLE_TIMEOUT);
        server.addConnector(connector);


        // Handlers
        final HandlerCollection handlers = new HandlerCollection();

        // WebAppContext
        handlers.addHandler(createWebApp());

        server.setHandler(handlers);
        server.setStopAtShutdown(true);
        server.setStopTimeout(5000);

        return server;
    }


    private static WebAppContext createWebApp() {
        // The WebAppContext is the entity that controls the environment in
        // which a web application lives and breathes.
        final WebAppContext webApp = new WebAppContext();

        // Parent loader priority is a class loader setting that Jetty accepts.
        // By default Jetty will behave like most web containers in that it will
        // allow your application to replace non-server libraries that are part of the
        // container. Setting parent loader priority to true changes this behavior.
        // Read more here: http://wiki.eclipse.org/Jetty/Reference/Jetty_Classloading
        webApp.setParentLoaderPriority(true);

        // fail if the web app does not deploy correctly
        webApp.setThrowUnavailableOnStartupException(true);

        // Add an AliasCheck instance to possibly permit aliased resources
        //webapp.addAliasCheck(new AllowSymLinkAliasChecker());

        // Directory listing
        webApp.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed",
                Boolean.valueOf(ENABLE_DIRECTORY_LISTING).toString());

        // Context path
        webApp.setContextPath(CONTEXT_PATH);

        // Web app location
        final Resource baseResource = Resource.newClassPathResource(RESOURCE_BASE);
        final URI baseResourceLocation = baseResource.getURI();
        webApp.setBaseResource(baseResource);
        logger.debug("Base resource URI: {}.", baseResourceLocation);


        // AnntationConfiguration class scans annotations via its scanForAnnotations(WebAppContext) method.
        // In the method AnnotationConfiguration class scans following path.
        //   container jars
        //   WEB-INF/classes
        //   WEB-INF/libs
        //
        // We also need Jetty to scan the the webapp jar
        // (and/or "target/classes" and "target/test-classes") directory for annotations
        String warpath = null;

        if("jar".equals(baseResourceLocation.getScheme())) {
            final String s = FileUtil.toURL(baseResourceLocation).getPath();
            warpath = Splitter.on('!').trimResults().splitToList(s).get(0); // remove  e.g. "!/webapp"
            webApp.setWar(warpath);
        }

        final Path classesPath = FileUtil.classesPath();
        if(classesPath != null) {
            final Path testClassesPath = FileUtil.testClassesPath();
            warpath = Joiner.on(";")
                    .skipNulls()
                    .join(warpath,
                            classesPath.toAbsolutePath(),
                            testClassesPath != null ? testClassesPath.toAbsolutePath() : null
                    );
        }

        if(warpath != null) {
            logger.info("Extra class path @ {}", warpath);
            webApp.setExtraClasspath(warpath);
        }

        // URL location = JettyFactory.class.getProtectionDomain().getCodeSource().getLocation();

        return webApp;
    }


    /**
     * Start embedded Jetty server.
     */
    public static void start(final Server server) {
        try {
            server.start();
            //server.dump(System.err);
        }
        catch (Exception e) {
            SneakyThrow.exception(e);
        }
        logger.info("Jetty started at: " + server.getURI());
    }

    /**
     * Stop embedded Jetty server.
     */
    public static void stop(final Server server) {
        logger.info("Stopping Jetty at: " + server.getURI());
        try {
            server.stop();
            server.join();
        }
        catch (Exception e) {
            SneakyThrow.exception(e);
        }
        logger.debug("Jetty stopped!");
    }

    /**
     * Start embedded Jetty server and wait until the server is done executing.
     */
    public static void startAndWait(final Server server) {
        start(server);

        String s = "\n" +
                ">>>\n" +
                ">>> PRESS ENTER TO STOP JETTY\n" +
                ">>>";

        System.out.println(s);

        try {
            System.in.read();
        }
        catch (IOException e) {
            // Do nothing
        }
        finally {
            if(server.isRunning()) {
                stop(server);
            }
        }
    }
}

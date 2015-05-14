package com.github.leifoolsen.jerseyguice.main;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.primitives.Ints;
import com.google.common.reflect.ClassPath;

import eu.nets.oss.jetty.ContextPathConfig;
import eu.nets.oss.jetty.EmbeddedJettyBuilder;
import eu.nets.oss.jetty.PropertiesFileConfig;
import eu.nets.oss.jetty.StaticConfig;
import eu.nets.oss.jetty.StdoutRedirect;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;



public class JettyMain {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws Exception {
        int port = args.length >= 1 ? MoreObjects.firstNonNull(Ints.tryParse(args[0]), DEFAULT_PORT) : DEFAULT_PORT;


        Server server = JettyMain.startJetty(port);

        ClassPath cp = ClassPath.from(Thread.currentThread().getContextClassLoader());
        String appPath = "";
        for (ClassPath.ClassInfo classInfo : cp.getTopLevelClassesRecursive("com.github.leifoolsen")) {
            Class<?> clazz = classInfo.load();
            if(clazz.isAnnotationPresent(ApplicationPath.class)) {
                // Remove '*' from @ApplicationPath, e.g:  "/api/*" -> /api/
                appPath = CharMatcher.is('*').removeFrom(
                        MoreObjects.firstNonNull(clazz.getAnnotation(ApplicationPath.class).value(), ""));

                break;
            }
        }

        URI applicationURI = UriBuilder
                .fromUri(server.getURI())
                .path(appPath)
                .path("application.wadl")
                .build();

        System.out.println(String.format(">>> Application WADL @: %s", applicationURI));

        if(!EmbeddedJettyBuilder.isStartedWithAppassembler()) {
            System.out.println(String.format(">>> Hit ENTER to stop"));
            System.in.read();
            JettyMain.stopJetty(server);
        }
    }

    public static Server startJetty(final int port) {
        // Properties "app.home", "app.name", "app.repo" from "./appassembler/bin/startapp"
        boolean onServer = EmbeddedJettyBuilder.isStartedWithAppassembler();


        ContextPathConfig config;
        if (onServer) {
            config = new HerokuConfig(new PropertiesFileConfig());
        } else {
            config = new StaticConfig("/", port);
        }

        final EmbeddedJettyBuilder builder = new EmbeddedJettyBuilder(config, !onServer);

        if (onServer) {
            builder.addHttpAccessLogAtRoot();
        }
        StdoutRedirect.tieSystemOutAndErrToLog();

        System.out.println(String.format(">>> Starting Jetty"));

        EmbeddedJettyBuilder.ServletContextHandlerBuilder<WebAppContext> ctx =
                builder.createRootWebAppContext("", Resource.newClassPathResource("/webapp"));

        WebAppContext handler = ctx.getHandler();

        // AnntationConfiguration class scans annotations via its scanForAnnotations(WebAppContext) method.
        // In the method AnnotationConfiguration class scans following path.
        //   container jars
        //   WEB-INF/classes
        //   WEB-INF/libs
        //
        // In exploded mode we also need Jetty to scan the "target/classes" directory for annotations
        URL classes = JettyMain.class.getProtectionDomain().getCodeSource().getLocation();
        if(classes != null) {
            handler.setExtraClasspath(classes.getPath());
        }

        // Parent loader priority is a class loader setting that Jetty accepts.
        // By default Jetty will behave like most web containers in that it will
        // allow your application to replace non-server libraries that are part of the
        // container. Setting parent loader priority to true changes this behavior.
        // Read more here: http://wiki.eclipse.org/Jetty/Reference/Jetty_Classloading
        handler.setParentLoaderPriority(true);

        // fail if the web app does not deploy correctly
        handler.setThrowUnavailableOnStartupException(true);

        // disable directory listing
        handler.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

        try {
            builder.createServer().startJetty();
        }
        catch (Exception e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            Throwables.propagate(e);
        }

        return builder.getServer();
    }

    public static void stopJetty(final Server server) {
        System.out.println(String.format(">>> Stopping Jetty"));
        try {
            server.stop();
            server.join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class HerokuConfig implements ContextPathConfig {

        private final PropertiesFileConfig delegate;

        private HerokuConfig(PropertiesFileConfig delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getContextPath() {
            return delegate.getContextPath();
        }

        @Override
        public int getPort() {
            String port = System.getenv("PORT");
            return port == null ? delegate.getPort() : Integer.parseInt(port);
        }
    }
}

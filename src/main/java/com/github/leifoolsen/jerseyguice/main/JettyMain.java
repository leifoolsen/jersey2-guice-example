package com.github.leifoolsen.jerseyguice.main;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;


public class JettyMain {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws Exception {

        final Map<String, String> m = argsToMap(args);

        int port = Ints.tryParse(m.get("port"));

        if(m.containsKey("shutdown")) {
            JettyMain.attemptShutdown(port, m.get("token"));
        }
        else {
            JettyMain.attemptStartup(port);
        }
    }

    private static Map<String, String> argsToMap(String[] args) {
        // Convert args: "port"  " = " "8087" -> "port=8007"
        final String j = Joiner.on(' ').skipNulls().join(args);
        final List<String> argsList = Splitter.on(',').omitEmptyStrings().splitToList(j);
        final Map<String, String> argsMap = Maps.newHashMap();

        for (String s : argsList) {
            // Can not use Splitter.withKeyValueSeparator as it treats "=" differently from " = "
            List<String> p = Splitter.on('=').trimResults().splitToList(s);
            argsMap.put(p.get(0), p.size() > 1 ? p.get(1) : "");
        }
        argsMap.putIfAbsent("port", Integer.toString(DEFAULT_PORT));
        return argsMap;
    }

    private static void attemptShutdown(final int port, final String shutdownToken) {
        try {
            URL url = new URL("http://localhost:" + port + "/shutdown?token=" + shutdownToken);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.getResponseCode();
            System.out.println(">>> Shutting down server @ " + url + ": " + connection.getResponseMessage());
        }
        catch (SocketException e) {
            System.out.println(">>> Server not running @ http://localhost:" + port);
            // Okay - the server is not running
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void attemptStartup(final int port) throws IOException {

        Server server = JettyMain.startJetty(port);

        // Find class annotated with @ApplicationPath
        ClassPath cp = ClassPath.from(Thread.currentThread().getContextClassLoader());
        String appPath = "";
        for (ClassPath.ClassInfo classInfo : cp.getTopLevelClassesRecursive("com.github.leifoolsen")) {
            Class<?> clazz = classInfo.load();
            if(clazz.isAnnotationPresent(ApplicationPath.class)) {

                // Remove '/*' from @ApplicationPath, e.g:  "/api/*" -> /api
                appPath = clazz.getAnnotation(ApplicationPath.class).value();
                appPath = appPath.substring(0, appPath.endsWith("/*")
                        ? appPath.lastIndexOf("/*") : appPath.length() - 1);

                break;
            }
        }

        URI applicationURI = UriBuilder
                .fromUri(server.getURI())
                .path(appPath)
                .path("application.wadl")
                .build();

        System.out.println(String.format(">>> Application WADL @: %s", applicationURI));

        // Ctrl+C does not work inside IntelliJ
        if(!EmbeddedJettyBuilder.isStartedWithAppassembler()) {
            System.out.println(">>> Hit ENTER to stop");
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


        // TODO: Get token from config or as a param
        // TODO: How to do this with eu.nets.oss.jetty.EmbeddedJettyBuilder???
        //builder.addHandlerAtRoot(new HandlerBuilder<Handler>(new ShutdownHandler("foo")));


        if (onServer) {
            builder.addHttpAccessLogAtRoot();
        }

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
            handler.setExtraClasspath(classes.getPath());  // TODO: Set path to test-classes if needed
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

        StdoutRedirect.tieSystemOutAndErrToLog();
        System.out.println(">>> Starting Jetty");

        try {
            builder.createServer();
            builder.startJetty();
        }
        catch (Exception e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            Throwables.propagate(e);
        }

        return builder.getServer();
    }

    public static void stopJetty(final Server server) {
        System.out.println(">>> Stopping Jetty");
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

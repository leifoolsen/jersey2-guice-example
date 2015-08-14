package com.github.leifoolsen.jerseyguice.main;

import com.github.leifoolsen.jerseyguice.embeddedjetty.JettyFactory;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.primitives.Ints;
import org.eclipse.jetty.server.Server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Main {
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_CONTEXT_PATH = "/";

    private Main() {}

    public static void main(String[] args) throws Exception {

        final Map<String, String> argsMap = Main.argsToMap(args);
        int port = MoreObjects.firstNonNull(Ints.tryParse(argsMap.get("port")), DEFAULT_PORT);

        Server server = JettyFactory.createServer(8080);
        JettyFactory.startAndWait(server);

    }

    private static Map<String, String> argsToMap(String[] args) {
        // Convert args, e.g: port = 8087 context-path /myapp shutdown token=secret
        //                 -> port=8007", "context-path=/myapp", "shutdown= ", "token=secret"

        final Map<String, String> argsMap = new HashMap<>();

        if(args != null) {
            int i = 0;
            int n = args.length;
            while (i < n) {
                if (args[i].startsWith("port") ||
                    args[i].startsWith("token") ||
                    args[i].startsWith("context-path") ||
                    args[i].startsWith("contextPath")) {

                    List<String> p = Splitter.on('=').trimResults().splitToList(args[i]);
                    String name = p.get(0).equals("contextPath") ? "context-path" : p.get(0);
                    String value = p.size() > 1 ? p.get(1) : i < n-1 ? args[++i] : "";
                    if(args[i].equals("=")) value = i < n-1 ? args[++i] : "";

                    argsMap.put(name, value);
                }
                else if (args[i].equals("shutdown")) {
                    argsMap.put(args[i], "");
                }
                i++;
            }
        }
        argsMap.putIfAbsent("port", Integer.toString(DEFAULT_PORT));
        argsMap.putIfAbsent("context-path", DEFAULT_CONTEXT_PATH);
        return argsMap;
    }
}

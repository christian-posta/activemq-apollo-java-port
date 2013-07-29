/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.apollo.broker.web;

import org.apache.activemq.apollo.broker.Broker;
import org.apache.activemq.apollo.dto.BrokerDTO;
import org.apache.activemq.apollo.dto.WebAdminDTO;
import org.apache.activemq.apollo.util.BaseService;
import org.apache.activemq.apollo.util.FileSupport;
import org.apache.activemq.apollo.util.URISupport;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.servlet.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.apache.activemq.apollo.util.FunctionUtils.stripPrefix;
import static org.apache.activemq.apollo.util.FunctionUtils.stripSuffix;


/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class JettyWebServer extends BaseService implements WebServer {

    private Logger LOG = LoggerFactory.getLogger(getClass().getName());
    private Broker broker;
    private Server server;
    private List<WebAdminDTO> webAdmins;
    private List<URI> uriAddresses;

    public JettyWebServer(Broker broker) {
        super(Dispatch.createQueue());
        this.broker = broker;
    }

    @Override
    public void update(final Task onComplete) {
        List<WebAdminDTO> newList = broker.getConfig().web_admins;
        if (newList != webAdmins) {
            // restart to pickup the changes!
            stop(new Task() {
                @Override
                public void run() {
                    start(onComplete);
                }
            });
        }else {
            onComplete.run();
        }
    }

    @Override
    public URI[] uris() {
        return (URI[]) this.uriAddresses.toArray();
    }


    @Override
    protected void _start(final Task onCompleted) {
        Broker.BLOCKABLE_THREAD_POOL.execute(new Task() {
            @Override
            public void run() {
                synchronized (this) {
                    // Explicitly set the Jetty Log impl to avoid
                    // the NPE raised at https://issues.apache.org/jira/browse/APLO-264
                    try {
                        org.eclipse.jetty.util.log.Log.setLog(new Slf4jLog());
                    } catch (Exception e) {
                        // ignore this...
                    }

                    BrokerDTO config = broker.getConfig();
                    File webappPath = webapp(broker.getTmp());
                    if (webappPath == null) {
                        LOG.warn("Administration interface cannot be started: webapp resources not found");
                    } else {
                        // start up the admin interface
                        LOG.debug("Starting administration interface");

                        if (broker.getTmp() != null) {
                            try {
                                System.setProperty("scalate.workdir", new File(broker.getTmp(), "scalate").getCanonicalPath());
                            } catch (IOException e) {
                                LOG.warn("Could not set scalate workdir. Admin interface may not function properly");
                            }
                        }


                        HashMap<String, Handler> contexts = new HashMap<String, Handler>();
                        HashMap<String, Connector> connectors = new HashMap<String, Connector>();


                        webAdmins = config.web_admins;
                        for (WebAdminDTO webAdmin : webAdmins) {
                            String bind = webAdmin.bind == null ? "http://127.0.0.1:61680" : webAdmin.bind;
                            URI bindUri;
                            try {
                                bindUri = new URI(bind);
                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e);
                            }

                            String prefix = "/" + stripPrefix(bindUri.getPath(), "/");

                            String scheme = bindUri.getScheme();
                            String host = bindUri.getHost();
                            int port = bindUri.getPort();

                            Map<String, String> query = null;
                            try {
                                query = URISupport.parseQuery(bindUri.getQuery());

                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e);
                            }

                            String corsOrigin = query.get("cors_origin");

                            if (port == -1) {
                                if (scheme.equals("http")) {
                                    port = 80;
                                }else if (scheme.equals("https")) {
                                    port = 433;
                                }else {
                                    throw new RuntimeException("Invalid 'web_admin' bind setting. Protocol scheme should be 'http' or 'https'");
                                }
                            }

                            // Only add the connector if has not been added yet
                            String connectorId = scheme + "://" + host + ":" + port;
                            if (!connectors.containsKey(connectorId)) {
                                Connector connector = getConnector(scheme);

                                connector.setHost(host);
                                connector.setPort(port);
                                connectors.put(connectorId, connector);
                            }

                            // only add the app context if not yet added...
                            if (!contexts.containsKey(prefix)) {
                                WebAppContext context = new WebAppContext();
                                context.setContextPath(prefix);
                                try {
                                    context.setWar(webappPath.getCanonicalPath());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                context.setClassLoader(Broker.classLoader());

                                EnumSet<DispatcherType> ALL = EnumSet.allOf(DispatcherType.class);
                                if (corsOrigin != null && !corsOrigin.trim().isEmpty()) {
                                    Set<String> origins = resolveOrigins(corsOrigin);
                                    context.addFilter(new FilterHolder(new AllowAnyOriginFilter(origins)), "/*", ALL);
                                }

                                context.addFilter(new FilterHolder(new Filter() {
                                    @Override
                                    public void init(FilterConfig filterConfig) throws ServletException { }

                                    @Override
                                    public void destroy() { }

                                    @Override
                                    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                                        request.setAttribute("APOLLO_BROKER", broker);
                                        chain.doFilter(request, response);
                                    }
                                }), "/*", ALL);

                                if (broker.getTmp() != null) {
                                    context.setTempDirectory(broker.getTmp());
                                }


                                contexts.put(prefix, context);
                            }


                        }

                        HandlerList contextList = new HandlerList();
                        for (Handler h : contexts.values()) {
                            contextList.addHandler(h);
                        }

                        server = new Server();
                        server.setHandler(contextList);
                        server.setConnectors((Connector[]) connectors.values().toArray());
                        server.setThreadPool(new ExecutorThreadPool(Broker.BLOCKABLE_THREAD_POOL));
                        try {
                            server.start();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }


                        Iterator<String> iter = contexts.keySet().iterator();
                        for (Connector c : connectors.values()) {
                            int localPort = c.getLocalPort();
                            String prefix = iter.next();
                            String scheme = c instanceof SslSelectChannelConnector ? "https" : "http";
                            try {
                                URI uri = new URI(scheme, null, c.getHost(), localPort, prefix, null, null);
                                broker.getConsoleLog().info("Administration interface available at: {}", uri);
                                uriAddresses.add(uri);

                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    }

                    onCompleted.run();
                }
            }
        });

    }

    private Set<String> resolveOrigins(String corsOrigin) {
        String[] origins = corsOrigin.split(",");
        LinkedHashSet<String> rc = new LinkedHashSet<String>();
        for (String s : origins) {
            rc.add(s.trim());
        }

        return rc;
    }

    private Connector getConnector(String scheme) {
        if (scheme.equals("http")) {
            return new SelectChannelConnector();
        }else {
            SSLContext sslContext;
            if (broker.getKeyStorage() != null) {
                String protocol = "TLS";
                try {
                    sslContext = SSLContext.getInstance(protocol);
                    sslContext.init(broker.getKeyStorage().createKeyManagers(), broker.getKeyStorage().createTrustManagers(), null);

                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (KeyManagementException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    sslContext = SSLContext.getDefault();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
            SslSelectChannelConnector rc = new SslSelectChannelConnector();
            SslContextFactory factory = rc.getSslContextFactory();
            factory.setSslContext(sslContext);
            factory.setWantClientAuth(true);
            return rc;
        }
    }

    private static File webapp(File tmp) {
        ClassLoader classLoader = JettyWebServer.class.getClassLoader();

        File rc = null;
        if (System.getProperty("apollo.webapp") != null) {
            rc = new File(System.getProperty("apollo.webapp"));
        } else {
            try {
                // so the apollo-web module will put together this jar file by default and put in this location...
                // but it could be elsewhere on the classpath in other set ups
                Enumeration<URL> resources = classLoader.getResources("META-INF/services/org.apache.activemq.apollo/webapp-resources.jar");
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    rc = new File(tmp, "webapp-resources");
                    rc.mkdirs();

                    explodeWebappResources(rc, url);
                }

            } catch (IOException e) {
                // no worries, just continue on
            }


            // the war might be on the classpath...
            if (rc == null) {
                String bootClazz = "org/apache/activemq/apollo/web/Boot.class";
                URL url = classLoader.getResource(bootClazz);
                if (rc == null) {
                    rc = null;
                } else {
                    if (url.getProtocol().equals("file")) {
                        // we are probably being run from an IDE :)
                        File classesDir = new File(stripSuffix(url.getFile(), "/" + bootClazz));
                        if (new File(classesDir, "../../src/main/webapp").isDirectory()) {
                            rc = new File(classesDir, "../../src/main/webapp");
                        }else if (new File(classesDir, "../../apollo-web/src/main/webapp").isDirectory()) {
                            rc = new File(classesDir, "../../apollo-web/src/main/webapp");
                        }
                        else {
                            rc = null;
                        }
                    }
                    else {
                        rc = null;
                    }
                }
            }

        }
        return rc;
    }

    private static void explodeWebappResources(File rc, URL url) throws IOException {
        JarInputStream is = null;
        try {
            is = new JarInputStream(url.openStream());
            JarEntry entry = is.getNextJarEntry();
            while (entry != null) {
                // if we found a dir, create the dir..
                if (entry.isDirectory()) {
                    new File(rc, entry.getName()).mkdirs();
                }else {
                    // otherwise, it's a file, and we should explode it
                    // a little nasty with all these nested try-finally...
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(new File(rc, entry.getName()));
                        FileSupport.copy(is, fos);
                    }finally {
                        if (fos != null) {
                            fos.close();
                        }
                    }
                }
            }
        }finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Override
    protected void _stop(final Task onCompleted) {
        // jetty operations can block, so don't do them on hawtdispatch threads...
        // but we'll also need to resort to traditional mutex synchronization
        Broker.BLOCKABLE_THREAD_POOL.execute(new Task() {

            @Override
            public void run() {
                synchronized (this) {
                    if (server != null) {
                        try {
                            server.stop();
                        } catch (Exception e) {
                            LOG.info("Could not shutdown jetty server properly when stopping the web server", e);
                        }
                        server = null;
                        uriAddresses = null;
                    }

                    onCompleted.run();
                }

            }
        });

    }

    @Override
    public String toString() {
        return "jetty webserver";
    }
}

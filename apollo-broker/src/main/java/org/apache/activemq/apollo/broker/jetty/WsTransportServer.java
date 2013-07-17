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
package org.apache.activemq.apollo.broker.jetty;

import org.apache.activemq.apollo.broker.Broker;
import org.apache.activemq.apollo.broker.BrokerAware;
import org.apache.activemq.apollo.broker.web.AllowAnyOriginFilter;
import org.apache.activemq.apollo.util.BaseService;
import org.apache.activemq.apollo.util.IntrospectionSupport;
import org.apache.activemq.apollo.util.URISupport;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.TaskWrapper;
import org.fusesource.hawtdispatch.transport.TransportServer;
import org.fusesource.hawtdispatch.transport.TransportServerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class WsTransportServer extends BaseService implements TransportServer, BrokerAware {
    private Logger LOG = LoggerFactory.getLogger(getClass().getName());


    private static final String WS = "ws";
    private static final String WSS = "wss";

    private Executor blockingExecutor;
    private TransportServerListener transportServerListener;
    private Boolean binaryTransfers = true;
    private String corsOrigin = null;
    private Broker broker;
    private Server server;
    private Connector connector;
    private final URI uri;
    private DispatchQueue acceptDispatchQueue = dispatchQueue;
    private ArrayBlockingQueue<WebSocketTransport> pendingConnects = new ArrayBlockingQueue<WebSocketTransport>(100);


    public WsTransportServer(URI uri) {
        super(Dispatch.createQueue("web sockets dispatch queue"));
        this.uri = uri;
    }

    @Override
    protected void _start(Task onCompleted) {
        synchronized (this) {

            setJettyLog();

            introspectParameters();

            acceptDispatchQueue = dispatchQueue.createQueue("accept: " + uri);

            try {
                final String prefix = computePrefix();
                final String scheme = uri.getScheme();
                final String host = uri.getHost();
                final int port = resolvePort(scheme, uri.getPort());
                final Map<String, String> options = URISupport.parseParamters(uri);

                this.connector = resolveConnector(scheme, options);

                this.connector.setHost(host);
                this.connector.setPort(port);

                ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SECURITY);
                contextHandler.setContextPath(prefix);

                if (corsOrigin != null && !corsOrigin.trim().isEmpty()) {
                    EnumSet ALL = EnumSet.allOf(DispatcherType.class);
                    Set<String> origins = resolveOrigins();
                    contextHandler.addFilter(new FilterHolder(new AllowAnyOriginFilter(origins)), "/*", ALL);
                }

                contextHandler.addServlet(new ServletHolder(new WebSocketServlet() {

                    @Override
                    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                        resp.setContentType("application/json");
                        resp.getOutputStream().println("{}");
                    }

                    // this is what connects up the jetty websockets impl to our transports.. and jetty will make callbacks
                    // to onMessage, onOpen, onClose, etc.
                    @Override
                    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
                        return new WebSocketTransport(WsTransportServer.this, request, protocol);
                    }
                }), "/");

                this.server = new Server();
                this.server.setHandler(contextHandler);
                this.server.setConnectors(new Connector[]{ connector });
                this.server.setThreadPool(new ExecutorThreadPool((ExecutorService) blockingExecutor));
                this.server.start();

                onCompleted.run();


            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

    private Set<String> resolveOrigins() {
        String[] origins = corsOrigin.split(",");
        LinkedHashSet<String> rc = new LinkedHashSet<String>();
        for (String s : origins) {
            rc.add(s.trim());
        }

        return rc;
    }

    private Connector resolveConnector(String scheme, Map<String, String> options) {
        if (isWebSocket(scheme)) {
            return new SelectChannelConnector();
        }else if (isSecureWebSocket(scheme)) {
            SSLContext context = resolveSslContext();
            SslSelectChannelConnector channelConnector = new SslSelectChannelConnector();
            SslContextFactory sslSettings = channelConnector.getSslContextFactory();
            sslSettings.setSslContext(context);
            if (options.containsKey("client_auth")) {
                String clientAuth = options.get("client_auth");
                if (clientAuth != null) {
                    sslSettings.setWantClientAuth(true);
                }else if("want".equals(clientAuth)) {
                    sslSettings.setWantClientAuth(true);
                }else if("need".equals(clientAuth)) {
                    sslSettings.setNeedClientAuth(true);
                }else if("none".equals(clientAuth)) {
                    // don't do anything? default to whatever jetty uses
                } else {
                    LOG.warn("Invalid setting for the wss protocol 'client_auth' query option. Please set to one of: none, want, need");
                }

            }
            return channelConnector;

        }  else {
            throw new RuntimeException("Invalid bind protocol: " + scheme);
        }
    }

    private SSLContext resolveSslContext() {

        if (broker.getKeyStorage() != null) {
            String protocol = "TLS";
            try {
                SSLContext context = SSLContext.getInstance(protocol);
                context.init(broker.getKeyStorage().createKeyManagers(), broker.getKeyStorage().createTrustManagers(), null);
                return context;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else {
            LOG.warn("You are using a transport that expects the broker's key storage to be configured.");
            try {
                return SSLContext.getDefault();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private int resolvePort(String scheme, int port) {
        if (isWebSocket(scheme)) {
            if (port == -1) {
                return 80;
            }else {
                return port;
            }
        }else if (isSecureWebSocket(scheme)) {
            if (port == -1) {
                return 443;
            }else {
                return port;
            }

        }else {
            throw new RuntimeException("Invalid bind protocol: " + scheme);
        }
    }

    private boolean isWebSocket(String scheme) {
        return WS.equals(scheme);
    }

    private boolean isSecureWebSocket(String scheme) {
        return WSS.equals(scheme);
    }

    private void introspectParameters() {
        try {
            IntrospectionSupport.setProperties(this, URISupport.parseParamters(uri));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void setJettyLog() {
        // Explicitly set the Jetty Log impl to avoid the NPE raised at
        // https://issues.apache.org/jira/browse/APLO-264
        try {
            org.eclipse.jetty.util.log.Log.setLog(new Slf4jLog());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void _stop(Task onCompleted) {
        onCompleted.run();
    }

    @Override
    public void setBroker(Broker broker) {
        this.broker = broker;
    }



    @Override
    public void setTransportServerListener(TransportServerListener transportServerListener) {
        this.transportServerListener = transportServerListener;
    }

    @Override
    public String getBoundAddress() {
        String prefix =  computePrefix();
        try {
            return new URI(uri.getScheme() + "://" + uri.getHost() + ":" + connector.getLocalPort() + prefix).toString();
        } catch (Exception e) {
            broker.getConsoleLog().warn("Could not compute WS address", e);
            return "Could not compute WS address";
        }
    }

    private String computePrefix() {
        return "/" + getPathWithoutPrefix("/");
    }
    private String getPathWithoutPrefix(String prefix) {
        if(uri.getPath().startsWith(prefix)) return uri.getPath().substring(0, prefix.length());

        return "";
    }

    /**
     * Used to connect up/bridge the websocket impl from jetty to the broker's accept listeners to
     * process the transport
     */
    public void fireAccept() {
        acceptDispatchQueue.execute(new Task() {

            @Override
            public void run() {
                final WebSocketTransport transport = pendingConnects.poll();
                if (transport != null) {
                    if (serviceState.isStarted()) {
                        try {
                            transportServerListener.onAccept(transport);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else {
                        // run this on the blocking executor so not to stall this hawtdispatch thread
                        blockingExecutor.execute(new Task() {

                            @Override
                            public void run() {
                                transport.getConnection().close();
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public SocketAddress getSocketAddress() {
        return new InetSocketAddress(uri.getHost(), connector.getLocalPort());
    }

    @Override
    public void setDispatchQueue(DispatchQueue dispatchQueue) {
        this.dispatchQueue = dispatchQueue;
    }

    @Override
    public void suspend() {
        acceptDispatchQueue.suspend();
    }

    @Override
    public void resume() {
        acceptDispatchQueue.resume();
    }

    @Override
    public Executor getBlockingExecutor() {
        return this.blockingExecutor;
    }

    @Override
    public void setBlockingExecutor(Executor executor) {
        this.blockingExecutor = executor;
    }

    @Override
    public void start(final Runnable runnable) throws Exception {
        super.start(new TaskWrapper(runnable));
    }

    @Override
    public void stop(final Runnable runnable) throws Exception {
        super.stop(new TaskWrapper(runnable));
    }

    public void addPendingConnects(WebSocketTransport transport) {
        try {
            this.pendingConnects.put(transport);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

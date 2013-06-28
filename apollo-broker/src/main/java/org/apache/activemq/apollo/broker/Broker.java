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
package org.apache.activemq.apollo.broker;

import org.apache.activemq.apollo.dto.AcceptingConnectorDTO;
import org.apache.activemq.apollo.dto.BrokerDTO;
import org.apache.activemq.apollo.dto.VirtualHostDTO;
import org.apache.activemq.apollo.filter.FilterException;
import org.apache.activemq.apollo.filter.Filterable;
import org.apache.activemq.apollo.filter.XPathExpression;
import org.apache.activemq.apollo.filter.XalanXPathEvaluator;
import org.apache.activemq.apollo.util.ApolloThreadPool;
import org.apache.activemq.apollo.util.BaseService;
import org.apache.activemq.apollo.util.ClassFinder;
import org.apache.activemq.apollo.util.FileSupport;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.BufferInputStream;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.util.BufferPools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class Broker extends BaseService {

    private Logger LOG = LoggerFactory.getLogger(getClass().getName());

    // JMX mbea server
    private static MBeanServer MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();
    private static final int SERVICE_TIMEOUT = 1000 * 5;

    public static final ThreadPoolExecutor BLOCKABLE_THREAD_POOL = ApolloThreadPool.INSTANCE;

    public static final Long MAX_JVM_HEAP_SIZE = getMaxJvmHeapSize();

    private static Long getMaxJvmHeapSize() {
        try {
            CompositeData data = (CompositeData) MBEAN_SERVER.getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
            return (Long) data.get("max");
        } catch (Exception e) {
            return Long.valueOf(1024 * 1024 * 1024); // assume default 1GB
        }
    }

    // properties of the runtime environment
    private String version;
    private String os;
    private String jvm;
    private Long maxFDLimit;
    private Object container;

    private File tmp;

    private BrokerDTO config = new BrokerDTO();

    private final BufferPools bufferPools = new BufferPools();
    private long connectionIdCounter;
    private KeyStorage keyStorage;

    private int autoTunedSendReceiveBufferSize;
    private PeriodStat currentPeriod = new PeriodStat();;
    private long maxConnectionsIn5min;

    private volatile VirtualHost defaultVirtualHost = null;
    private final Map<AsciiBuffer, VirtualHost> virtualHosts = new LinkedHashMap<AsciiBuffer, VirtualHost>();
    private final Map<AsciiBuffer, VirtualHost> virtualHostsByHostname = new LinkedHashMap<AsciiBuffer, VirtualHost>();

    // This is a copy of the virtual_hosts_by_hostname variable which
    // can be accessed by any thread.
    private volatile Map<AsciiBuffer, VirtualHost> cowVirtualHostsByHostname = new HashMap<AsciiBuffer, VirtualHost>();

    private Map<String, Connector> connectors = new LinkedHashMap<String, Connector>();;
    private Map<Long, BrokerConnection> connections = new LinkedHashMap<Long, BrokerConnection>();


    private volatile long now = System.currentTimeMillis();

    public Broker() {
        super(Dispatch.createQueue("broker"));
        initXPathEvaluator();
        initDefaultVirtualHostConfig();
        initDefaultConnectorConfig();
    }

    private void initDefaultConnectorConfig() {
        AcceptingConnectorDTO rc = new AcceptingConnectorDTO();
        rc.id = "default";
        rc.bind = "tcp://0.0.0.0:0";
        config.connectors.add(rc);
    }

    private void initDefaultVirtualHostConfig() {
        VirtualHostDTO rc = new VirtualHostDTO();
        rc.id = "default";
        rc.host_names.add("localhost");
        config.virtual_hosts.add(rc);
    }

    // Make sure XPATH selector support is enabled and optimize a little.
    private void initXPathEvaluator() {
        XPathExpression.XPATH_EVALUATOR_FACTORY = new XPathExpression.XPathEvaluatorFactory() {

            @Override
            public XPathExpression.XPathEvaluator create(String xpath) {
                return new XalanXPathEvaluator(xpath) {
                    @Override
                    public boolean evaluate(Filterable m) throws FilterException {
                        Buffer body = m.getBodyAs(Buffer.class);
                        if (body != null) {
                            return evaluate(new InputSource(new BufferInputStream(body)));
                        } else {
                            return super.evaluate(m);
                        }
                    }
                };
            }
        };
    }

    public ClassLoader classLoader() {
        return ClassFinder.getDefaultClassLoader();
    }

    public String version() {
        if (this.version == null) {
            InputStream in = getClass().getResourceAsStream("version.txt");
            version = FileSupport.readText(in);
        }
        return version;

    }

    public String os() {
        if (os == null) {
            os = System.getProperty("os.name") + " " + System.getProperty("os.version");

            // todo:ceposta more detailed OS info?
            // check the scala source, there is a good way to get more detailed OS info
        }

        return os;
    }

    public String jvm() {
        if (jvm == null) {
            String vendor = System.getProperty("java.vendor");
            String version = System.getProperty("java.version");
            String vm = System.getProperty("java.vm.name");
            jvm = String.format("%s %s (%s)", vm, version, vendor);
        }

        return jvm;
    }

    public Long maxFDLimit() {
        if (maxFDLimit == null) {
            if( System.getProperty("os.name").toLowerCase().startsWith("windows") ) {
                maxFDLimit = Long.MAX_VALUE;
            } else {
                try {
                    ObjectName osMbean = new ObjectName("java.lang:type=OperatingSystem");
                    Object obj = MBEAN_SERVER.getAttribute(osMbean, "MaxFileDescriptorCount");
                    if (obj instanceof Long) {
                        maxFDLimit = (Long) obj;
                    } else {
                        maxFDLimit = Long.MAX_VALUE;
                    }
                } catch (Exception e) {
                    LOG.debug("Could not figure out the FD Limit, using MAX");
                    maxFDLimit = Long.MAX_VALUE;
                }

            }
        }

        return maxFDLimit;
    }

    public void tuneSendReceiveBuffers() {
        maxConnectionsIn5min = Math.max(maxConnectionsIn5min, currentPeriod.getMaxConnections());
        if (maxConnectionsIn5min == 0) {
            autoTunedSendReceiveBufferSize = 64 * 1024;
        } else {
            Long x = MAX_JVM_HEAP_SIZE;

            // Lets only use 1/8th of the heap for connection buffers.
            x = x / 8;

            // 1/2 for send buffers, the other 1/2 for receive buffers.
            x = x / 2;
            // Ok, how much space can we use per connection?

            x = x / maxConnectionsIn5min;
            // Drop the bottom bits so that we are working /w 1k increments.
            x = x & 0xFFFFFF00;

            // Constrain the result to be between a 2k and a 64k buffer.
            autoTunedSendReceiveBufferSize = Math.min(Math.max(x.intValue(), 2 * 1024), 64 * 1024);

            // Basically this means that we will use a 64k send/receive buffer
            // for the first 1024 connections established and then the buffer
            // size will start getting reduced down until it gets to 2k buffers.
            // Which will occur when you get to about 32,000 connections.

            for (Connector c : this.connectors.values()) {
                c.updateBufferSettings();
            }
        }
    }

    public Object getContainer() {
        return container;
    }

    public void setContainer(Object container) {
        this.container = container;
    }

    public BrokerDTO getConfig() {
        return config;
    }

    public void setConfig(BrokerDTO config) {
        this.config = config;
    }

    public File getTmp() {
        return tmp;
    }

    public void setTmp(File tmp) {
        this.tmp = tmp;
    }

    public Map<String, Connector> getConnectors() {
        return connectors;
    }

    public void setConnectors(Map<String, Connector> connectors) {
        this.connectors = connectors;
    }

    public Map<Long, BrokerConnection> getConnections() {
        return connections;
    }

    public void setConnections(Map<Long, BrokerConnection> connections) {
        this.connections = connections;
    }

    public Logger getConnectionLog() {
        return LOG;
    }

    public Logger getConsoleLog() {
        return LOG;
    }

    @Override
    protected void _start(Task onCompleted) {
    }

    @Override
    protected void _stop(Task onCompleted) {
    }

    public Long getNextConnectionId() {
        return connectionIdCounter++;
    }

    public KeyStorage getKeyStorage() {
        return keyStorage;
    }

    public void setKeyStorage(KeyStorage keyStorage) {
        this.keyStorage = keyStorage;
    }

    public int getAutoTunedSendReceiveBufferSize() {
        return autoTunedSendReceiveBufferSize;
    }

    public void setAutoTunedSendReceiveBufferSize(int autoTunedSendReceiveBufferSize) {
        this.autoTunedSendReceiveBufferSize = autoTunedSendReceiveBufferSize;
    }

    public PeriodStat getCurrentPeriod() {
        return currentPeriod;
    }

    public long getMaxConnectionsIn5min() {
        return maxConnectionsIn5min;
    }


}

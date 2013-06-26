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

import org.apache.activemq.apollo.dto.BrokerDTO;
import org.apache.activemq.apollo.util.ApolloThreadPool;
import org.apache.activemq.apollo.util.BaseService;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class Broker extends BaseService {

    private Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private static MBeanServer MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();


    public static final Long MAX_JVM_HEAP_SIZE = getMaxJvmHeapSize();

    private static Long getMaxJvmHeapSize() {
        try {
            CompositeData data = (CompositeData) MBEAN_SERVER.getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
            return (Long) data.get("max");
        } catch (Exception e) {
            return Long.valueOf(1024 * 1024 * 1024); // assume default 1GB
        }
    }

    private BrokerDTO config;

    private File tmp;
    private Map<String, Connector> connectors;
    private Map<Long, BrokerConnection> connections;

    public static final ThreadPoolExecutor BLOCKABLE_THREAD_POOL = ApolloThreadPool.INSTANCE;
    private long connectionIdCounter;
    private KeyStorage keyStorage;
    private int autoTunedSendReceiveBufferSize;
    private PeriodStat currentPeriod;
    private long maxConnectionsIn5min;

    public Broker() {
        super(Dispatch.createQueue("broker"));
        connectors = new LinkedHashMap<String, Connector>();
        connections = new LinkedHashMap<Long, BrokerConnection>();
        currentPeriod = new PeriodStat();
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
}

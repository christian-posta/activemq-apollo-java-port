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

import com.sun.corba.se.impl.logging.OMGSystemException;
import org.apache.activemq.apollo.dto.BrokerDTO;
import org.apache.activemq.apollo.util.ApolloThreadPool;
import org.apache.activemq.apollo.util.BaseService;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class Broker extends BaseService {
    private BrokerDTO config;

    private File tmp;
    private Map<String, Connector> connectors;
    private Map<Long, BrokerConnection> connections;

    public static final ThreadPoolExecutor BLOCKABLE_THREAD_POOL = ApolloThreadPool.INSTANCE;



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
}

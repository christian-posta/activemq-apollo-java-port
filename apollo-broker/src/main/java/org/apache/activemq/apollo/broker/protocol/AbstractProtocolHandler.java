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
package org.apache.activemq.apollo.broker.protocol;

import org.apache.activemq.apollo.broker.BrokerConnection;
import org.apache.activemq.apollo.dto.ConnectionStatusDTO;
import org.fusesource.hawtdispatch.Dispatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public abstract class AbstractProtocolHandler implements ProtocolHandler {
    private Logger LOG = LoggerFactory.getLogger(getClass().getName());

    protected BrokerConnection connection;



    public BrokerConnection getConnection() {
        return connection;
    }

    // todo:ceposta what about defer() ?

    @Override
    public void setConnection(BrokerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void onTransportFailure(IOException e) {
        LOG.trace("Error in protocol handler, transport failed: ", e);
        connection.stop(Dispatch.NOOP);
    }

    @Override
    public void onTransportCommand(Object o) {
    }

    @Override
    public void onTransportConnected() {
    }

    @Override
    public void onTransportDisconnected() {
    }

    @Override
    public ConnectionStatusDTO createConnectionStatus(boolean debug) {
        return new ConnectionStatusDTO();
    }
}

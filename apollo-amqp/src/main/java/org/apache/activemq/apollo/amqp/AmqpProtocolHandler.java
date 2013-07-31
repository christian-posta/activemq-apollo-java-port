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
package org.apache.activemq.apollo.amqp;

import org.apache.activemq.apollo.broker.BrokerConnection;
import org.apache.activemq.apollo.broker.protocol.ProtocolHandler;
import org.apache.activemq.apollo.dto.ConnectionStatusDTO;

import java.io.IOException;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class AmqpProtocolHandler implements ProtocolHandler {
    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public BrokerConnection getConnection() {
        return null;
    }

    @Override
    public ConnectionStatusDTO createConnectionStatus(boolean debug) {
        return null;
    }

    @Override
    public void onTransportCommand(Object o) {
    }

    @Override
    public void onTransportFailure(IOException e) {
    }

    @Override
    public void onTransportConnected() {
    }

    @Override
    public void onTransportDisconnected() {
    }

    @Override
    public void setConnection(BrokerConnection connection) {
    }
}

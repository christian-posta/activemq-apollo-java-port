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

import java.io.IOException;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public interface ProtocolHandler {
    public String getProtocol();
    public String getSessionId();
    public BrokerConnection getConnection();

    public ConnectionStatusDTO createConnectionStatus(boolean debug);

    public void onTransportCommand(Object o);

    public void onTransportFailure(IOException e);

    public void onTransportConnected();

    public void onTransportDisconnected();

    public void setConnection(BrokerConnection connection);
}

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

import org.apache.activemq.apollo.dto.ConnectionStatusDTO;
import org.fusesource.hawtdispatch.Dispatch;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class BrokerConnection extends AbstractConnection implements Connection{

    // todo:ceposta NEXT STEP + 0 -- finish broker connection

    private final Long id;
    private ProtocolHandler protocolHandler;
    private final Connector connector;

    public BrokerConnection(Connector connector, Long id) {
        super(Dispatch.createQueue());
        this.id = id;
        this.connector = connector;
    }

    public Connector getConnector() {
        return connector;
    }


    public ConnectionStatusDTO getConnectionStatus(boolean debug) {
        return null;
    }

    public Long getId() {
        return id;
    }

    public ProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    public void setProtocolHandler(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }
}

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

import org.apache.activemq.apollo.broker.security.ResourceKind;
import org.apache.activemq.apollo.broker.transport.TransportFactory;
import org.apache.activemq.apollo.dto.*;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.hawtdispatch.transport.TransportServer;
import org.fusesource.hawtdispatch.transport.TransportServerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Map;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class AcceptingConnector extends AbstractConnector implements Connector {

    private Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final Broker broker;
    private final String id;
    private AcceptingConnectorDTO config;
    private TransportServer transportServer = null;
    private Protocol protocol;
    private Long accepted = 0L;
    private Long connected = 0L;

    // number of messages sent through this connector
    private Long deadMessagesSent = 0L;

    // number of messages received through this connector
    private Long deadMessagesReceived = 0L;

    private Long deadReadCounter = 0L;
    private Long deadWriteCounter = 0L;

    private Integer sendBufferSize;
    private Integer receiveBufferSize;

    private boolean receiveBufferAutoTune = true;
    private boolean sendBufferAutoTune = true;

    public AcceptingConnector(Broker broker, String id) {
        super(broker.getDispatchQueue());
        this.broker = broker;
        this.id = id;

        config = new AcceptingConnectorDTO();
        config.id = id;
        config.bind = "tcp://0.0.0.0:0";

    }


    @Override
    protected void _start(Task onCompleted) {
        assert config != null : "Connector must be configured before it is started!";
        receiveBufferAutoTune = config.receive_buffer_auto_tune != null ? config.receive_buffer_auto_tune : true;
        sendBufferAutoTune = config.send_buffer_auto_tune != null ? config.send_buffer_auto_tune : true;
        accepted = 0L;
        connected = 0L;

        protocol = ProtocolFactory.get(config.protocol != null ? config.protocol : "any");
        try {
            transportServer = TransportFactory.bind(config.bind);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        transportServer.setDispatchQueue(getDispatchQueue());
        transportServer.setBlockingExecutor(Broker.BLOCKABLE_THREAD_POOL);
        transportServer.setTransportServerListener(new BrokerAcceptListener());

        // todo:ceposta finsih this up... it's not finished

    }

    @Override
    protected void _stop(Task onCompleted) {
    }

    @Override
    public Broker getBroker() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public void stopped(BrokerConnection connection) {
    }

    @Override
    public ConnectorTypeDTO getConfig() {
        return null;
    }

    @Override
    public Long getAccepted() {
        return null;
    }

    @Override
    public Long getConnected() {
        return null;
    }

    @Override
    public void update(final ConnectorTypeDTO config, final Task onComplete) {
        if (!serviceState.isStarted() || this.config == config) {
            this.config = (AcceptingConnectorDTO) config;
            onComplete.run();
        } else {
            stop(new Task() {
                @Override
                public void run() {
                    AcceptingConnector.this.config = (AcceptingConnectorDTO) config;
                    start(onComplete);
                }
            });
        }
    }

    @Override
    public SocketAddress getSocketAddress() {
        if (transportServer != null) {
            return transportServer.getSocketAddress();
        }
        return null;
    }

    @Override
    public ServiceStatusDTO getStatus() {
        ConnectorStatusDTO result = new ConnectorStatusDTO();
        result.id = id;
        result.state = serviceState.toString();
        result.state_since = serviceState.since();
        result.connection_counter = accepted;
        result.connected = connected;
        result.protocol = config.protocol != null ? config.protocol : "any";
        result.local_address = getSocketAddress() != null ? getSocketAddress().toString() : "any";

        result.messages_sent = deadMessagesSent;
        result.messages_received = deadMessagesReceived;
        result.read_counter = deadReadCounter;
        result.write_counter = deadWriteCounter;

        for (Map.Entry<Long, BrokerConnection> entry : broker.getConnections().entrySet()) {
            if (entry.getValue().getConnector() == this) {
                BrokerConnection connection = entry.getValue();
                result.connections.add(new LongIdLabeledDTO(entry.getKey(), connection.getTransport().getRemoteAddress().toString()));
                ConnectionStatusDTO connectionStatus = connection.getConnectionStatus(false);
                if (connectionStatus != null) {
                    result.messages_sent += connectionStatus.messages_sent;
                    result.messages_received += connectionStatus.messages_received;
                    result.read_counter += connectionStatus.read_counter;
                    result.write_counter += connectionStatus.write_counter;
                }
            }
        }
        return result;
    }

    public boolean atConnectionLimit() {
        Integer connectionLimit = config.connection_limit;
        if (connectionLimit != null) {
            return connected >= config.connection_limit;
        }else {
            return connected >= Integer.MAX_VALUE;
        }
    }




    @Override
    public ResourceKind getResourceKind() {
        return null;
    }

    @Override
    public void updateBufferSettings() {
    }

    @Override
    public String toString() {
        return "connector: " + config.id;
    }

    class BrokerAcceptListener implements TransportServerListener{

        @Override
        public void onAccept(Transport transport) throws Exception {
            if (protocol != null) {
                transport.setProtocolCodec(protocol.createProtocolCodec(AcceptingConnector.this));
            }

            accepted++;
            connected++;

            BrokerConnection connection = new BrokerConnection(AcceptingConnector.this, broker.getNextConnectionId());
            connection.getDispatchQueue().setLabel(String.format("connection %d to %s", connection.getId(), transport.getRemoteAddress()));
            connection.setProtocolHandler(protocol.createProtocolHandler());
            connection.setTransport(transport);

            broker.getConnections().put(connection.getId(), connection);

            // todo:ceposta NEXT STEP + 1.. fill in protocol, protocolhandlers stuff, connection setters, et,
            // todo:ceposta we also need to finish up Accepting connector (finsih start, stop, update settings, etc)
        }

        @Override
        public void onAcceptError(Exception e) {
            LOG.warn("Error occurred while accepting a client connection", e);

        }
    }

}

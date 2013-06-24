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

import org.apache.activemq.apollo.broker.protocol.ProtocolHandler;
import org.apache.activemq.apollo.dto.ConnectionStatusDTO;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.transport.ProtocolCodec;
import org.fusesource.hawtdispatch.transport.SecuredSession;
import org.fusesource.hawtdispatch.transport.WrappingProtocolCodec;

import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class BrokerConnection extends AbstractConnection implements Connection{


    private final Long id;
    private ProtocolHandler protocolHandler;
    private final Connector connector;

    public BrokerConnection(Connector connector, Long id) {
        this.id = id;
        this.connector = connector;
    }

    public Connector getConnector() {
        return connector;
    }


    public Long getId() {
        return id;
    }

    public String getSessionId(){
        return protocolHandler.getSessionId();
    }

    public <T> T getProtocolCodec(Class<T> clazz) {
        ProtocolCodec rc = transport.getProtocolCodec();
        while (rc != null) {
            if (clazz.isInstance(rc)) {
                return clazz.cast(rc);
            }

            if (rc instanceof WrappingProtocolCodec) {
                rc = ((WrappingProtocolCodec) rc).getNext();
            }
            else {
                rc = null;
            }
        }

        return null;
    }

    public X509Certificate[] getCertificates() {
        if (transport instanceof SecuredSession) {
            return ((SecuredSession) transport).getPeerX509Certificates();
        }else {
            SecuredSession securedSession = getProtocolCodec(SecuredSession.class);
            if(securedSession != null) return securedSession.getPeerX509Certificates();
        }

        return new X509Certificate[0];
    }

    public ConnectionStatusDTO getConnectionStatus(boolean debug) {
        ConnectionStatusDTO result =
                (protocolHandler == null) ?
                        new ConnectionStatusDTO() : protocolHandler.createConnectionStatus(debug);
        result.id = id.toString();
        result.state = serviceState.toString();
        result.state_since = serviceState.since();
        result.protocol = protocolHandler.getProtocol();
        result.connector = connector.getId();
        result.remote_address = transport != null ? transport.getRemoteAddress().toString() : null;
        result.local_address = transport != null ? transport.getLocalAddress().toString() : null;
        result.protocol_session_id = getSessionId();
        ProtocolCodec codec = transport.getProtocolCodec();
        if (codec != null) {
            result.write_counter = codec.getWriteCounter();
            result.read_counter = codec.getReadCounter();
            result.last_read_size = codec.getLastReadSize();
            result.last_write_size = codec.getLastWriteSize();
        }

        return result;
    }


    @Override
    protected void onTransportCommand(Object command) {
        try {
            protocolHandler.onTransportCommand(command);
        } catch (Exception e) {
            onFailure(e);
        }
    }

    @Override
    protected void onTransportConnected() {
        connector.getBroker().getConnectionLog().info("connected: local:{}, remote:{}", transport.getLocalAddress(), transport.getRemoteAddress());
        protocolHandler.onTransportConnected();
    }

    @Override
    protected void onTransportDisconnected() {
        connector.getBroker().getConnectionLog().info("disconnected: local:{}, remote:{}", transport.getLocalAddress(), transport.getRemoteAddress());
        protocolHandler.onTransportDisconnected();
    }

    @Override
    protected void onTransportFailure(IOException e) {
        protocolHandler.onTransportFailure(e);
    }

    @Override
    protected void _start(Task onCompleted) {
        protocolHandler.setConnection(this);
        super._start(onCompleted);
    }

    @Override
    protected void _stop(Task onCompleted) {
        connector.stopped(this);
        super._stop(onCompleted);
    }


    public ProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    public void setProtocolHandler(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }


    @Override
    public String toString() {
        return "id: " + id.toString();
    }
}

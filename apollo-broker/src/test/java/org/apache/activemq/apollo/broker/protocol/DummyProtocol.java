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
import org.apache.activemq.apollo.broker.Connector;
import org.apache.activemq.apollo.dto.ConnectionStatusDTO;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtdispatch.transport.ProtocolCodec;
import org.fusesource.hawtdispatch.transport.Transport;

import java.io.IOException;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class DummyProtocol implements Protocol{
    @Override
    public ProtocolHandler createProtocolHandler() {
        return new ProtocolHandler() {
            @Override
            public String getProtocol() {
                return "dummy-stomp";
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
        };
    }

    @Override
    public ProtocolCodec createProtocolCodec(Connector connector) {
        return new ProtocolCodec() {
            @Override
            public void setTransport(Transport transport) {
            }

            @Override
            public Object read() throws IOException {
                return null;
            }

            @Override
            public void unread(byte[] bytes) {
            }

            @Override
            public long getReadCounter() {
                return 0;
            }

            @Override
            public long getLastReadSize() {
                return 0;
            }

            @Override
            public int getReadBufferSize() {
                return 0;
            }

            @Override
            public int getWriteBufferSize() {
                return 0;
            }

            @Override
            public BufferState write(Object o) throws IOException {
                return null;
            }

            @Override
            public BufferState flush() throws IOException {
                return null;
            }

            @Override
            public boolean full() {
                return false;
            }

            @Override
            public long getWriteCounter() {
                return 0;
            }

            @Override
            public long getLastWriteSize() {
                return 0;
            }
        };
    }

    @Override
    public boolean isIdentifiable() {
        return true;
    }

    @Override
    public int maxIdentificaionLength() {
        return "CONNECT".length();
    }

    @Override
    public boolean matchesIdentification(Buffer buffer) {
        if (buffer.length < "CONNECT".length()) {
            return false;
        }
        else {
            return buffer.startsWith(Buffer.ascii("CONNECT"));
        }
    }

    @Override
    public String getId() {
        return "dummy-stomp";
    }
}

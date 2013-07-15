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

import org.apache.activemq.apollo.broker.Connector;
import org.fusesource.hawtdispatch.transport.ProtocolCodec;
import org.fusesource.hawtdispatch.transport.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class AnyProtocolCodec implements ProtocolCodec {

    private Connector connector;
    private Transport transport;
    protected List<Protocol> protocols = ProtocolFactoryFinder.identifiableProtocols();
    protected ProtocolCodec next;
    protected ByteBuffer buffer;

    /**
     * Used to wrap all protocol types, and automatically detect and switch to the protocol
     * that the client is attempting to use upon first connecting
     *
     * @param connector
     */
    public AnyProtocolCodec(Connector connector) {
        this.connector = connector;

        if (protocols.isEmpty()) {
            throw new IllegalArgumentException("No protocol configured for identification.");
        }

        // determine the largest 'magic' of all the protocols and set our buffer to that size
        buffer = ByteBuffer.allocate(maxIdentifcationLength());
    }

    public String getProtocol() {
        return "any";
    }

    private int maxIdentifcationLength() {
        int a = 0;
        for (Protocol b : protocols) {
            if (b.maxIdentificaionLength() > a) {
                a = b.maxIdentificaionLength();
            }
        }

        return a;
    }

    public ReadableByteChannel getChannel() {
        return transport.getReadChannel();
    }

    @Override
    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public Transport getTransport() {
        return transport;
    }

    @Override
    public Object read() throws IOException {
        if (next != null) {
            throw new IllegalStateException("Not expecting AnyProtocolCodec to have a next!");
        }
        getChannel().read(buffer);
        org.fusesource.hawtbuf.Buffer buff = new org.fusesource.hawtbuf.Buffer(buffer.array(), 0, buffer.position());
        for (Protocol p : protocols) {
            if (p.matchesIdentification(buff)) {
                next = p.createProtocolCodec(connector);
                AnyProtocol.changeProtocolCodec(transport, next);
                next.unread(buff.toByteArray());

                // this codec should only return ProtocolDetected objects... this is what
                // onTransportCommand(Object) will be called with for the AnyProtocolHandler
                return new ProtocolDetected(p.getId(), next);
            }
        }

        if (buffer.position() == buffer.capacity()) {
            throw new IOException("Could not identify the protocol.");
        }
        return null;
    }



    @Override
    public void unread(byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getReadCounter() {
        return buffer.position();
    }

    @Override
    public long getLastReadSize() {
        return 0;
    }

    @Override
    public int getReadBufferSize() {
        return buffer.capacity();
    }

    @Override
    public int getWriteBufferSize() {
        return 0;
    }

    @Override
    public BufferState write(Object o) throws IOException {
        return ProtocolCodec.BufferState.FULL;
    }

    @Override
    public BufferState flush() throws IOException {
        return ProtocolCodec.BufferState.FULL;
    }

    @Override
    public boolean full() {
        return true;
    }

    @Override
    public long getWriteCounter() {
        return 0L;
    }

    @Override
    public long getLastWriteSize() {
        return 0;
    }

    public ProtocolCodec getNext() {
        return next;
    }

    public void setNext(ProtocolCodec next) {
        this.next = next;
    }

    public void setProtocols(List<Protocol> protocols) {
        this.protocols = protocols;
    }

    public List<Protocol> getProtocols() {
        return protocols;
    }
}

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
package org.apache.activemq.apollo.broker.jetty;

import org.apache.activemq.apollo.util.BaseService;
import org.apache.activemq.apollo.util.SerialExecutor;
import org.eclipse.jetty.websocket.WebSocket;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtdispatch.*;
import org.fusesource.hawtdispatch.transport.ProtocolCodec;
import org.fusesource.hawtdispatch.transport.SecuredSession;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.hawtdispatch.transport.TransportListener;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.concurrent.Executor;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class WebSocketTransport extends BaseService implements WebSocket.OnTextMessage, WebSocket.OnBinaryMessage,
                                                Transport, SecuredSession, ScatteringByteChannel, GatheringByteChannel {

    private Connection connection;
    private final WsTransportServer server;
    private TransportListener transportListener;
    private X509Certificate[] certificates;
    private ProtocolCodec protocolCodec;

    private CustomDispatchSource<Integer, Integer> drainOutboundEvents;
    private DispatchQueue inboundDispatchQueue = dispatchQueue.createQueue("inbound queue");
    private LinkedList<Buffer> inbound = new LinkedList<Buffer>();

    private int inboundCapacityRemaining = 0;
    private int outboundCapacityRemaining = 1024 * 64;

    private boolean binaryTransfers = false;


    /**
     * Constructor
     * @param server
     * @param request
     * @param protocol
     */
    public WebSocketTransport(WsTransportServer server, HttpServletRequest request, String protocol) {
        super(Dispatch.createQueue("ws transport"));
        this.server = server;

        this.certificates = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

        drainOutboundEvents = Dispatch.createSource(EventAggregators.INTEGER_ADD, dispatchQueue);
        drainOutboundEvents.setEventHandler(new Task() {

            @Override
            public void run() {
                flush();
            }
        });
        drainOutboundEvents.resume();


        // TODO:ceposta NEXT STEPS... left off implement this transport...
        // pay attention to the different executors, event listeners, and counters...
    }

    private SerialExecutor outboundExecutor = new SerialExecutor(getBlockingExecutor()){
        private int outboundDrained = 0;

        @Override
        protected void drained() {
            final int val = outboundDrained;
            dispatchQueue.execute(new Task () {

                @Override
                public void run() {
                    outboundCapacityRemaining += val;
                    flush();
                    transportListener.onRefill();
                }
            });
        }
    };

    @Override
    protected void _start(Task onCompleted) {
        inboundDispatchQueue.setTargetQueue(dispatchQueue);
        drainOutboundEvents.setTargetQueue(dispatchQueue);
        transportListener.onTransportConnected();

        synchronized (inbound) {
            inboundCapacityRemaining = 1024 * 64;
            inbound.notify();
        }

        onCompleted.run();
    }

    @Override
    protected void _stop(final Task onCompleted) {
        inboundDispatchQueue.resume();
        outboundExecutor.execute(new Task() {

            @Override
            public void run() {
                synchronized (inbound) {
                    inbound.notify();
                }
                connection.close();
                dispatchQueue.execute(new Task() {

                    @Override
                    public void run() {
                        protocolCodec =  null;
                        onCompleted.run();
                        transportListener.onTransportDisconnected();
                    }
                });
            }
        });
    }

    @Override
    public void onMessage(byte[] bytes, int i, int i2) {
    }

    @Override
    public void onMessage(String s) {
    }

    @Override
    public void onOpen(Connection connection) {
    }

    @Override
    public void onClose(int i, String s) {
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return 0;
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException {
        return 0;
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return 0;
    }

    @Override
    public long read(ByteBuffer[] dsts) throws IOException {
        return 0;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return 0;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return 0;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public X509Certificate[] getPeerX509Certificates() {
        return this.certificates;
    }

    @Override
    public void start(Runnable runnable) {
        super.start(new TaskWrapper(runnable));
    }

    @Override
    public void stop(Runnable runnable) {
        super.stop(new TaskWrapper(runnable));
    }

    @Override
    public boolean full() {
        return false;
    }

    @Override
    public boolean offer(Object o) {
        return false;
    }

    @Override
    public void flush() {
        dispatchQueue.assertExecuting();
        if (!serviceState.isStarted()) {
            return;
        }
        try{
            getProtocolCodec().flush();
        } catch (IOException e) {
            getTransportListener().onTransportFailure(e);
        }
    }

    @Override
    public TransportListener getTransportListener() {
        return this.transportListener;
    }

    @Override
    public void setTransportListener(TransportListener transportListener) {
        this.transportListener = transportListener;
    }

    @Override
    public void setDispatchQueue(DispatchQueue dispatchQueue) {
        this.dispatchQueue = dispatchQueue;
    }

    @Override
    public void suspendRead() {
    }

    @Override
    public void resumeRead() {
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public void drainInbound() {
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public ProtocolCodec getProtocolCodec() {
        return this.protocolCodec;
    }

    @Override
    public void setProtocolCodec(ProtocolCodec codec) throws Exception {
        this.protocolCodec = codec;
        if (this.protocolCodec != null) {
            this.protocolCodec.setTransport(this);
        }
    }

    @Override
    public Executor getBlockingExecutor() {
        return server.getBlockingExecutor();
    }

    @Override
    public void setBlockingExecutor(Executor executor) {
        // purposefully left blank.. use server's blocking executor
    }

    @Override
    public ReadableByteChannel getReadChannel() {
        return this;
    }

    @Override
    public WritableByteChannel getWriteChannel() {
        return this;
    }

    public Connection getConnection() {
        return connection;
    }
}

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
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtdispatch.*;
import org.fusesource.hawtdispatch.transport.ProtocolCodec;
import org.fusesource.hawtdispatch.transport.SecuredSession;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.hawtdispatch.transport.TransportListener;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
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
 * we act as the transport, including reading the low level bits from websocket connection in jetty connector
 *
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
    private HttpServletRequest request;

    private int inboundCapacityRemaining = 0;
    private int outboundCapacityRemaining = 1024 * 64;

    private boolean writeFailed = false;
    private boolean binaryTransfers = false;
    private ClosedCode closed;
    private boolean firstMessage = true;


    /**
     * Constructor
     * @param server
     * @param request
     * @param protocol
     */
    public WebSocketTransport(WsTransportServer server, HttpServletRequest request, String protocol) {
        super(Dispatch.createQueue("ws transport"));
        this.server = server;
        this.request = request;
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

        @Override
        protected void drained() {
            final long val = outboundDrained;
            dispatchQueue.execute(new Task () {

                @Override
                public void run() {
                    // once we're drained, add this value back so we can open up that 64K window some more
                    outboundCapacityRemaining += val;
                    flush();
                    transportListener.onRefill();
                }
            });
        }
    };

    /** *********************************************************************************************************
     *
     *  Transport functionality
     *
     ** *********************************************************************************************************/
    @Override
    public Executor getBlockingExecutor() {
        return server.getBlockingExecutor();
    }

    @Override
    public void setBlockingExecutor(Executor executor) {
        // purposefully left blank.. use server's blocking executor
    }

    @Override
    public void setDispatchQueue(DispatchQueue dispatchQueue) {
        this.dispatchQueue = dispatchQueue;
        this.drainOutboundEvents.setTargetQueue(dispatchQueue);
        this.inboundDispatchQueue.setTargetQueue(dispatchQueue);
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
    public X509Certificate[] getPeerX509Certificates() {
        return this.certificates;
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
    public ReadableByteChannel getReadChannel() {
        return this;
    }

    @Override
    public WritableByteChannel getWriteChannel() {
        return this;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return new InetSocketAddress(request.getRemoteAddr(), request.getRemotePort());
    }

    @Override
    public SocketAddress getLocalAddress() {
        return new InetSocketAddress(request.getLocalAddr(), request.getLocalPort());
    }

    @Override
    public boolean isConnected() {
        // todo:ceposta:APLO-scala BUG?? write a test and fix this
        return connection == null || connection.isOpen();
    }

    @Override
    public boolean isClosed() {
        return connection == null;
    }

    @Override
    public void start(Runnable runnable) {
        super.start(new TaskWrapper(runnable));
    }

    @Override
    public void stop(Runnable runnable) {
        super.stop(new TaskWrapper(runnable));
    }

    /**
     * This will get called after jetty has informed us a new websocket connection has come in,
     * and then a new broker connection will be created, and this transport will be started when the
     * connection is started... so that means the transportListener.onTransportConnected() will end up
     * being called. By defaul the Any protocol handles it, which will then auto-detect STOMP protocol
     *
     * @param onCompleted
     */
    @Override
    protected void _start(Task onCompleted) {
        inboundDispatchQueue.setTargetQueue(dispatchQueue);
        drainOutboundEvents.setTargetQueue(dispatchQueue);
        transportListener.onTransportConnected();

        synchronized (inbound) {
            // give 64k of capacity (in bytes) once we've started... this would allow the
            // on message continue on
            inboundCapacityRemaining = 1024 * 64;
            inbound.notify();
        }

        onCompleted.run();
    }

    @Override
    protected void _stop(final Task onCompleted) {
        // todo:ceposta:APLO-scala why would we resume this?
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
                        protocolCodec = null;
                        onCompleted.run();
                        transportListener.onTransportDisconnected();
                    }
                });
            }
        });
    }

    /** *********************************************************************************************************
     *
     *  Websocket lifecycle
     *
     ** *********************************************************************************************************/
    private class ClosedCode {
        private int code;
        private String message;

        private ClosedCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        private int getCode() {
            return code;
        }

        private String getMessage() {
            return message;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void onOpen(Connection connection) {
        this.connection = connection;
        server.addPendingConnects(this);
        server.fireAccept();
    }

    @Override
    public void onClose(int code, String message) {
        this.closed = new ClosedCode(code, message);
        inboundDispatchQueue.execute(new Task() {
            @Override
            public void run() {
                doDrainInbound();
            }
        });
    }

    /** *********************************************************************************************************
     *
     *  Inbound messages
     *
     ** *********************************************************************************************************/
    @Override
    public void onMessage(String message) {
        if (this.firstMessage) {
            // if the first message the client sends us is a text message (as denoted by this callback method
            // being called), then we will respond with text messages
            binaryTransfers = false;
            firstMessage = false;
        }

        // convert the string message to bytes message.. our codecs just work with bytes
        Buffer buffer = new AsciiBuffer(message);

        // delegate to the callback the deals with bytes
        onMessage(buffer.getData(), buffer.getOffset(), buffer.getLength());

    }

    // guess this would be called on one of jetty's threads.. not ours
    @Override
    public void onMessage(final byte[] bytes, final int offset, final int length) {
        if (this.firstMessage) {
            // if the first message the client sends us is a binary message (as denoted by this callback method
            // being called), then we will respond with binary messages
            binaryTransfers = true;
            firstMessage = false;
        }

        synchronized (inbound) {

            // wait while there is no capacity, which could happen when we've "started", but haven't
            // been given space.. or when there is legitimately no space left on this transport..
            // default to 64K space
            while (inboundCapacityRemaining <= 0 && serviceState.isUpward()) {
                try {
                    inbound.wait();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // now we've been given some space, so let's keep track of how much we're using here
            inboundCapacityRemaining -= length;
        }


        // this part happens on our own hawtdispatch threads...
        inboundDispatchQueue.execute(new Task() {
            @Override
            public void run() {
                inbound.add(new Buffer(bytes, offset, length));
                doDrainInbound();
            }
        });

    }


    @Override
    public void drainInbound() {
        inboundDispatchQueue.execute(new Task() {
            @Override
            public void run() {
                doDrainInbound();
            }
        });
    }

    private void doDrainInbound() {
        dispatchQueue.assertExecuting();
        try {
            while (true) {

                // if we are not started, or the inboundqueue is suspended, exit out
                if (!serviceState.isStarted() || inboundDispatchQueue.isSuspended()) {
                    return;
                }

                Object command = protocolCodec.read();
                if (command != null) {
                    try {
                        transportListener.onTransportCommand(command);
                    } catch (Exception e) {
                        transportListener.onTransportFailure(new IOException("Transport listener failure for websocket transport: " + e));
                    }
                } else {
                    return;
                }
            }
        } catch (IOException e) {
            transportListener.onTransportFailure(e);
        }
    }

    @Override
    public void resumeRead() {
        inboundDispatchQueue.resume();
        inboundDispatchQueue.execute(new Task() {
            @Override
            public void run() {
                doDrainInbound();
            }
        });
    }

    @Override
    public void suspendRead() {
        inboundDispatchQueue.suspend();
    }

    @Override
    public boolean isOpen() {
        // todo:ceposta:APLO-scala can we look closer at this? why is closed!=null == open? if closed!=null,
        // then we've rceived the onClose method from the websocket impl...
        return inbound.isEmpty() && closed != null;
    }

    @Override
    public void close() throws IOException {
        // left blank on purpose
    }


    @Override
    public int read(ByteBuffer dst) throws IOException {
        inboundDispatchQueue.assertExecuting();

        // if we don't have anything and have been closed, then get out of here, transport cannot read
        if (inbound.isEmpty() && closed != null) {
            return -1;
        }

        int rc = 0;

        // grab one incoming websocket at a time, and try to fill the destination buffer
        while (!dst.hasRemaining() && !inbound.isEmpty()) {
            Buffer src = inbound.get(0);
            int len = Math.min(src.length, dst.remaining());
            rc += len;
            dst.put(src.data, src.offset, len);
            src.moveHead(len);
            if (src.length == 0) {
                inbound.removeFirst();
            }
        }

        final int totalCapacityToGiveBack = rc;
        getBlockingExecutor().execute(new Task() {
            @Override
            public void run() {
                synchronized (inbound) {
                    // give back the capacity since we've used it..
                    inboundCapacityRemaining += totalCapacityToGiveBack;
                }
            }
        });

        return rc;
    }

    @Override
    public long read(ByteBuffer[] dsts) throws IOException {
        // read into all of the buffers
        return read(dsts, 0, dsts.length);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        if (offset + length > dsts.length || length < 0 || offset < 0) {
            throw new IndexOutOfBoundsException("Could not read from the websocket transport: invalid length or offsets");
        }

        long rc = 0L;
        int i = 0;
        while (i < length) {
            ByteBuffer dst = dsts[offset + i];
            if (dst.hasRemaining()) {
                rc += read(dst);
            }

            // short circuit since we read our data into the buffer, but never filled it
            if (dst.hasRemaining()) {
                return rc;
            }

            i++;
        }

        return rc;
    }



    /** *********************************************************************************************************
     *
     *  Outbound messages
     *
     ** *********************************************************************************************************/

    @Override
    public boolean full() {
        return protocolCodec == null || protocolCodec.full();
    }

    // transport object is trying to offer back out to underlying transport mechanism (stream)
    @Override
    public boolean offer(Object command) {
        dispatchQueue.assertExecuting();
        try {
            if (!serviceState.isStarted()) {
                // this command gets dropped since it was issued after we were stopped!
                return false;
            }

            // this asks the protocol codec to write it.. but it woun't actually get sent to the
            // web socket connection until flush is called..
            ProtocolCodec.BufferState state = protocolCodec.write(command);
            if (state == ProtocolCodec.BufferState.FULL) {
                return false;
            } else {

                // interesting... sets an event to flush the protocol transport
                // this is what actually delivers it to the websocket connection by calling flush, and
                // flush on the protocol codec.
                drainOutboundEvents.merge(1);
                return true;
            }
        } catch (IOException e) {
            // drop the frame
            transportListener.onTransportFailure(e);
            return false;
        }

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
    public int write(ByteBuffer src) throws IOException {
        dispatchQueue.assertExecuting();

        // how many bytes left to write
        final int remaining = src.remaining();

        if (remaining == 0) {
            // nothing left to write?
            return 0;
        }

        // we are within our limits of 64K to write back out to web sockets
        if (outboundCapacityRemaining > 0) {
            outboundCapacityRemaining -= remaining;
            final Buffer buffer = new Buffer(src.array(), src.arrayOffset(), src.remaining());

            // write this on the outbound executor...
            outboundExecutor.execute(new Task() {
                @Override
                public void run() {
                    if (serviceState.isStartingOrStarted() || !writeFailed) {
                        try {
                            if (!binaryTransfers) {
                                connection.sendMessage(buffer.ascii().toString());
                            }else {
                                connection.sendMessage(buffer.getData(), buffer.getOffset(), buffer.getLength());
                            }
                            outboundExecutor.outboundDrained += remaining;
                        } catch (final IOException e) {
                            writeFailed = true;
                            dispatchQueue.execute(new Task() {
                                @Override
                                public void run() {
                                    transportListener.onTransportFailure(e);
                                }
                            });

                        }
                    }
                }
            });
            src.position(src.position() + remaining);
            return remaining;
        }else {
            return 0;
        }
    }



    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if (offset + length > srcs.length || offset < 0 || length < 0) {
            throw new IndexOutOfBoundsException("Could not write to the websocket transport: invalid length or offsets");
        }

        long rc = 0L;
        int i = 0;
        while (i < length) {
            ByteBuffer src = srcs[offset + i];
            if (src.hasRemaining()) {
                rc += write(src);
            }

            // could not even fill this one buffer, so reached the end.. can return
            if (src.hasRemaining()) {
                return rc;
            }

            i++;
        }

        // filled all buffers to the extent we could
        return rc;
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }



}

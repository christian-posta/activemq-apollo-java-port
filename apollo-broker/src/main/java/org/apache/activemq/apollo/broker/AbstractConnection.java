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

import org.apache.activemq.apollo.util.BaseService;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.hawtdispatch.transport.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public abstract class AbstractConnection extends BaseService{
    // todo:ceposta what about DeferringDispatched traint that becomes difficult to mixin?

    protected Logger LOG = LoggerFactory.getLogger(getClass().getName());

    protected boolean stopped = true;
    protected Transport transport;

    // todo:ceposta gotta do the Sink stuff
    protected TransportSink transportSink;

    protected AbstractConnection() {
        super(Dispatch.createQueue());
    }

    @Override
    protected void _start(Task onCompleted) {
        stopped = false;
        transportSink = new TransportSink(transport);
        transport.setDispatchQueue(dispatchQueue);
        transport.setTransportListener(new TransportListener() {
            @Override
            public void onTransportCommand(Object o) {
                AbstractConnection.this.onTransportCommand(o);
            }

            @Override
            public void onRefill() {
                AbstractConnection.this.onRefill();
            }

            @Override
            public void onTransportFailure(IOException e) {
                AbstractConnection.this.onTransportFailure(e);
            }

            @Override
            public void onTransportConnected() {
                AbstractConnection.this.onTransportConnected();
            }

            @Override
            public void onTransportDisconnected() {
                AbstractConnection.this.onTransportDisconnected();
            }
        });

        // start the transport!
        transport.start(onCompleted);

    }

    @Override
    protected void _stop(Task onCompleted) {
        stopped = true;
        transport.stop(onCompleted);
    }

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public void setDispatchQueue(final DispatchQueue dispQueue, final Task onComplete) {
        this.dispatchQueue.execute(new Task() {

            @Override
            public void run() {
                if (transport != null) {
                    transport.setDispatchQueue(dispQueue);
                }
                AbstractConnection.this.dispatchQueue = dispQueue;

                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    protected void onRefill() {
        // todo:ceposta implement as part of the TransportSink
    }

    // subclasses should override the methods they're interested in
    protected abstract void onTransportCommand(Object o);

    protected void onTransportFailure(IOException e) {
        if (!stopped) {
            onFailure(e);
        }
    }

    protected abstract void onTransportConnected();

    protected abstract void onTransportDisconnected();

    protected void onFailure(Exception e) {
        LOG.warn("There was a failure of the transport, shutting it down", e);
        transport.stop(Dispatch.NOOP);
    }
}

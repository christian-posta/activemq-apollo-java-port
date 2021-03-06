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
package org.apache.activemq.apollo.broker.sink;

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.transport.Transport;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class TransportSink<T> implements Sink<T> {

    private Transport transport;

    public TransportSink(Transport transport) {
        this.transport = transport;
    }

    @Override
    public boolean full() {
        return transport.full();
    }

    @Override
    public Task refiller() {
        return Dispatch.NOOP;
    }

    @Override
    public void setRefiller(Task value) {
        // this stays blank because we use a transport for this
    }

    @Override
    public boolean offer(T value) {
        return transport.offer(value);
    }

    @Override
    public String toString() {
        return "TransportSink(full:" + full() + ")";

    }
}

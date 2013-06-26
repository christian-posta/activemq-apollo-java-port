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

import org.apache.activemq.apollo.broker.protocol.DummyProtocol;
import org.apache.activemq.apollo.util.ServiceControl;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Metrics;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.internal.SerialDispatchQueue;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class AnyProtocolDetectionTest {

    @Test
    public void testDetectDummyProtocol() throws InterruptedException {
        Dispatch.profile(true);
        System.out.println("Starting test..");
        // start up a connector
        Broker broker = new Broker();
        final AcceptingConnector connector = new AcceptingConnector(broker, "test-connector");
        connector.start(new Task() {

            @Override
            public void run() {
                assertTrue(connector.getServiceState().isStarted());
                runTest(connector);
            }
        });


        // would like to one day get rid of this sleep..
        Thread.sleep(1000);
        assertConnection(connector);

        ServiceControl.stop(connector);
        List<Metrics> metrics = Dispatch.metrics();
        for (Metrics m : metrics) {
            System.out.println(m.toString());
        }
        System.out.println("Shutting down test..");
    }

    private void runTest(final AcceptingConnector connector) {

        System.out.println("Running test...");
        Socket socket = new Socket();
        int port = ((InetSocketAddress) connector.getSocketAddress()).getPort();
        try {
            socket.connect(new InetSocketAddress("127.0.0.01", port));
            socket.getOutputStream().write("CONNECT".getBytes("UTF-8"));
            socket.getOutputStream().flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    private void assertConnection(AcceptingConnector connector) {
        System.out.println("asserting connection");
        Map<Long, BrokerConnection> connections = connector.getBroker().getConnections();
        assertTrue(connections.size() > 0);
        BrokerConnection connection = connections.values().iterator().next();
        assertNotNull(connection);
        assertTrue(connection.getProtocolHandler() instanceof DummyProtocol.DummyProtocolHandler);
        assertTrue(connection.getTransport().getProtocolCodec() instanceof DummyProtocol.DummyProtocolCodec);
    }
}

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

import org.fusesource.hawtdispatch.Task;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class AnyProtocolDetectionTest {

    @Test
    public void testFoo() throws InterruptedException {
        System.out.println("Starting test..");
        // start up a connector
        Broker broker = new Broker();
        final AcceptingConnector connector = new AcceptingConnector(broker, "test-connector");

        connector.start(new Task() {

            @Override
            public void run() {
                runTest(connector, new Task() {

                    @Override
                    public void run() {
                        System.out.println("Test has ended... shutting down connector");
                        connector.stop(new Task() {
                            @Override
                            public void run() {
                                System.out.println("Connector has stopped.");
                            }
                        });
                    }
                });
            }
        });

        Thread.sleep(2000);
        assertConnection(connector);
        System.out.println("Shutting down test..");
    }

    private void runTest(final AcceptingConnector connector, Task task) {
        // perform test here...
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

        task.run();
    }

    private void assertConnection(AcceptingConnector connector) {
        Map<Long, BrokerConnection> connection = connector.getBroker().getConnections();
        assertTrue(connection.size() > 0);
    }
}

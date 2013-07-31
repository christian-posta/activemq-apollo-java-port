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
package org.apache.activemq.apollo.amqp.test;

import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.hawtdispatch.api.*;
import org.fusesource.hawtdispatch.Task;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class AmqpConnectionTest extends AmqpTestSupport{

    @Test
    public void testConnection() throws Exception {

        // todo:ceposta NEXT STEPS: for some reason the AMQP magic isn't properly being sent? or
        // parsed by the broker? unknown... but we cannot identify the protocol properly so hawtdispatch
        // spins in an infinite loop.. note.. this could also be a bug in scala apollo? what happens if a
        // buffer of all zeros (nulls) is sent? does the broker go toast?
        // shoudl figure out whether this transport is properly converting the magic and sending it...
        AmqpConnectOptions options = new AmqpConnectOptions();
        System.out.println("Using port: " + port);
        options.setHost("localhost", port);
        options.setUser("admin");
        options.setPassword("password");

        final AmqpConnection connection = AmqpConnection.connect(options);

        assertNotNull(connection);
        System.out.println("We have connected");
        final AtomicBoolean success = new AtomicBoolean(false);

        connection.queue().execute(new Task(){

            @Override
            public void run() {
                final AmqpSession session = connection.createSession();
                Target target = new Target();
                target.setAddress("queue://FOO");
                AmqpSender sender = session.createSender(target);
                MessageDelivery delivery = sender.send(session.createTextMessage("hello World!"));
                delivery.onSettle(new Callback<DeliveryState>() {

                    @Override
                    public void onSuccess(DeliveryState deliveryState) {
                        System.out.println("message sent completed");
                        System.out.println("========================================================");
                        System.out.println("========================================================");
                        Source source = new Source();
                        source.setAddress("queue://FOO");
                        AmqpReceiver receiver = session.createReceiver(source);
                        receiver.resume();
                        receiver.setDeliveryListener(new AmqpDeliveryListener() {
                            @Override
                            public void onMessageDelivery(MessageDelivery messageDelivery) {
                                System.out.println("Received: " + ((AmqpValue)messageDelivery.getMessage().getBody()).getValue());
                                messageDelivery.settle();
                                connection.close();
                                success.set(true);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        System.out.println("message sent failed");
                        throwable.printStackTrace();
                    }
                });
            }
        });

        connection.waitForDisconnected();
        assertTrue(success.get());
    }
}

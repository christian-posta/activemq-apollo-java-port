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

import org.apache.activemq.apollo.util.ClassFinder;

import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public final class ApolloBrokerFactory {


    private static ClassFinder<BrokerFactory> finder = new ClassFinder("META-INF/services/org.apache.activemq.apollo/broker-factory.index", BrokerFactory.class);


    public static Broker createBroker(String brokerUri) {
        Properties props = new Properties();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            props.put("env." + entry.getKey(), entry.getValue());
        }
        props.putAll(System.getProperties());
        return createBroker(brokerUri, props);
    }

    public static Broker createBroker(String brokerUri, Properties props) {
        if (brokerUri == null) {
            return null;
        }

        // find the first factory that can create a valid broker and return that
        for (BrokerFactory factory : finder.getSingletons()) {
            Broker broker = factory.createBroker(brokerUri, props);
            if (broker != null) {
                return broker;
            }
        }

        throw new IllegalArgumentException("Uknonwn broker uri: " + brokerUri);

    }

}

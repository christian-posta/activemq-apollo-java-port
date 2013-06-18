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

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.Properties;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class BrokerTestSupport {

    static Broker broker;
    protected int port = 0;
    private static String brokerConfigUri = "xml:classpath:apollo.xml";

    @BeforeClass
    public static void beforeAll() {
        Properties props = new Properties(System.getProperties());
        props.setProperty("testdatadir", "");
        ApolloBrokerFactory.createBroker(brokerConfigUri, props);
    }

    @AfterClass
    public static void afterAll() {
        // todo:ceposta NEXT STEP... continue putting together broker support and slowly implementing broker
    }

    public static int connectorPort(Broker broker, String connector) {
        return 61612;
    }

    protected int connectorPort(String connector) {
        return BrokerTestSupport.connectorPort(broker, connector);
    }
}

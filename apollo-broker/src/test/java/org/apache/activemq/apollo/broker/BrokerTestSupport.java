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

import org.apache.activemq.apollo.util.ApolloTestSupport;
import org.apache.activemq.apollo.util.ServiceControl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class BrokerTestSupport extends ApolloTestSupport {

    // todo:ceposta:priority=10 figure out how to speed up tests:
    // + parallelize
    // + only bring up broker one time when starting, and close when ending
    // todo:ceposta:priority=10 check to see if we can write our own @RunWith runner

    private Logger LOG = LoggerFactory.getLogger(getClass().getName());


    static Broker broker;
    protected int port = 0;
    private static String brokerConfigUri = "xml:classpath:apollo.xml";

    private static Broker createBroker(){
        Properties props = new Properties(System.getProperties());
        props.setProperty("testdatadir", "");
        return ApolloBrokerFactory.createBroker(brokerConfigUri, props);
    }

    @Before
    public void before() {
        initBroker();
    }

    private void initBroker() {
        LOG.info("Starting broker...");
        try {
            broker = createBroker();
            broker.setTmp(new File(getTestDataDir(), "tmp"));
            broker.getTmp().mkdirs();
            ServiceControl.start(broker);
        } catch (Exception e) {

        }
    }

    @After
    public void after() {
        ServiceControl.stop(broker);
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

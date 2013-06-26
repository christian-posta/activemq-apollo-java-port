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

import org.apache.activemq.apollo.util.FunctionUtils;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.Metrics;
import org.fusesource.hawtdispatch.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public enum  BrokerRegistry {

    INSTANCE;

    private Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private Set<Broker> brokers;
    private volatile int monitorSession = 0;

    private BrokerRegistry() {
        brokers = new HashSet<Broker>();
    }

    public synchronized Broker[] list() {
        Broker[] rc = new Broker[brokers.size()];
        brokers.toArray(rc);
        return rc;
    }

    public synchronized Set<Broker> add(Broker broker) {
        boolean added = brokers.add(broker);
        if (added && brokers.size() == 1 && Boolean.getBoolean("hawtdispatch.profile")) {
            monitorSession++;
            monitorHawtdispatch(monitorSession);
        }
        return brokers;
    }

    private void monitorHawtdispatch(final int sessionId) {
        // if we get a new broker, stop dispatching this current run.. a new one would be scheduled
        Dispatch.getGlobalQueue().executeAfter(1, TimeUnit.SECONDS, new Task() {

            @Override
            public void run() {
                if (sessionId == monitorSession) {
                    List<Metrics> toPrint = new LinkedList<Metrics>();
                    List<Metrics> metrics = Dispatch.metrics();
                    for (Metrics m : metrics) {
                        if (m.totalWaitTimeNS > TimeUnit.MILLISECONDS.toNanos(10) || m.totalRunTimeNS > TimeUnit.MILLISECONDS.toNanos(10)) {
                            toPrint.add(m);
                        }
                    }

                    if (!toPrint.isEmpty()) {
                        LOG.info(FunctionUtils.mkString(toPrint, "\n"));
                    }
                }
            }
        });
    }
}

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
package org.apache.activemq.apollo.stomp.test;

import org.apache.activemq.apollo.broker.BrokerTestSupport;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.apache.activemq.apollo.stomp.test.RegexMatcher.regex;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class StompParallelTest extends BrokerTestSupport {

    private List<StompClient> clients = new LinkedList<StompClient>();

    @Test
    public void testConnect() {
        connect("1.0");
    }

    @Test
    public void testConnect11() {
        connect("1.1");
    }

    public StompClient connect(String version) {
        StompClient c = new StompClient();
        connect(version, c, null, null);
        return c;
    }

    public StompClient connect(String version, StompClient c, String headers, String connector) {
        c.setVersion(version);

        if (headers == null) {
            headers = "";
        }

        String frame = connectRequest(version, c, headers, connector);
        assertThat(frame, startsWith("CONNECTED\n"));
        assertThat(frame, regex("session:.+?\\n"));
        assertThat(frame, containsString("version:" + version + "\n"));

        return c;
    }

    private String connectRequest(String version, StompClient c, String headers, String connector) {
        int port = connectorPort(connector);
        port = port == 0 ? this.port : port;

        c.open("localhost", port);
        writeConnectFrame(c, version, headers);
        clients.add(c);
        return c.receive();
    }

    private void writeConnectFrame(StompClient c, String version, String headers) {
        if ("1.0".equals(version)) {
            c.write(
                    "CONNECT\n" +
                    headers +
                    "\n");
        } else if ("1.1".equals(version) || "1.2".equals(version)) {
            c.write(
                    "CONNECT\n" +
                            "accept-version:"+version+"\n" +
                            "host:localhost\n" +
                            headers +
                            "\n");
        } else {
            throw new RuntimeException("Invalid STOMP version:  " + version);
        }
    }


}

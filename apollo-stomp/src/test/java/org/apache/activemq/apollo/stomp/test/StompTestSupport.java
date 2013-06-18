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
import org.junit.After;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.activemq.apollo.stomp.test.RegexMatcher.regex;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class StompTestSupport extends BrokerTestSupport {

    private List<StompClient> clients = new LinkedList<StompClient>();
    private StompClient client = new StompClient();
    private AtomicLong receiptCounter = new AtomicLong(0);

    @After
    public void cleanup() {
        for (StompClient c : clients) {
            c.close();
        }
        clients = null;
    }


    /**
     * STOMP connect
     *
     *
     */
    public StompClient connect(String version) {
        connect(version, client, null, null);
        return client;
    }

    public StompClient connect(String version, StompClient client, String headers, String connector) {
        StompClient c = client == null ? this.client : client;

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

    protected String connectRequest(String version, StompClient c, String headers, String connector) {
        int port = connectorPort(connector);
        port = port == 0 ? this.port : port;

        c.open("localhost", port);
        writeConnectFrame(c, version, headers);
        if (!clients.contains(c)) {
            clients.add(c);
        }
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

    public void close(StompClient c) {
        c.close();
        clients.remove(c);
    }


    /**
     * STOMP Disconnect
     *
     *
     */
    public void disconnect() {
        disconnect(this.client);
    }
    public void disconnect(StompClient c) {
        long rid = receiptCounter.incrementAndGet();
        c.write(
                "DISCONNECT\n" +
                        "receipt:" + rid + "\n" +
                        "\n");
        waitForReceipt("" + rid, c);
        close(c);
    }


    /**
     * STOMP Sync send
     *
     *
     */
    public void syncSend(String dest, String body) {
        syncSend(dest, body, "");
    }
    public void syncSend(String dest, String body, String headers){
        syncSend(dest, body, headers, this.client);
    }
    public void syncSend(String dest, String body, String headers, StompClient c) {

        long rid = receiptCounter.incrementAndGet();

        c.write(
                "SEND\n" +
                        "destination:" + dest + "\n" +
                        "receipt:" + rid + "\n" +
                        headers +
                        "\n" +
                        body);
        waitForReceipt("" + rid, c);
    }

    /**
     * STOMP Async send
     *
     *
     */
    public void asyncSend(String dest, String body) {
        asyncSend(dest, body, "");
    }
    public void asyncSend(String dest, String body, String headers) {
        asyncSend(dest, body, headers, this.client);
    }
    public void asyncSend(String dest, String body, String headers, StompClient c) {
        c.write(
                "SEND\n" +
                        "destination:" + dest + "\n" +
                        headers +
                        "\n" +
                        body);
    }

    /**
     *  STOMP Begin
     *  Begin a new transaction
     *
     */
    public void begin(String txid) {
        begin(txid, this.client);
    }
    public String begin(String transactionId, StompClient client) {
        StompClient c = client == null ? this.client : client;
        String txid = transactionId == null ? "x" : transactionId;
        c.write(
                "BEGIN\n" +
                        "transaction:"+txid+"\n" +
                        "\n");
        return txid;
    }

    /**
     *  STOMP Abort
     *
     *
     */
    public void abort(String txid, boolean sync) {
        abort(txid, sync, this.client);
    }
    public void abort(String txid, boolean sync, StompClient c) {

        long rid = receiptCounter.incrementAndGet();

        c.write(
                "ABORT\n" +
                        "transaction:"+txid+"\n" +
                        (sync ? "receipt:" + rid + "\n" : "") +
                "\n");
        if (sync) {
            waitForReceipt("" + rid, c);
        }
    }

    /**
     * STOMP Commit
     *
     *
     */
    public void commit(String txid, boolean sync) {
        commit(txid, sync);
    }
    public void commit(String txid, boolean sync, StompClient c) {

        long rid = receiptCounter.incrementAndGet();

        c.write(
                "COMMIT\n" +
                        "transaction:"+txid+"\n" +
                        (sync ? "receipt:" + rid + "\n" : "") +
                        "\n");
        if (sync) {
            waitForReceipt("" + rid, c);
        }
    }

    /**
     * STOMP Subscribe
     *
     *
     */
    public void subscribe(String id, String dest) {
        subscribe(id, dest, "auto");
    }

    public void subscribe(String id, String dest, String mode) {
        subscribe(id, dest, mode, false);
    }

    public void subscribe(String id, String dest, String mode, boolean persistent) {
        subscribe(id, dest, mode, persistent, "", true, this.client);
    }
    public void subscribe(String id, String dest, String mode, boolean persistent, String headers, boolean sync, StompClient c) {

        long rid = receiptCounter.incrementAndGet();

        c.write(
                "SUBSCRIBE\n" +
                        "destination:" + dest + "\n" +
                        "id:" + id + "\n" +
                        (persistent ? "persistent:true\n" : "") +
                "ack:" + mode + "\n" +
                (sync ? "receipt:" + rid + "\n" : "") +
                headers +
                "\n");
        if (sync) {
            waitForReceipt("" + rid, c);
        }
    }

    /**
     * STOMP Unsubscribe
     *
     *
     */
    public void unsubscribe(String id) {
        unsubscribe(id, "");
    }
    public void unsubscribe(String id, String headers) {
        unsubscribe(id, headers, this.client);
    }
    public void unsubscribe(String id, String headers, StompClient c) {
        long rid = receiptCounter.incrementAndGet();
        c.write(
                "UNSUBSCRIBE\n" +
                        "id:" + id + "\n" +
                        "receipt:" + rid + "\n" +
                        headers +
                        "\n");

        waitForReceipt("" + rid, c);
    }

    /**
     * STOMP assert received
     *
     *
     */
    public AckCallback assertReceived(String body, String sub) {
        return assertReceived(body, sub, this.client, "");
    }
    public AckCallback assertReceived(Object body, String sub){
        return assertReceived(body, sub, this.client, "");
    }
    public AckCallback assertReceived(Object bodyPattern, String sub, StompClient c, String txid) {
        AckCallback callback = receiveMessage(sub, c, txid);
        String frame = callback.getFrame();

        if (bodyPattern instanceof String) {
            assertThat(frame, endsWith("\n\n" + bodyPattern));
        }else {
            // we can add more pattern matching here as needed
            fail("We can only handle string bodies for assertion ATM");
        }

        return callback;
    }

    public AckCallback receiveMessage(String sub, StompClient c, String txid) {
        String frame = c.receive();
        assertThat(frame, startsWith("MESSAGE\n"));
        if (sub != null) {
            assertThat(frame, containsString("subscription:" + sub + "\n"));
        }

        return new AckCallback(c, frame, txid);
    }

    /**
     * Wait for receipt
     *
     *
     */
    protected void waitForReceipt(String rid) {
        waitForReceipt(rid, this.client);
    }
    protected void waitForReceipt(String rid, StompClient c) {
        waitForReceipt(rid, c, false, 10000);
    }
    protected String waitForReceipt(String id, StompClient c, boolean discardOthers, int timeout) {

        // we care about every frame we get back, so we don't skip over "others"
        if (!discardOthers) {
            String frame = c.receive(timeout);
            assertThat(frame, startsWith("RECEIPT\n"));
            if (id != null) {
                assertThat(frame, containsString("receipt-id:" + id + "\n"));
                return id;
            } else {
                int pos = frame.indexOf("receipt-id:");
                if (pos > 0) {
                    pos += "receipt-id:".length();
                    int pos2 = frame.indexOf("\n", pos);
                    if (pos2 > 0) {
                        return frame.substring(pos, pos2);
                    }
                }
            }
        }else {
            while(true) {
                String frame = c.receive();
                if (frame.startsWith("RECEIPT\n") && frame.indexOf("receipt-id:" + id + "\n") >= 0) {
                    return id;
                }
            }
        }

        return null;
    }

    public static class AckCallback {

        private static final Pattern SUB_REGEX = Pattern.compile("(?s).*\\nsubscription:([^\\n]+)\\n.*");
        private static final Pattern MSGID_REGEX = Pattern.compile("(?s).*\\nmessage-id:([^\\n]+)\\n.*");
        private static final Pattern ACK_REGEX = Pattern.compile("(?s).*\\nack:([^\\n]+)\\n.*");

        private String frame;
        private StompClient c;
        private String txid;

        public AckCallback(StompClient c, String frame, String txid) {
            this.frame = frame;
            this.c = c;
            this.txid = txid;
        }

        public void ack() {
            doAck(true);
        }

        public void nack() {
            doAck(false);
        }

        private void doAck(boolean ack) {
            if ("1.0".equals(c.getVersion()) || "1.1".equals(c.getVersion())) {
                c.write((ack ? "ACK\n" : "NACK\n") +
                        "subscription:" + getSub(frame) + "\n" +
                        "message-id:" + getMsgid(frame) + "\n" +
                        (txid != null ? "transaction:" + txid + "\n" : "") +
                        "\n");
            }
            else {
                // we assume else is version STOMP v1.2
                c.write(
                        (ack ?  "ACK\n" : "NACK\n") +
                        "id:" + getId(frame) + "\n" +
                        (txid != null ? "transaction:" + txid + "\n" : "") +
                        "\n");
            }
        }

        private String getSub(String frame) {
            return checkMatcher(SUB_REGEX.matcher(frame));
        }

        private String getMsgid(String frame) {
            return checkMatcher(MSGID_REGEX.matcher(frame));

        }

        private String getId(String frame) {
            return checkMatcher(ACK_REGEX.matcher(frame));
        }

        private String checkMatcher(Matcher matcher) {
            if (matcher.matches()) {
                return matcher.group(1);
            }
            else {
                return "";
            }
        }

        public String getFrame() {
            return frame;
        }
    }
}

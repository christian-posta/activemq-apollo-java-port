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

import org.apache.activemq.apollo.broker.KeyStorage;
import org.apache.activemq.apollo.broker.ProtocolException;
import org.apache.activemq.apollo.stomp.Stomp;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.ByteArrayOutputStream;

import javax.net.ssl.SSLContext;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.EOFException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class StompClient {

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private int bufferSize = 64 * 1024;
    private KeyStorage keyStorage;
    private long bytesWritten;
    private String version;

    public void open(String host, int port) {
        // reset number of bytes written..
        bytesWritten = 0;

        try {
            initSocket();
            socket.connect(new InetSocketAddress(host, port));
            socket.setSoLinger(true, 1);
            socket.setSoTimeout(30 * 1000);
            out = new BufferedOutputStream(socket.getOutputStream(), bufferSize);
            in = new BufferedInputStream(socket.getInputStream(), bufferSize);
        } catch (IOException e) {
            throw new IllegalStateException("Could not open socket connection for STOMP client.", e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not instantiate socket", e);
        }

    }

    private void initSocket() throws NoSuchAlgorithmException, KeyManagementException, IOException {
        if (keyStorage != null) {
            SSLContext context = SSLContext.getInstance("TLS");
            // todo:ceposta:stomp implement keystore with java
//            context.init(keyStorage.create_key_managers(), keyStorage.create_trust_managers(), null);
            socket = context.getSocketFactory().createSocket();
        }else {
            socket = new Socket();
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            //todo:ceposta:stomp log the error that we couldn't close
        }
    }

    public void write(String frame) {
        try {
            write(frame.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Could not write", e);
        }
    }

    public void write(byte[] frame) {
        try {
            out.write(frame);
            bytesWritten += frame.length;
            out.write(0);
            bytesWritten++;
            out.write('\n');

            bytesWritten++;
            out.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write", e);
        }
    }

    public void skip() {
        try {

            int c = in.read();
            while (c >= 0) {
                if (c == 0) {
                    return;
                }
                c = in.read();
            }
        }catch (IOException e) {
            throw new IllegalStateException("Could not read input stream for skipping");
        }

        throw new IllegalStateException("Socket closed, couldn't complete skipping", new EOFException());
    }

    public String receive(int timeout) {
        int original = 0;
        try {
            original = socket.getSoTimeout();
            socket.setSoTimeout(timeout);
            return receive();
        }catch (SocketException e) {
            throw new IllegalStateException("Could not alter socket timeout settings");
        } finally {
            try {
                socket.setSoTimeout(original);
            } catch (SocketException e) {
                // let's log this at least?
            }
        }

    }

    public String receive() {
        boolean start = true;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {

            int c = in.read();
            while (c >= 0) {
                if (c == 0) {
                    return new String(buffer.toByteArray());
                }
                // skip newlines at the beginning as they aren't significant
                if (!start || c != Stomp.NEWLINE) {
                    start = false;
                    buffer.write(c);
                }
                c = in.read();
            }
        }catch (IOException e) {
            throw new IllegalStateException("Error receiving!", e);
        }

        throw new IllegalStateException("Socket closed, couldn't complete receiving", new EOFException());

    }

    public void waitForReceipt(String id) {

    }

    public AsciiBuffer receiveAscii() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {

            int c = in.read();
            while (c >= 0) {
                if (c == 0) {
                    return buffer.toBuffer().ascii();
                }
                buffer.write(c);
                c = in.read();
            }
        }catch (IOException e) {
            throw new IllegalStateException("Error receiving!", e);
        }

        throw new IllegalStateException("Socket closed, couldn't complete receiving", new EOFException());
    }

    public String receive(String expected){
        String rc = receive();
        if (!rc.startsWith(expected)) {
            throw new ProtocolException("Expected: " + expected);
        }
        return rc;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}

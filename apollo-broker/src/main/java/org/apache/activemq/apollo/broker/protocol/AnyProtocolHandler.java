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
package org.apache.activemq.apollo.broker.protocol;

import org.apache.activemq.apollo.broker.ProtocolException;
import org.apache.activemq.apollo.dto.AcceptingConnectorDTO;
import org.apache.activemq.apollo.dto.DetectDTO;
import org.apache.activemq.apollo.dto.ProtocolDTO;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.Task;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class AnyProtocolHandler extends AbstractProtocolHandler implements ProtocolHandler {

    // discriminated == true means client connected with a known protocol
    private boolean discriminated;

    protected DetectDTO config;

    @Override
    public String getProtocol() {
        return "any";
    }

    @Override
    public String getSessionId() {
        // we won't have any session ids for AnyProtocol
        return null;
    }

    // make sure client connects eventually
    public boolean assertDiscriminated() {
        if(connection.getServiceState().isStarted() && !discriminated) {
            connection.stop(Dispatch.NOOP);
            return false;
        }

        return true;
    }

    @Override
    public void onTransportCommand(Object command) {
        if (!(command instanceof ProtocolDetected)) {
            throw new ProtocolException("Expected a ProtocolDetected object");
        }

        // we have a detection!
        discriminated = true;

        ProtocolDetected expectedProtocol = (ProtocolDetected) command;
        Protocol realProtocol = ProtocolFactoryFinder.get(expectedProtocol.getId());
        if (realProtocol == null) {
            throw new ProtocolException("No protocol handler available for protocol: " + expectedProtocol.getId());
        }
        ProtocolHandler handler = realProtocol.createProtocolHandler();

        // replace the current handler with this one
        // by setting this, we now delegate all transport listener calls to the new protocol handler
        connection.setProtocolHandler(handler);

        // stop the transport
        connection.getTransport().suspendRead();

        // set the connection
        handler.setConnection(connection);

        // tell the new handler we've connected, which should take the responsibility of resuming
        // the transport
        connection.getTransport().getTransportListener().onTransportConnected();
    }

    @Override
    public void onTransportConnected() {
        // when a transport is connected, we are expected to "resume" reading it... this will
        // usually be the "first" time the transport's read event source will begin reading
        // unless some other parts of the chain have already started...
        getConnection().getTransport().resumeRead();

        AnyProtocolCodec codec = connection.getProtocolCodec(AnyProtocolCodec.class);
        AcceptingConnectorDTO connectorConfig = (AcceptingConnectorDTO) connection.getConnector().getConfig();
        for (ProtocolDTO d : connectorConfig.protocols) {
            if (d instanceof DetectDTO) {
                config = (DetectDTO) d;
                break;
            }
        }
        if (config == null) {
            config = new DetectDTO();
        }

        // if there are configured protocols, limit what the any-codec can see
        if (config.protocols != null) {
            String[] configuredProtocols = config.protocols.split("\\s+");
            LinkedHashSet<String> filteredProtocols = filterProtocols(configuredProtocols);
            codec.setProtocols(allowableProtocols(filteredProtocols, codec.getProtocols()));
        }

        Long timeout = config.timeout;
        if (timeout == null) {
            timeout = 5000L;
        }

        connection.getDispatchQueue().executeAfter(timeout, TimeUnit.MILLISECONDS, new Task() {

            @Override
            public void run() {
                assertDiscriminated();
            }
        });
    }

    private List<Protocol> allowableProtocols(LinkedHashSet<String> filteredProtocols, List<Protocol> existing) {
        LinkedList<Protocol> rc = new LinkedList<Protocol>();
        for (Protocol p : existing) {
            if (filteredProtocols.contains(p.getId())) {
                rc.add(p);

            }
        }

        return rc;
    }

    private LinkedHashSet<String> filterProtocols(String[] allProtocols) {
        LinkedHashSet<String> rc = new LinkedHashSet<String>();
        for (String s : allProtocols) {
            if (s.length() != 0) {
                rc.add(s);
            }
        }

        return rc;
    }
}

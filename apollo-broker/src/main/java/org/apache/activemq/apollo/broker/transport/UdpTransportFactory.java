/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.apollo.broker.transport;

import org.apache.activemq.apollo.util.IntrospectionSupport;
import org.apache.activemq.apollo.util.URISupport;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.hawtdispatch.transport.TransportServer;
import org.fusesource.hawtdispatch.transport.UdpTransport;
import org.fusesource.hawtdispatch.transport.UdpTransportServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class UdpTransportFactory implements TransportFactoryFinder.Provider {
    private static final Logger LOG = LoggerFactory.getLogger(UdpTransportFactory.class);

    public TransportServer bind(String location) throws Exception {

        URI uri = new URI(location);
        Map<String, String> options = new HashMap<String, String>(URISupport.parseParamters(uri));

        UdpTransportServer server = createUdpTransportServer(uri, options);
        if (server == null) return null;

        Map<String, String> copy = new HashMap<String, String>(options);
        IntrospectionSupport.setProperties(server, new HashMap(options));
        return server;
    }

    public Transport connect(String location) throws Exception {
        throw new UnsupportedOperationException() ;
    }

    protected UdpTransportServer createUdpTransportServer(final URI location, final Map<String, String> options) throws IOException, URISyntaxException, Exception {
        if( !location.getScheme().equals("udp") ) {
            return null;
        }

        return new UdpTransportServer(location) {
            @Override
            protected UdpTransport createTransport() {
                UdpTransport transport = super.createTransport();
                IntrospectionSupport.setProperties(transport, new HashMap(options));
                return transport;
            }
        };
    }
}

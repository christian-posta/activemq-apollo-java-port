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
package org.apache.activemq.apollo.broker.jetty;

import org.apache.activemq.apollo.broker.transport.TransportFactoryFinder;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.hawtdispatch.transport.TransportServer;

import java.net.URI;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class WebSocketTransportFactory implements TransportFactoryFinder.Provider {

    @Override
    public TransportServer bind(String location) throws Exception {
        URI uri = new URI(location);
        String scheme = uri.getScheme();
        if ("ws".equals(scheme) || "wss".equals(scheme)) {
            return new WsTransportServer(new URI(location));
        }else{
            return null;
        }

    }

    @Override
    public Transport connect(String location) throws Exception {
        return null;
    }
}

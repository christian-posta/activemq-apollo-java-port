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

import org.apache.activemq.apollo.util.URISupport;
import org.apache.activemq.apollo.dto.BrokerDTO;
import org.apache.activemq.apollo.dto.XmlCodec;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class XmlBrokerFactory implements BrokerFactory {
    @Override
    public Broker createBroker(String value, Properties props) {
        try {
            URI brokerURI = new URI(value);
            brokerURI = URISupport.stripScheme(brokerURI);
            URL configURL = resolveConfigUrl(brokerURI);
            BrokerDTO config = XmlCodec.decode(BrokerDTO.class, configURL, props);
            Broker broker = new Broker();
            broker.setConfig(config);
            return broker;
        } catch (Exception e) {
            throw new RuntimeException("Cannot create broker with URI: " + value, e);
        }

    }

    URL resolveConfigUrl(URI brokerURI) throws IOException, URISyntaxException {
        URL configURL = null;
        String scheme = brokerURI.getScheme();
        if (scheme == null || "file".equals(scheme)) {
            configURL = URISupport.changeScheme(URISupport.stripScheme(brokerURI), "file").toURL();
        } else if ("classpath".equals(scheme)) {
            configURL = Thread.currentThread().getContextClassLoader().getResource(brokerURI.getSchemeSpecificPart());
        }

        if (configURL == null) {
            throw new IOException("Cannot create broker from non-existent URI: " + brokerURI);
        }
        return configURL;
    }
}

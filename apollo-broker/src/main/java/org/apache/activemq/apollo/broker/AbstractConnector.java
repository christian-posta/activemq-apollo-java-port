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

import org.apache.activemq.apollo.dto.AcceptingConnectorDTO;
import org.apache.activemq.apollo.dto.ProtocolDTO;
import org.apache.activemq.apollo.util.BaseService;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public abstract class AbstractConnector extends BaseService implements Connector {
    private Logger LOG = LoggerFactory.getLogger(getClass().getName());

    protected AbstractConnector(DispatchQueue dispatchQueue) {
        super(dispatchQueue);
    }

    @Override
    public <T extends ProtocolDTO> T protocolCodecConfig(Class<T> clazz) {
        AcceptingConnectorDTO connectorConfig = (AcceptingConnectorDTO) getConfig();
        for (ProtocolDTO x : connectorConfig.protocols) {
            if (clazz.isInstance(x)) {
                return clazz.cast(x);
            }
        }

        LOG.info("Could not find a valid protocol for this connector");
        return null;
    }
}

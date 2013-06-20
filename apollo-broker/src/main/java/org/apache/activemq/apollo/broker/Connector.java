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

import org.apache.activemq.apollo.broker.security.ResourceKind;
import org.apache.activemq.apollo.broker.security.SecuredResource;
import org.apache.activemq.apollo.dto.ConnectorTypeDTO;
import org.apache.activemq.apollo.dto.ProtocolDTO;
import org.apache.activemq.apollo.dto.ServiceStatusDTO;
import org.fusesource.hawtdispatch.Task;

import java.net.SocketAddress;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public interface Connector extends SecuredResource {

    public Broker getBroker();
    public String getId();

    public void stopped(BrokerConnection connection);
    public ConnectorTypeDTO getConfig();


    public Long getAccepted();
    public Long getConnected();

    public void update(ConnectorTypeDTO config, Task onComplete);
    public SocketAddress getSocketAddress();

    public ServiceStatusDTO getStatus();

    public ResourceKind getResourceKind();

    public void updateBufferSettings();

    public <T extends ProtocolDTO> T protocolCodecConfig(Class<T> clazz);

}

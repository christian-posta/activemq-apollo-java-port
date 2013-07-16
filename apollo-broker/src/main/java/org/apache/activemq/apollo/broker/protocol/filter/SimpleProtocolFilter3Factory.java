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
package org.apache.activemq.apollo.broker.protocol.filter;

import org.apache.activemq.apollo.broker.Broker;
import org.apache.activemq.apollo.broker.protocol.ProtocolHandler;
import org.apache.activemq.apollo.broker.protocol.ProtocolHandlerAware;
import org.apache.activemq.apollo.dto.ProtocolFilterDTO;
import org.apache.activemq.apollo.dto.SimpleProtocolFilterDTO;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class SimpleProtocolFilter3Factory implements ProtocolFilter3.Provider {

    @Override
    public ProtocolFilter3 create(ProtocolFilterDTO dto, ProtocolHandler handler) {

        if (dto instanceof SimpleProtocolFilterDTO) {
            try {
                Object instance = Broker.classLoader().loadClass(((SimpleProtocolFilterDTO) dto).kind).newInstance();
                if (!(instance instanceof ProtocolFilter3)) {
                    throw new IllegalArgumentException("Invalid protocol filter type: "+instance.getClass());
                }
                ProtocolFilter3 filter = (ProtocolFilter3) instance;

                if (filter instanceof ProtocolFilterDtoAware) {
                    ((ProtocolFilterDtoAware) filter).setDto(dto);
                }

                if (filter instanceof ProtocolHandlerAware) {
                    ((ProtocolHandlerAware) filter).setProtocolHandler(handler);
                }

                return filter;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // wasn't a simple protocol DTO, can't do much with it
        return null;
    }


}

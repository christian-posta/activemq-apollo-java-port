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

import org.apache.activemq.apollo.broker.protocol.ProtocolHandler;
import org.apache.activemq.apollo.dto.ProtocolFilterDTO;
import org.apache.activemq.apollo.util.ClassFinder;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class ProtocolFilter3FactoryFinder {

    public static final ClassFinder<ProtocolFilter3.Provider> providers =
            new ClassFinder<ProtocolFilter3.Provider>("META-INF/services/org.apache.activemq.apollo/protocol-filter-factory.index",
                    ProtocolFilter3.Provider.class);

    public ProtocolFilter3 create(ProtocolFilterDTO dto, ProtocolHandler handler) {
        for (ProtocolFilter3.Provider p : providers.getSingletons()) {
            ProtocolFilter3 rc = p.create(dto, handler);
            if (rc != null) {
                return rc;
            }
        }

        throw new IllegalArgumentException("Cannot create a protocol filter for DTO: "+dto);
    }

    // todo:ceposta NEXT STEP still in the middle of protocol filter: SimpleProtocolFilter3Factory
}

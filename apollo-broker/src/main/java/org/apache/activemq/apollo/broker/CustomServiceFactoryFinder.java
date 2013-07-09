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

import org.apache.activemq.apollo.dto.CustomServiceDTO;
import org.apache.activemq.apollo.util.ClassFinder;
import org.apache.activemq.apollo.util.Service;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class CustomServiceFactoryFinder  {

    private static ClassFinder<CustomServiceFactory> finder =
            new ClassFinder<CustomServiceFactory>("META-INF/services/org.apache.activemq.apollo/custom-service-factory.index", CustomServiceFactory.class);

    public static Service create(Broker broker, CustomServiceDTO dto) {
        if (dto == null) {
            return null;
        }

        for (CustomServiceFactory f : finder.getSingletons()) {
            Service customService = f.create(broker, dto);
            if (customService != null) {
                return  customService;
            }
        }

        return null;
    }


}

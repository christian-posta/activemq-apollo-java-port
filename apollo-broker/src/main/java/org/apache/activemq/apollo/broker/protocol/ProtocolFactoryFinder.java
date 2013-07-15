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

import org.apache.activemq.apollo.util.ClassFinder;

import java.util.*;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public final class ProtocolFactoryFinder {

    private static ClassFinder<Protocol> finder = new ClassFinder<Protocol>("META-INF/services/org.apache.activemq.apollo/protocol-factory.index", Protocol.class);

    public static Protocol[] protocols() {
        Protocol[] rc = new Protocol[finder.getSingletons().size()];
        finder.getSingletons().toArray(rc);
        return rc;
    }

    public static Map<String, Protocol> protocolsById() {
        LinkedHashMap<String, Protocol> rc = new LinkedHashMap<String, Protocol>();
        List<Protocol> protocols = finder.getSingletons();
        for (Protocol p : protocols) {
            rc.put(p.getId(), p);
        }
        return rc;
    }

    public static Protocol get(String name) {
        return protocolsById().get(name);
    }

    public static List<Protocol> identifiableProtocols() {
        LinkedList<Protocol> rc = new LinkedList<Protocol>();
        List<Protocol> protocols = finder.getSingletons();
        for (Protocol p : protocols) {
            if (p.isIdentifiable()) {
                rc.add(p);
            }
        }
        return rc;
    }

}

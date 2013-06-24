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

import org.apache.activemq.apollo.broker.Connector;
import org.fusesource.hawtdispatch.transport.ProtocolCodec;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.hawtdispatch.transport.WrappingProtocolCodec;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class AnyProtocol extends BaseProtocol{

    @Override
    public ProtocolHandler createProtocolHandler() {
        return new AnyProtocolHandler();
    }

    @Override
    public ProtocolCodec createProtocolCodec(Connector connector) {
        return new AnyProtocolCodec(connector);
    }

    @Override
    public String getId() {
        return "any";
    }

    public static void changeProtocolCodec(Transport transport, ProtocolCodec codec) {
        ProtocolCodec current = transport.getProtocolCodec();
        WrappingProtocolCodec wrapper = null;
        while (current != null) {
            if (current instanceof WrappingProtocolCodec) {
                wrapper = (WrappingProtocolCodec) current;
                current = ((WrappingProtocolCodec) current).getNext();
            } else {
                current = null;
            }

        }

        if (wrapper != null) {
            wrapper.setNext(codec);
        } else {
            try {
                transport.setProtocolCodec(codec);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

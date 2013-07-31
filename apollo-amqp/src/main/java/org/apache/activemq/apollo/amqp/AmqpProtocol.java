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
package org.apache.activemq.apollo.amqp;

import org.apache.activemq.apollo.broker.Connector;
import org.apache.activemq.apollo.broker.protocol.Protocol;
import org.apache.activemq.apollo.broker.protocol.ProtocolHandler;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtdispatch.transport.ProtocolCodec;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class AmqpProtocol implements Protocol {

    private static final String id = "amqp";
    private static final AsciiBuffer PROTOCOL_ID = Buffer.ascii(id);
    private static final Buffer PROTOCOL_MAGIC = new Buffer(new byte[]{'A', 'M', 'Q', 'P'});


    public ProtocolHandler createProtocolHandler() {
        return new AmqpProtocolHandler();
    }

    @Override
    public ProtocolCodec createProtocolCodec(Connector connector) {
        return null;
    }

    @Override
    public boolean isIdentifiable() {
        return true;
    }

    @Override
    public int maxIdentificaionLength() {
        return PROTOCOL_MAGIC.length;
    }

    @Override
    public boolean matchesIdentification(Buffer header) {
        if (header.length < PROTOCOL_MAGIC.length) {
            return false;
        } else {
            return header.startsWith(PROTOCOL_MAGIC);
        }
    }

    @Override
    public String getId() {
        return id;
    }
}

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

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class BufferConversions {

    public static AsciiBuffer toAsciiBuffer(String value) {
        return new AsciiBuffer(value);
    }

    public static UTF8Buffer toUTF8Buffer(String value) {
        return new UTF8Buffer(value);
    }

    public static String fromAsciiBuffer(AsciiBuffer buffer) {
        return buffer.toString();
    }

    public static String fromUTF8Buffer(UTF8Buffer buffer) {
        return buffer.toString();
    }

    public static AsciiBuffer toAsciiBuffer(Buffer value) {
        return value.ascii();
    }

    public static UTF8Buffer toUTF8Buffer(Buffer value) {
        return value.utf8();
    }
}

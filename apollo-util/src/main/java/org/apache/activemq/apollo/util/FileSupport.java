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
package org.apache.activemq.apollo.util;

import java.io.*;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class FileSupport {

    public static String readText(InputStream inputStream) {
        return readText(inputStream, "UTF-8");
    }
    public static String readText(InputStream inputStream, String charset) {
        try {
            return new String(readBytes(inputStream), charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readBytes(InputStream inputStream) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(inputStream, out);
        return out.toByteArray();
    }

    // return bytes copied
    public static long copy(InputStream in, OutputStream out) {
        try {
            Long bytesCopied = 0L;
            byte[] buffer = new byte[8192];
            int bytes = in.read(buffer);
            while (bytes >= 0) {
                out.write(buffer, 0, bytes);
                bytesCopied += bytes;
                bytes = in.read(buffer);
            }
            return bytesCopied;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

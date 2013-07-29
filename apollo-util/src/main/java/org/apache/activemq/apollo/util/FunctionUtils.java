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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class FunctionUtils {

    public static String mkString(List<?> items, String delimiter) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < items.size(); i++) {
            builder.append(items.get(i).toString());

            if (i + 1 < items.size()) {
                builder.append(delimiter);
            }
        }

        return builder.toString();
    }

    public static void using(InputStream is, Procedure0 proc) {

        try {
            proc.execute();
        }finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {

                }
            }
        }
    }

    public static String stripSuffix(String value, String suffix) {
        if (value.indexOf(suffix) != -1) {
            int pos = value.indexOf(suffix);
            return value.substring(0, pos);
        }else {
            return value;
        }
    }

    public static String stripPrefix(String value, String prefix) {
        if (value.indexOf(prefix) != -1) {
            int pos = value.indexOf(prefix);
            return value.substring(pos + prefix.length(), value.length());
        }else {
            return value;
        }
    }
}

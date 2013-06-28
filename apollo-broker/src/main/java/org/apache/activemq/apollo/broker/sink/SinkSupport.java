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
package org.apache.activemq.apollo.broker.sink;

import org.apache.activemq.apollo.util.Function1;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public abstract class SinkSupport<Y,T> {

    public Sink<T> map(final Function1<Y,T> func) {

        return (Sink<T>)new SinkMapper<Y,T>(sink()) {

            @Override
            public <T> T passing(Y value) {
                return (T)func.apply(value);
            }

            @Override
            public String toString() {
                return downstream().toString();
            }
        };
    }

    public Sink<T> flatMap(final Function1<Y,T> func) {
        return (Sink<T>)new FlatMapSink<Y, T>(sink(), func);
    }

    // refers to the sink in question.. that is, the sink that's implemented by interface Sink<T>
    // so usually a class that implements this should just return "this"
    public abstract Sink<T> sink();

}

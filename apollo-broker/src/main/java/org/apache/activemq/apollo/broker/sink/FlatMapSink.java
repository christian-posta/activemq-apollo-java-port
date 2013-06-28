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
public class FlatMapSink<Y,T> extends SinkFilterSupport<T> implements Sink<Y>, SinkFilter<T>{

    private Function1<Y,T> func;

    public FlatMapSink(Sink<T> downstream, Function1<Y,T> func) {
        super(downstream);
        this.func = func;
    }

    @Override
    public String toString() {
        return downstream().toString();
    }

    /**
     * This is a little different from map because it allows a user-defined func to operate on
     * the value.. the map kinda does that, but is muilt into the SinkMapper .. flat map also
     * checks for null before offering downstream
     *
     * @param value
     * @return
     */
    @Override
    public boolean offer(Y value) {
        if (full()) {
            return false;
        } else {
            T val = func.apply(value);
            if (val != null) {
                return downstream().offer(val);
            }
        }


        // todo:ceposta, is this right? should we return false?
        return true;
    }
}

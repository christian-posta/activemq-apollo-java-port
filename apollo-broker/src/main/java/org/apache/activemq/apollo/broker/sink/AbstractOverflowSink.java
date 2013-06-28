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

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.Task;

import java.util.LinkedList;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public abstract class AbstractOverflowSink<T> implements Sink<T> {

    private Sink<T> downstream;
    private LinkedList<T> overflow;
    private Task refiller;

    public AbstractOverflowSink(Sink<T> downstream) {
        refiller = Dispatch.NOOP;
        this.downstream = downstream;
        this.downstream.setRefiller(new Task() {

            @Override
            public void run() {
                drain();
            }
        });
    }

    protected void drain() {
        while (overflowed()) {
            if (!downstream.offer(overflow.peekFirst())) {
                return;
            }else {
                onDelivered(overflow.removeFirst());
            }
        }
    }

    public  T removeFirst() {
        if (!overflow.isEmpty()) {
            T rc = overflow.removeFirst();
            onDelivered(rc);
            return rc;
        }
        return null;
    }

    public T removeLast() {
        if (!overflow.isEmpty()) {
            T rc = overflow.removeLast();
            onDelivered(rc);
            return rc;
        }
        return null;
    }


    public boolean overflowed() {
        return !overflow.isEmpty();
    }

    public Sink<T> downstream(){
        return downstream;
    }

    @Override
    public boolean full() {
        return overflowed() || downstream.full();
    }

    public void clear() {
        this.overflow.clear();
    }

    /**
     *
     * @param value
     * @return true always even when full since those messages just get stored in a
     *         overflow list
     */
    @Override
    public boolean offer(T value) {
        if( overflowed() || !downstream.offer(value)) {
            overflow.addLast(value);
        } else {
            onDelivered(value);
        }
        return true;
    }

    @Override
    public Task refiller() {
        return refiller;
    }

    @Override
    public void setRefiller(Task value) {
    }

    @Override
    public String toString() {
        return "overflow: "+overflow.toString() + ", full: "+full();
    }

    protected abstract void onDelivered(T value);

}

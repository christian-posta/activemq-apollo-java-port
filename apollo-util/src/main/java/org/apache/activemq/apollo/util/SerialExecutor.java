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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Provides serial execution of runnable tasks on any executor.</p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class SerialExecutor implements Executor{
    private final Executor target;

    private final AtomicBoolean triggered = new AtomicBoolean();
    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();


    public SerialExecutor(Executor target) {
        this.target = target;
    }

    final public void execute(Runnable runnable) {
        queue.add(runnable);
        if (triggered.compareAndSet(false, true)) {
            target.execute(new Runnable() {
                @Override
                public void run() {
                    drain();
                }
            });
        }
    }


    final private void drain() {
        while (true) {
            try {
                Runnable action = queue.poll();
                while (action != null) {
                    try {
                        action.run();
                    } catch (Exception e) {
                        Thread thread = Thread.currentThread();
                        thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
                    }
                    action = queue.poll();
                }
            }finally {
                drained();
                triggered.set(false);
                if (queue.isEmpty() || !triggered.compareAndSet(false, true)) {
                    return;
                }
            }
        }
    }

    /**
     * Subclasses can override this as a callback to know when this queue has been drained
     */
    protected void drained() {}
}

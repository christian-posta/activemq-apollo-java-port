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

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class TaskTracker {


    private final HashSet<TrackedTask> tasks = new HashSet<TrackedTask>();
    private Runnable callback;

    private final DispatchQueue queue;
    private boolean done = false;
    protected String name;
    protected Long timeout;

    public TaskTracker() {
        this("unknown");
    }

    public TaskTracker(String name) {
        this(name, 0L);
    }

    public TaskTracker(String name, Long timeout) {
        this.name = name;
        done = false;
        queue = Dispatch.createQueue("tracker: " + name);
        this.timeout = timeout;
    }

    public TrackedTask task() {
        return task("unknown");
    }
    public TrackedTask task(String name) {
        final TrackedTask rc = new TrackedTask(name);
        final Runnable _callback = callback;
        queue.execute(new Task(){

            @Override
            public void run() {
                if (_callback == null || !tasks.isEmpty()) {
                    tasks.add(rc);
                }
            }
        });

        return rc;
    }

    public void callback(final Runnable handler) {
        long start = System.currentTimeMillis();
        queue.execute(new Task() {

            @Override
            public void run() {
                setNewCallbackHandler(handler);
                checkDone();
            }
        });

        new ScheduleChecker(start).scheduleCheck(timeout);
    }

    private class ScheduleChecker {
        private final long start;

        private ScheduleChecker(long start) {
            this.start = start;
        }

        private void scheduleCheck(Long timeout) {
            if (timeout > 0) {
                queue.executeAfter(timeout, TimeUnit.MILLISECONDS, new Task() {

                    @Override
                    public void run() {
                        if (!done) {
                            scheduleCheck(onTimeout(start, getRemainingTasksAsStringList()));
                        }
                    }
                });
            }
        }
    }

    private List<String> getRemainingTasksAsStringList() {
        LinkedList<String> remaining = new LinkedList<String>();
        for (TrackedTask task : tasks) {
            remaining.add(task.toString());
        }
        return remaining;
    }

    /**
     * Subclasses can override if they want to log the timeout event.
     * the method should return the next timeout value.  If 0, then
     * it will not check for further timeouts.
     */
    protected Long onTimeout(long started, List<String> tasks) {
        return 0L;
    }

    private void setNewCallbackHandler(Runnable callback) {
        this.callback = callback;
    }

    private void checkDone() {
        assert !done;

        if (tasks.isEmpty() && callback != null && !done) {
            done = true;
            callback.run();
        }
    }

    private void remove(final Runnable r)  {
        queue.execute(new Task() {

            @Override
            public void run() {
                if (tasks.remove(r)) {
                    checkDone();
                }
            }
        });
    }

    /**
     * Latch that waits until tasks are done. If you need to run some code on completion, use
     * callback(...)
     */
    public void await() {
        final CountDownLatch latch = new CountDownLatch(1);
        callback(new Task() {

            @Override
            public void run() {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            // swallowing for now..
        }
    }

    public void await(Long timeout, TimeUnit unit) {
        final CountDownLatch latch = new CountDownLatch(1);
        callback(new Task() {

            @Override
            public void run() {
                latch.countDown();
            }
        });

        try {
            latch.await(timeout, unit);
        } catch (InterruptedException e) {
            // swallowing for now..
        }
    }
    /**
     * Internal class for tracking tasks
     */
    class TrackedTask extends Task {
        private Object name;

        TrackedTask(Object name) {
            this.name = name;
        }

        @Override
        public void run() {
            remove(this);
        }

        @Override
        public String toString() {
            return name.toString();
        }
    }

    @Override
    public String toString() {
        synchronized (tasks) {
            return name + " wating on tasks " + tasks;
        }
    }
}

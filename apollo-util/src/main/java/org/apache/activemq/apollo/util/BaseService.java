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


import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.activemq.apollo.util.ServiceState.*;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public abstract class BaseService extends Dispatched implements Service {

    private Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private List<Task> pendingActions = new LinkedList<Task>();

    protected volatile ServiceState serviceState = CREATED;
    protected volatile Throwable serviceFailure = null;

    protected Long startTransitionCounter = 0L;

    protected BaseService(DispatchQueue dispatchQueue) {
        super(dispatchQueue);
    }

    @Override
    public void start(Task onComplete){
        dispatchQueue.execute(new StartTask(onComplete));
    }

    @Override
    public void stop(Task onComplete)  {
        dispatchQueue.execute(new StopTask(onComplete));

    }

    @Override
    public Throwable serviceFailure() {
        return serviceFailure;
    }

    public ServiceState getServiceState() {
        return serviceState;
    }


    class StartTask extends Task {

        private Task onComplete;

        StartTask(Task onComplete) {
            this.onComplete = onComplete;
        }

        // run the start task, figure out what state we're in
        // and take the appropriate transition
        @Override
        public void run() {
            switch (serviceState) {
                case CREATED:
                    doStart();
                    break;
                case STOPPED:
                    doStart();
                    break;
                case STOPPING:
                    // we will just add the start task
                    // to the list of tasks that need to be completed when a stop has completed
                    // so basically, just get in line to "start" because an existing "stop" is Inprog
                    pendingActions.add(this);
                    break;
                case STARTING:
                    pendingActions.add(this);
                    break;
                case STARTED:
                    done();
                    break;
                default:
                    done();
                    LOG.error("Start should not be called from state: " + serviceState);
            }
        }

        void done() {
            for (Task t : pendingActions) {
                dispatchQueue.execute(t);
            }

            pendingActions.clear();

            if (onComplete != null) {
                onComplete.run();
            }
        }

        void doStart() {
            ServiceState state = STARTING;
            serviceState = state;
            startTransitionCounter++;
            try {

                // delegate to subclass to actually do starting
                _start(new Task() {

                    @Override
                    public void run() {
                        serviceState = STARTED;
                        done();
                    }
                });
            } catch (Exception e) {
                LOG.error("Start failed due to " + e, e);
                serviceFailure = e;
                serviceState = FAILED;
                done();
            }
        }


    }

    class StopTask extends Task {

        private Task onComplete;

        StopTask(Task onComplete) {
            this.onComplete = onComplete;
        }

        @Override
        public void run() {
            switch (serviceState) {
                case STARTED:
                    ServiceState state = STOPPING;
                    serviceState = state;
                    startTransitionCounter ++;
                    try {
                        _stop(new Task() {

                            @Override
                            public void run() {
                                serviceState = STOPPED;
                                done();
                            }
                        });
                    } catch (Exception e) {
                        LOG.error("Stop failed due to " + e, e);
                        serviceFailure = e;
                        serviceState = FAILED;
                        done();
                    }
                    break;
                case CREATED:
                    done();
                    break;
                case STOPPED:
                    done();
                    break;
                case STOPPING:
                    pendingActions.add(this);
                    break;
                case STARTING:
                    pendingActions.add(this);
                    break;
                default:
                    done();
                    LOG.error("Stop should not be called from state: " + serviceState);

            }
        }

        void done() {

            Task[] tmp = new Task[pendingActions.size()];
            pendingActions.toArray(tmp);
            pendingActions.clear();

            for (Task t : tmp) {
                dispatchQueue.execute(t);
            }

            pendingActions.clear();

            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    public void scheduleReocurring(Long time, TimeUnit unit, Procedure0 func) {
        Long counter = startTransitionCounter;
        schedule(time, unit, func, counter);
    }

    private void schedule(final Long time, final TimeUnit unit, final Procedure0 func, final Long counter) {
        dispatchQueue.executeAfter(time, unit, new Task() {

            @Override
            public void run() {
                // run the required procedure and try to reschedule when done iff our state hasn't
                // transitioned from start to stop or vice versa
                if (counter == startTransitionCounter) {
                    try {
                        func.execute();
                    }finally {
                        schedule(time, unit, func, counter);
                    }
                }
            }
        });


    }

    /**
     * This method to be implemented by subclasses
     * @param onCompleted
     */
    protected abstract void _start(Task onCompleted);

    /**
     * This method to be implemented by subclasses
     * @param onCompleted
     */
    protected abstract void _stop(Task onCompleted);
}

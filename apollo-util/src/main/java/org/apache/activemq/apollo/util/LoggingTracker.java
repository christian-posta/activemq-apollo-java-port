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

import org.fusesource.hawtdispatch.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import static org.apache.activemq.apollo.util.FunctionUtils.mkString;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class LoggingTracker extends TaskTracker{

    private final Logger LOG;
    private LoggingTrackerStatus status;

    public LoggingTracker(String name) {
        this(name, LoggerFactory.getLogger(LoggingTracker.class.getName()), 1000L);
    }

    public LoggingTracker(String name, Logger log) {
        this(name, log, 1000L);
    }
    public LoggingTracker(String name, Logger log, Long timeout) {
        super(name, timeout);
        LOG = log;
    }

    @Override
    protected Long onTimeout(long started, List<String> tasks) {
        if (status == null) {
            LOG.info("%s is waiting on %s", name, mkString(tasks, ","));
            status = new LoggingTrackerStatus(started, tasks);

        }else {
            if (!tasks.equals(status.tasks)) {
                LOG.info("%s is now waiting on %s", name, mkString(tasks, ","));
                status = new LoggingTrackerStatus(started, tasks);
            }
        }
        return timeout;
    }

    @Override
    public void callback(final Runnable handler) {
        super.callback(new Task() {

            @Override
            public void run() {
                if (status != null) {
                    LOG.info("%s is no longer waiting.  It waited a total of %d seconds.", name, ((System.currentTimeMillis() - status.start) / 1000));
                    status = null;
                }
                handler.run();
            }
        });
    }

    public void start(Service service) {
        try {
            service.start(task("start " + service.toString()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop(Service service) {
        try {
            service.stop(task("stop " + service.toString()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class LoggingTrackerStatus {
        Long start;
        List<String> tasks;

        private LoggingTrackerStatus(Long start, List<String> tasks) {
            this.start = start;
            this.tasks = tasks;
        }
    }

}

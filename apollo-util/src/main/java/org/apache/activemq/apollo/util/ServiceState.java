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

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public enum ServiceState {

    CREATED,
    STARTING,
    STARTED,
    STOPPING,
    STOPPED,
    FAILED;

    private final long since;


    private ServiceState() {
        this.since = System.currentTimeMillis();
    }

    // is on its way starting up
    public boolean isUpward() {
        return isStarted() || isStarting() || isCreated();
    }

    public boolean isStartingOrStarted() {
        return isStarted() | isStarting();
    }


    // is on its way shutting down
    public boolean isDownward() {
        return isStopped() | isStopping() | isFailed();
    }

    public boolean isStarted() {
        return this == STARTED;
    }

    public boolean isStarting() {
        return this == STARTING;
    }

    public boolean isCreated() {
        return this == CREATED;
    }

    public boolean isStopping() {
        return this == STOPPING;
    }

    public boolean isStopped() {
        return this == STOPPED;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public long since() {
        return since;
    }
}

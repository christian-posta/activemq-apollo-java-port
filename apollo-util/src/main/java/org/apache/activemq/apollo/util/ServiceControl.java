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

import java.util.List;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class ServiceControl {


    private static void controlService(boolean start, Service service, String action) {
        LoggingTracker tracker = new LoggingTracker(action);
        if (start) {
            tracker.start(service);
        }else {
            tracker.stop(service);
        }

        tracker.await();
    }

    private static void controlService(boolean start, List<Service> services, String action) {
        LoggingTracker tracker = new LoggingTracker(action);
        for (Service service : services) {
            if (start) {
                tracker.start(service);
            }else {
                tracker.stop(service);
            }
        }

        tracker.await();
    }

    public static void start(List<Service> services, String action) {
        controlService(true, services, action);
    }

    public static void stop(List<Service> services, String action) {
        controlService(false, services, action);
    }

    public static void start(Service service, String action) {
        controlService(true, service, action);
    }

    public static void stop(Service service, String action) {
        controlService(false, service, action);
    }

    public static void start(Service service) {
        controlService(true, service, "Starting "+service);

    }
    public static void stop(Service service) {
        controlService(false, service, "Stopping "+service);
    }
}

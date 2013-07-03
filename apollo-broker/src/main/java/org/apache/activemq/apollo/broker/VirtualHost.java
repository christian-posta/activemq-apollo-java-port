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
package org.apache.activemq.apollo.broker;

import org.apache.activemq.apollo.broker.security.Authenticator;
import org.apache.activemq.apollo.broker.security.JaasAuthenticator;
import org.apache.activemq.apollo.dto.LogCategoryDTO;
import org.apache.activemq.apollo.dto.VirtualHostDTO;
import org.apache.activemq.apollo.util.BaseService;
import org.apache.activemq.apollo.util.TaskTracker;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class VirtualHost extends BaseService{
    private Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private Router router;
    private VirtualHostDTO config;
    private Broker broker;

    private Logger securityLog;
    private Logger auditLog;
    private Logger connectionLog;
    private Logger consoleLog;
    private Authenticator authenticator;
    private Authorizer authorizer;

    public VirtualHost(Broker broker) {
        super(Dispatch.createQueue("virtual-host"));
        this.broker = broker;
    }

    @Override
    protected void _start(Task onCompleted) {
    }

    @Override
    protected void _stop(Task onCompleted) {
    }

    public Router getRouter() {
        return router;
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    public VirtualHostDTO getConfig() {
        return config;
    }

    public void setConfig(VirtualHostDTO config) {
        this.config = config;
    }

    public void update(final VirtualHostDTO config, final Task onComplete) {
        dispatchQueue.execute(new Task() {

            @Override
            public void run() {

                // if we're not started, just set the config, and let the normal lifecycle downstrea
                // start the VHost
                if (!serviceState.isStarted()) {
                    VirtualHost.this.config = config;
                    onComplete.run();
                } else {
                    // in some cases we will have to restart the virtual host
                    if (config.store != VirtualHost.this.config.store) {
                        stop(new Task() {

                            @Override
                            public void run() {
                                VirtualHost.this.config = config;
                                start(onComplete);
                            }
                        });
                    } else {
                        // if we don't have to restart the virtual host, then let's just apply updates in place
                        VirtualHost.this.config = config;
                        applyUpdate();
                        VirtualHost.this.router.applyUpdate(onComplete);
                    }
                }

            }
        });

    }

    // apply updates without having to restart the virtual host
    private void applyUpdate() {

        // update the loggers to reflect the categories specified in the config
        LogCategoryDTO logCategor = config.log_category == null ? new LogCategoryDTO() : config.log_category;
        updateSecurityLog(logCategor.security);
        updateAuditLog(logCategor.audit);
        updateConnectionLog(logCategor.connection);
        updateConsoleLog(logCategor.console);

        if (config.authentication != null) {
            // do a direct compare because could be null
            if (config.authentication.enabled == false) {
                authenticator = null;
            } else {
                authenticator = new JaasAuthenticator(config.authentication, securityLog);
            }
        } else {
            // otherwise use overall broker's auth(Z/N) settings
            authenticator = broker.getAuthenticator();
        }

        if (authenticator != null) {
            authorizer = new Authorizer(broker, this);
        } else {
            authorizer = new Authorizer();
        }
    }

    private void updateConsoleLog(String consoleLogCategory) {
        if (emptyOrNull(consoleLogCategory)) {
            consoleLog = broker.getConsoleLog();
        } else {
            consoleLog = LoggerFactory.getLogger(consoleLogCategory);
        }
    }

    private void updateConnectionLog(String connectionLogCategory) {
        if (emptyOrNull(connectionLogCategory)) {
            connectionLog = broker.getConnectionLog();
        } else {
            connectionLog = LoggerFactory.getLogger(connectionLogCategory);
        }
    }

    private void updateAuditLog(String auditLogCategory) {
        if (emptyOrNull(auditLogCategory)) {
            auditLog = broker.getAuditLog();
        } else {
            auditLog = LoggerFactory.getLogger(auditLogCategory);
        }

    }

    private void updateSecurityLog(String securityLogCategory) {
        if (emptyOrNull(securityLogCategory)) {
            securityLog = broker.getSecurityLog();
        }else {
            securityLog = LoggerFactory.getLogger(securityLogCategory);
        }
    }

    private boolean emptyOrNull(String value) {
        return value == null || value.isEmpty();
    }
}

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
package org.apache.activemq.apollo.broker.security;

import org.apache.activemq.apollo.dto.AuthenticationDTO;
import org.apache.activemq.apollo.util.Procedure0;
import org.slf4j.Logger;

import java.util.Set;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class JaasAuthenticator implements Authenticator {
    public JaasAuthenticator(AuthenticationDTO authentication, Logger securityLog) {
    }

    @Override
    public String authenticate(SecurityContext ctx, Procedure0 cb) {
        return null;
    }

    @Override
    public String userName(SecurityContext ctx) {
        return null;
    }

    @Override
    public Set<String> aclPrincipalKinds() {
        return null;
    }
}

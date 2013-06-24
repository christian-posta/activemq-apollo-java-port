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

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class AliasFilteringKeyManager extends X509ExtendedKeyManager {
    private String alias;
    private X509ExtendedKeyManager next;

    public AliasFilteringKeyManager(String alias, X509ExtendedKeyManager km) {
        this.alias = alias;
        this.next = km;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return next.getClientAliases(keyType,issuers);
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return alias;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return next.getServerAliases(keyType, issuers);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return alias;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return next.getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return next.getPrivateKey(alias);
    }

    @Override
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return alias;
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return alias;
    }
}

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

import org.apache.activemq.apollo.dto.KeyStorageDTO;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.LinkedList;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class KeyStorage {

    private KeyStorageDTO config;
    private KeyStore keyStore;
    private TrustManager[] trustManagers;
    private KeyManager[] keyManagers;


    public KeyStorage(KeyStorageDTO config) {
        this.config = config;
    }

    public KeyStore createKeyStore() {
        if (keyStore == null) {
            try {
                keyStore = KeyStore.getInstance(config.store_type == null ? "JKS" : config.store_type);
                try {
                    keyStore.load(new FileInputStream(config.file), getPassword());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        return keyStore;
    }

    public TrustManager[] createTrustManagers() {
        if (trustManagers == null) {
            try {
                TrustManagerFactory factory = TrustManagerFactory.getInstance(getTrustAlgorithm());
                factory.init(createKeyStore());
                trustManagers = factory.getTrustManagers();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return trustManagers;
    }

    public KeyManager[] createKeyManagers() {
        if (keyManagers == null) {
            try {
                KeyManagerFactory factory = KeyManagerFactory.getInstance(getKeyAlgorithm());
                factory.init(createKeyStore(), getKeyPassword());
                keyManagers = factory.getKeyManagers();

                if (config.key_alias != null) {
                    convertKeyManagersToAliasFiltering();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return keyManagers;
    }

    private void convertKeyManagersToAliasFiltering() {
        LinkedList<KeyManager> managers = new LinkedList<KeyManager>();
        for (KeyManager km : keyManagers) {
            if (km instanceof X509ExtendedKeyManager) {
                managers.add(new AliasFilteringKeyManager(config.key_alias, (X509ExtendedKeyManager)km));
            }else {
                managers.add(km);
            }

        }

        KeyManager[] managers1 = new KeyManager[managers.size()];
        managers.toArray(managers1);
        keyManagers = managers1;
    }


    public char[] getPassword() {
        String pw = config.password == null ? "" : config.password;
        return pw.toCharArray();
    }

    public String getTrustAlgorithm() {
        return config.trust_algorithm == null ? "SunX509" : config.trust_algorithm;
    }

    public String getKeyAlgorithm() {
        return config.key_algorithm == null ? "SunX509" : config.key_algorithm;
    }

    public char[] getKeyPassword() {
        String pw = config.key_password == null ? "" : config.key_password;
        return pw.toCharArray();
    }
}

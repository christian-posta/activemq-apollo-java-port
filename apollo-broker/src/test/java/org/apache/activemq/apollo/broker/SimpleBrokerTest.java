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

import org.apache.activemq.apollo.util.FileSupport;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class SimpleBrokerTest {


    @Test
    public void testVersion() {
        Broker broker = new Broker();
        String version = broker.version();
        assertNotNull(version);
        assertTrue(version.length() > 0);
        assertEquals(expectedVersion(), version);

    }

    @Test
    public void testOS() {
        Broker broker = new Broker();
        String os = broker.os();
        assertNotNull(os);
        assertEquals(expectedOS(), os);
        System.out.println(os);
    }

    @Test
    public void testJVM() {
        Broker broker = new Broker();
        String jvm = broker.jvm();
        assertNotNull(jvm);
        assertEquals(exectedJVM(), jvm);

        System.out.println(jvm);
    }

    private String exectedJVM() {
        String vendor = System.getProperty("java.vendor");
        String version = System.getProperty("java.version");
        String vm = System.getProperty("java.vm.name");
        return String.format("%s %s (%s)", vm, version, vendor);
    }

    private String expectedOS() {
        return System.getProperty("os.name") + " " + System.getProperty("os.version");
    }

    private String expectedVersion() {
        InputStream in = getClass().getResourceAsStream("version.txt");
        return FileSupport.readText(in);
    }

}

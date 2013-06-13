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

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class ClassFinderTest {

    private static final String INDEX_FILE = "META-INF/services/org.apache.activemq.apollo/dto-module.index";

    @Test
    public void testClassFinder() {
        ClassFinder<DtoModule> finder = new ClassFinder<DtoModule>(INDEX_FILE,
                DtoModule.class);

        List<DtoModule> singletons = finder.getSingletons();
        assertEquals("Incorrect number of singletons!", 3, singletons.size());

    }
}

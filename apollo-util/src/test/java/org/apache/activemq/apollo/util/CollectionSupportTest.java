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

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class CollectionSupportTest {
    @Test
    public void testDiff() {
        HashSet<Integer> a = new HashSet<Integer>();
        a.add(1);
        a.add(2);
        a.add(3);

        HashSet<Integer> b = new HashSet<Integer>();
        b.add(4);
        b.add(3);
        b.add(2);

        CollectionSupport.DiffResult<Integer> result = CollectionSupport.diff(a, b);

        assertEquals(2, result.getSame().size());
        assertTrue(result.getSame().contains(3));
        assertTrue(result.getSame().contains(2));

        assertEquals(1,result.getRemoved().size());
        assertTrue(result.getRemoved().contains(1));

        assertEquals(1, result.getAdded().size());
        assertTrue(result.getAdded().contains(4));
    }
}

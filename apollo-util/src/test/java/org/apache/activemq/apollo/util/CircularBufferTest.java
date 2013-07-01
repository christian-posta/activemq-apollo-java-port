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

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class CircularBufferTest {

    @Test
    public void testHappyPath() {
        CircularBuffer<Integer> cb = new CircularBuffer<Integer>(3);
        cb.add(1);
        cb.add(2);
        cb.add(3);
        assertEquals(3, cb.size());
        cb.add(4);
        assertEquals(3, cb.size());
        cb.add(4);
        assertEquals(3, cb.size());
        cb.add(4);
        assertEquals(4, (int) cb.get(0));
        assertEquals(4, (int) cb.get(1));
        assertEquals(4, (int) cb.get(2));

        cb.add(10, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddToIndexLargerThanMax() {
        CircularBuffer<Integer> cb = new CircularBuffer<Integer>(3);
        cb.add(10, 1);

    }

}

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

import java.util.ArrayList;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class CircularBuffer<E> extends ArrayList<E>{
    private int maxSize;
    private int pos = 0;

    public CircularBuffer(int maxSize) {
        super(maxSize);
        this.maxSize = maxSize;
    }


    @Override
    public boolean add(E e) {
        if (this.size() < this.maxSize) {
            return super.add(e);
        }
        else {
            onEvicted(this.get(pos));
            super.set(pos, e);
            pos++;
            if (pos > maxSize) {
                pos = 0;
            }
            return true;
        }
    }

    public int getMaxSize() {
        return maxSize;
    }

    protected void onEvicted(E elem) {

    }
}

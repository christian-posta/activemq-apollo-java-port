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

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class CollectionSupport {

    public static <E> DiffResult<E> diff(Set<E> a, Set<E> b) {
        Set<E> same = new HashSet<E>(a);
        same.retainAll(b);

        Set<E> added = new HashSet<E>(b);
        added.removeAll(a);

        Set<E> removed = new HashSet<E>(a);
        removed.removeAll(b);

        return new DiffResult<E>(same, added, removed);
    }

    public static class DiffResult<E> {
        final Set<E> same;
        final Set<E> added;
        final Set<E> removed;

        public DiffResult(Set<E> same, Set<E> added, Set<E> removed) {
            this.same = same;
            this.added = added;
            this.removed = removed;
        }

        public Set<E> getSame() {
            return same;
        }

        public Set<E> getAdded() {
            return added;
        }

        public Set<E> getRemoved() {
            return removed;
        }
    }
}

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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public final class DtoModules {

    private static ClassFinder<DtoModule> finder = new ClassFinder("META-INF/services/org.apache.activemq.apollo/dto-module.index", DtoModule.class);

    public static String[] packages() {
        List<DtoModule> singletons = finder.getSingletons();
        String[] rc = new String[singletons.size()];
        for (int i = 0; i < singletons.size(); i++) {
            rc[i] = singletons.get(i).dtoPackage();
        }
        return rc;
    }

    public static Class<?>[] extensionClasses(){
        List<DtoModule> singletons = finder.getSingletons();
        LinkedList<Class<?>> rcList = new LinkedList<Class<?>>();

        for (DtoModule module : singletons) {
            rcList.addAll(Arrays.asList(module.extensionClasses()));
        }

        Class<?>[] rc = new Class<?>[rcList.size()];
        rcList.toArray(rc);
        return rc;
    }
}

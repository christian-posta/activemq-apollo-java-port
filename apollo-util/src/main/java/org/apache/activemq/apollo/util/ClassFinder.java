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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * This class is responsible for dynamically loading classes from index files (key value
 * files with only keys)
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class ClassFinder<T> {

    private Logger LOG = LoggerFactory.getLogger(getClass().getName());
    private final List<T> singletons;

    public ClassFinder(String path, Class<T> clazz) {
        Set<ClassLoader> loaders = new LinkedHashSet<ClassLoader>();
        loaders.add(getDefaultClassLoader());
        singletons = discover(loaders, path, clazz);
    }


    protected List<T> discover(Set<ClassLoader> loaders, String path, Class<T> clazz) {
        LinkedList<T> rc = null;
        for (ClassLoader loader : loaders) {

            try {
                Enumeration<URL> resources = loader.getResources(path);
                Set<String> classNames = new LinkedHashSet<String>();
                LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
                while (resources.hasMoreElements()) {
                    Properties p = loadProperties(resources.nextElement().openStream());
                    for (Object o : p.keySet()) {
                        classNames.add((String)o);
                    }
                }

                for (String name : classNames) {
                    try {
                        classes.add(loader.loadClass(name));
                    } catch (ClassNotFoundException e) {
                        LOG.debug("Could not load class %s when using class loader %s", name, loader);
                    }
                }

                rc = new LinkedList<T>(instantiate(classes, clazz));
            } catch (IOException e) {
                throw new RuntimeException("Could not discover classes using class finder", e);
            }
        }

        return rc;
    }

    private Set<T> instantiate(List<Class<?>> classes, Class<?> clazz) {
        LinkedHashSet<T> distinct = new LinkedHashSet<T>();
        for (Class<?> aClass : classes) {
            try {
                distinct.add((T)aClass.cast(aClass.newInstance()));
            } catch (Throwable e) {
                LOG.debug("Could not load class %s when using class loader %s", clazz.getName(), clazz.getClassLoader());

            }
        }

        return distinct;
    }

    private Properties loadProperties(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }

        try {
            Properties rc = new Properties();
            rc.load(inputStream);

            return rc;
        } catch (IOException e) {
            return null;
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // swallow exception
            }
        }
    }

    public static ClassLoader getDefaultClassLoader() {
        ClassLoader rc = ClassFinder.class.getClassLoader();
        if (rc == null) {
            return ClassLoader.getSystemClassLoader();
        }

        return rc;
    }

    public List<T> getSingletons() {
        return Collections.unmodifiableList(singletons);
    }
}

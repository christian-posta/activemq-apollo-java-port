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
package org.apache.activemq.apollo.stomp.test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class RegexMatcher extends BaseMatcher {
    private final String regex;
    private final Pattern pattern;

    public RegexMatcher(String regex){
        this.regex = regex;
        pattern = Pattern.compile(regex);
    }

    public boolean matches(Object o){
        String s = (String)o;
        return pattern.matcher(s).find(0);
    }

    public void describeTo(Description description){
        description.appendText("matches regex=");
        description.appendText(regex);
    }

    public static RegexMatcher regex(String regex){
        return new RegexMatcher(regex);
    }
}

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
import org.slf4j.Marker;

import java.util.ArrayList;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class MemoryLogger implements Logger{

    protected Logger next;
    protected ArrayList<LogEntry> messages = new ArrayList<LogEntry>();

    public MemoryLogger(Logger next) {
        this.next = next;
    }

    public Logger add(String level, String message){
        messages.add(new LogEntry(level, message));
        while (messages.size() > 1000) {
            messages.remove(0);
        }

        return next;
    }

    @Override
    public String getName() {
        return next.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(String msg) {
        add("trace", msg).trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(String msg, Throwable t) {
        add("trace", msg).trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return true;
    }

    @Override
    public void trace(Marker marker, String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String msg) {
        add("debug", msg).debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(String msg, Throwable t) {
        add("debug", msg).debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return true;
    }

    @Override
    public void debug(Marker marker, String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInfoEnabled() {
        return true ;
    }

    @Override
    public void info(String msg) {
        add("info", msg).info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(String msg, Throwable t) {
        add("info", msg).info(msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return true;
    }

    @Override
    public void info(Marker marker, String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        add("warn", msg).warn(msg);
    }

    @Override
    public void warn(String msg, Throwable t) {
        add("warn", msg).warn(msg, t);
    }

    @Override
    public void warn(String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
    public void warn(Marker marker, String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        add("error", msg).error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(String msg, Throwable t) {
        add("error", msg).error(msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return true;
    }

    @Override
    public void error(Marker marker, String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String format, Object[] argArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException();
    }

    class LogEntry {
        private long ts = System.currentTimeMillis();
        String message;
        String level;

        LogEntry(String level, String message) {
            this.message = message;
            this.level = level;
        }


    }
}

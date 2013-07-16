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
package org.apache.activemq.apollo.broker.web;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class AllowAnyOriginFilter implements Filter {

    private final Set<String> allowed;
    private boolean allowAny;

    public AllowAnyOriginFilter(Set<String> allowed) {
        this.allowed = allowed;
        this.allowAny = allowed.contains("*");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void destroy() { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (response instanceof HttpServletResponse) {
            HttpServletRequest req = (HttpServletRequest) request;
            String origin = req.getHeader("Origin");

            if (allowAny || allowed.contains(origin)) {
                if (origin != null) {
                    ((HttpServletResponse) response).addHeader("Access-Control-Allow-Origin", origin);
                } else {
                    ((HttpServletResponse) response).addHeader("Access-Control-Allow-Origin", "*");
                }

                if ("OPTIONS".equals(req.getMethod())) {
                    ((HttpServletResponse) response).addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
                    String reqHeader = req.getHeader("Access-Control-Request-Headers");
                    if (reqHeader != null && !reqHeader.trim().isEmpty()) {
                        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Headers", reqHeader);
                    }

                    ((HttpServletResponse) response).addHeader("Access-Control-Max-Age", ""+ TimeUnit.DAYS.toSeconds(1));
                }
            }
        }

        chain.doFilter(request, response);
    }
}

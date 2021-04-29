/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.ext.servlet.valve;

import com.google.gson.Gson;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.lang.StringUtils;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * Tomcat valve which adds MDC from a header value received.
 * The header and its MDC can be configured.
 * <p>
 * By default a HTTP header "activityid" is added to MDC "Correlation-ID"
 * <p>
 * The header and MDC can be configured in tomcat valve configuration like,
 * <code>
 *
 * </code>
 */
public class IPAddressValve extends ValveBase {

    private static final String REMOTE_HOST_MDC = "Remote-Host";
    private Map<String, String> headerToIdMapping;
    private Map<String, String> queryToIdMapping;
    private static List<String> toRemoveFromThread = new ArrayList<>();
    private String IPAddressMdc = REMOTE_HOST_MDC;
    private String headerToIPAddressMapping;
    private String queryToIPAddressMapping;
    private String configuredIPAddressMdc;

    @Override
    protected void initInternal() throws LifecycleException {

        super.initInternal();
        Gson gson = new Gson();
        if (StringUtils.isNotEmpty(headerToIPAddressMapping)) {
            headerToIdMapping = gson.fromJson(this.headerToIPAddressMapping, Map.class);
            toRemoveFromThread.addAll(headerToIdMapping.values());
        }

        if (StringUtils.isNotEmpty(queryToIPAddressMapping)) {
            queryToIdMapping = gson.fromJson(this.queryToIPAddressMapping, Map.class);
            toRemoveFromThread.addAll(queryToIdMapping.values());
        }

        if (StringUtils.isNotEmpty(configuredIPAddressMdc)) {
            IPAddressMdc = configuredIPAddressMdc;
        }
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        try {
            associateHeadersToThread(request);
            if (MDC.get(IPAddressMdc) == null) {
                MDC.put(IPAddressMdc, "-");
            }
            getNext().invoke(request, response);
        } finally {
            disAssociateFromThread();
            MDC.remove(IPAddressMdc);
        }
    }

    /**
     * Remove all headers values associated with the thread.
     */

    private void disAssociateFromThread() {

        if (toRemoveFromThread != null) {
            for (String correlationIdName : toRemoveFromThread) {
                MDC.remove(correlationIdName);
            }
        }
    }

    /**
     * Search though the list of headers configured against headers received.
     *
     * @param servletRequest request received
     */
    private void associateHeadersToThread(ServletRequest servletRequest) {
        if (headerToIdMapping != null && (servletRequest instanceof HttpServletRequest)) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            String headerValue = httpServletRequest.getRemoteHost();
            if (StringUtils.isNotEmpty(headerValue)) {
                MDC.put(REMOTE_HOST_MDC, headerValue);
            }
        }
    }

    public void setHeaderToCorrelationIdMapping(String headerToCorrelationIdMapping) {

        this.headerToIPAddressMapping = headerToCorrelationIdMapping;
    }

    public void setQueryToCorrelationIdMapping(String queryToCorrelationIdMapping) {

        this.queryToIPAddressMapping = queryToCorrelationIdMapping;
    }

    public void setConfiguredCorrelationIdMdc(String configuredCorrelationIdMdc) {

        this.configuredIPAddressMdc = configuredCorrelationIdMdc;
    }
}

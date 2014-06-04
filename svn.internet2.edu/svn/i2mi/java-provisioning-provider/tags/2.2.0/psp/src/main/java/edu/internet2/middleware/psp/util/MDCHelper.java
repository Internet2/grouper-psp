/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.psp.util;

import org.opensaml.xml.util.DatatypeHelper;
import org.openspml.v2.msg.spml.Request;
import org.slf4j.MDC;

/**
 * Help control MDC logging.
 */
public class MDCHelper {

    /** The MDC request ID. */
    public static final String MDC_REQUESTID = "requestID";

    /** An SPML request. */
    private Request request;

    /** True if this object puts a key into the MDC. */
    private boolean isStarted;

    /**
     * 
     * Constructor.
     * 
     * @param request the SPML request
     */
    public MDCHelper(Request request) {
        this.request = request;
    }

    /**
     * Add the request to the MDC.
     * 
     * @return the mdc helper
     */
    public MDCHelper start() {

        if (DatatypeHelper.isEmpty(MDC.get(MDC_REQUESTID))) {
            MDC.put(MDC_REQUESTID, PSPUtil.toString(request));
            isStarted = true;
        }

        return this;
    }

    /**
     * Remove the request from the MDC.
     */
    public void stop() {
        if (isStarted) {
            MDC.remove(MDC_REQUESTID);
        }
    }
}

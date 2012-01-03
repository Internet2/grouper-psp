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

package edu.internet2.middleware.psp;

import java.util.Map;

import edu.internet2.middleware.psp.spml.request.ProvisioningRequest;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;

/** Contains attributes returned from an attribute resolver resulting from the evaluation of a provisioning request. */
public class PspContext {

    /** The attributes returned from an attribute authority. */
    private Map<String, BaseAttribute<?>> attributes;

    /** The provisioning request. */
    private ProvisioningRequest provisioningRequest;

    /** The provisioning service provider. */
    private Psp provisioningServiceProvider;

    /**
     * Gets the attributes returned from the attribute authority.
     * 
     * @return the attributes returned from the attribute authority
     */
    public Map<String, BaseAttribute<?>> getAttributes() {
        return attributes;
    }

    /**
     * Sets the attributes returned from the attribute authority.
     * 
     * @param attributes the attributes returned from the attribute authority
     */
    public void setAttributes(Map<String, BaseAttribute<?>> attributes) {
        this.attributes = attributes;
    }

    /**
     * Gets the provisioning request.
     * 
     * @return the provisioning request
     */
    public ProvisioningRequest getProvisioningRequest() {
        return provisioningRequest;
    }

    /**
     * Sets the provisioning request.
     * 
     * @param provisioningRequest the provisioning request
     */
    public void setProvisioningRequest(ProvisioningRequest provisioningRequest) {
        this.provisioningRequest = provisioningRequest;
    }

    /**
     * Gets the provisioning service provider.
     * 
     * @return the provisioning service provider
     */
    public Psp getProvisioningServiceProvider() {
        return provisioningServiceProvider;
    }

    /**
     * Sets the provisioning service provider.
     * 
     * @param provisioningServiceProvider the provisioning service provider
     */
    public void setProvisioningServiceProvider(Psp provisioningServiceProvider) {
        this.provisioningServiceProvider = provisioningServiceProvider;
    }
}

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

package edu.internet2.middleware.psp.spml.provider;

import java.util.Collection;
import java.util.Map;

import org.openspml.v2.msg.spml.AddRequest;
import org.openspml.v2.msg.spml.AddResponse;
import org.openspml.v2.msg.spml.DeleteRequest;
import org.openspml.v2.msg.spml.DeleteResponse;
import org.openspml.v2.msg.spml.ErrorCode;
import org.openspml.v2.msg.spml.LookupRequest;
import org.openspml.v2.msg.spml.LookupResponse;
import org.openspml.v2.msg.spml.ModifyRequest;
import org.openspml.v2.msg.spml.ModifyResponse;
import org.openspml.v2.msg.spml.StatusCode;
import org.openspml.v2.msg.spmlsearch.SearchRequest;
import org.openspml.v2.msg.spmlsearch.SearchResponse;
import org.openspml.v2.profiles.dsml.DSMLAttr;
import org.openspml.v2.profiles.dsml.DSMLProfileException;
import org.openspml.v2.profiles.dsml.DSMLValue;

import edu.internet2.middleware.psp.Psp;

/** Base class for a {@link SpmlTarget}. */
public abstract class BaseSpmlTarget extends BaseSpmlProvider implements SpmlTarget {

    /** The provisioning service provider. */
    private Psp psp;

    /** Whether or not multiple modifications are supported in a modify request. Defaults to true. */
    private boolean bundleModifications = true;

    /** {@inheritDoc} */
    public Psp getPSP() {
        return psp;
    }

    /** {@inheritDoc} */
    public void setPSP(Psp psp) {
        this.psp = psp;
    }

    /**
     * Get the value of the dsml attribute with the given name from the map.
     * 
     * @param dsmlAttrs map of dsml attributes
     * @param name dsml attribute name
     * @return the value of the dsml attribute with the given name from the map
     */
    public String getDsmlValue(Map<String, DSMLAttr> dsmlAttrs, String name) {

        if (dsmlAttrs.containsKey(name)) {
            DSMLValue[] dsmlValues = dsmlAttrs.get(name).getValues();
            if (dsmlValues != null && dsmlValues.length == 1) {
                return dsmlValues[0].getValue();
            }
        }

        return null;
    }

    /**
     * Gets a dsml attribute with the given name and values.
     * 
     * @param name the name of the returned attribute
     * @param values the values of the returned attribute
     * @return a dsml attribute with the given name and values
     * @throws DSMLProfileException if a dsml error occurs
     */
    public DSMLAttr getDsmlAttr(String name, Collection<String> values) throws DSMLProfileException {

        DSMLValue[] dsmlValues = null;
        DSMLAttr dsmlAttr = new DSMLAttr(name, dsmlValues);
        for (String value : values) {
            dsmlAttr.addValue(new DSMLValue(value));
        }
        return dsmlAttr;
    }

    /** {@inheritDoc} */
    public boolean isBundleModifications() {
        return bundleModifications;
    }

    /** {@inheritDoc} */
    public void setBundleModifications(boolean bundleModifications) {
        this.bundleModifications = bundleModifications;
    }

    /**
     * {@inheritDoc}
     * 
     * The target ID of the request's PSO identifier and this target's ID must be equal.
     */
    public void validate(AddRequest addRequest, AddResponse addResponse) {

        super.validate(addRequest, addResponse);

        if (addResponse.getStatus() != null && !addResponse.getStatus().equals(StatusCode.SUCCESS)) {
            return;
        }

        if (!addRequest.getPsoID().getTargetID().equals(getId())) {
            fail(addResponse, ErrorCode.NO_SUCH_IDENTIFIER);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * The target ID of the request's PSO identifier and this target's ID must be equal.
     */
    public void validate(DeleteRequest deleteRequest, DeleteResponse deleteResponse) {

        super.validate(deleteRequest, deleteResponse);

        if (deleteResponse.getStatus() != null && !deleteResponse.getStatus().equals(StatusCode.SUCCESS)) {
            return;
        }

        if (!deleteRequest.getPsoID().getTargetID().equals(getId())) {
            fail(deleteResponse, ErrorCode.NO_SUCH_IDENTIFIER);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * The target ID of the request's PSO identifier and this target's ID must be equal.
     */
    public void validate(LookupRequest lookupRequest, LookupResponse lookupResponse) {

        super.validate(lookupRequest, lookupResponse);

        if (lookupResponse.getStatus() != null && !lookupResponse.getStatus().equals(StatusCode.SUCCESS)) {
            return;
        }

        if (!lookupRequest.getPsoID().getTargetID().equals(getId())) {
            fail(lookupResponse, ErrorCode.NO_SUCH_IDENTIFIER);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * The target ID of the request's PSO identifier and this target's ID must be equal.
     */
    public void validate(ModifyRequest modifyRequest, ModifyResponse modifyResponse) {

        super.validate(modifyRequest, modifyResponse);

        if (modifyResponse.getStatus() != null && !modifyResponse.getStatus().equals(StatusCode.SUCCESS)) {
            return;
        }

        if (!modifyRequest.getPsoID().getTargetID().equals(getId())) {
            fail(modifyResponse, ErrorCode.NO_SUCH_IDENTIFIER);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * The target ID of the request's search query and this target's ID must be equal.
     */
    public void validate(SearchRequest searchRequest, SearchResponse searchResponse) {

        super.validate(searchRequest, searchResponse);
        if (searchResponse.getStatus() != null && !searchResponse.getStatus().equals(StatusCode.SUCCESS)) {
            return;
        }

        if (!searchRequest.getQuery().getTargetID().equals(getId())) {
            fail(searchResponse, ErrorCode.NO_SUCH_IDENTIFIER);
        }
    }

}

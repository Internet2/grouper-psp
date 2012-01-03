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

package edu.internet2.middleware.psp.spml.config;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.openspml.v2.profiles.dsml.DSMLAttr;
import org.openspml.v2.profiles.dsml.DSMLProfileException;
import org.openspml.v2.profiles.dsml.DSMLValue;

import edu.internet2.middleware.psp.PspContext;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;

/** Represents an spmlv2 provisioning service object attribute calculated from a shibboleth attribute resolver. */
public class PsoAttribute {

    /** Whether or not to return the first or all values from the attribute resolver. */
    private boolean isMultiValued;

    /** The provisioned object attribute name. */
    private String name;

    /** The id of the attribute resolver definition referred to. */
    private String ref;

    /** Whether or not values are added and deleted or replaced. */
    private boolean replaceValues;

    /** Whether or not existing values are deleted during a modification. */
    private boolean retainAll;

    /**
     * Gets the attribute from the psp context.
     * 
     * @param context the psp context containing attribute resolver attributes
     * @return the attribute
     * @throws DSMLProfileException if a dsml error occurs
     */
    public DSMLAttr getAttribute(PspContext context) throws DSMLProfileException {

        Map<String, BaseAttribute<?>> attributes = context.getAttributes();

        if (!attributes.containsKey(ref)) {
            return null;
        }

        BaseAttribute<?> attribute = attributes.get(ref);

        DSMLValue[] dsmlValues = null;

        DSMLAttr dsmlAttr = new DSMLAttr(this.getName(), dsmlValues);

        if (isMultiValued()) {
            for (Object value : attribute.getValues()) {
                dsmlAttr.addValue(new DSMLValue(value.toString()));
            }
        } else {
            dsmlAttr.addValue(new DSMLValue(attribute.getValues().iterator().next().toString()));
        }

        return dsmlAttr;
    }

    /**
     * The name of this attribute.
     * 
     * @return the name of this attribute
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the id of the attribute definition referred to.
     * 
     * @return the id of the attribute definition referred to
     */
    public String getRef() {
        return ref;
    }

    /**
     * Gets whether or not to return the first or all values from the attribute resolver.
     * 
     * @return whether or not to return the first or all values from the attribute resolver
     */
    public boolean isMultiValued() {
        return isMultiValued;
    }

    /**
     * Gets whether or not values are added and deleted or replaced during a modification.
     * 
     * @return whether or not values are added and deleted or replaced during a modification
     */
    public boolean isReplaceValues() {
        return replaceValues;
    }

    /**
     * Gets whether or not existing values are deleted during a modification.
     * 
     * @return whether or not existing values are deleted during a modification
     */
    public boolean isRetainAll() {
        return retainAll;
    }

    /**
     * Sets whether or not to return the first or all values from the attribute resolver.
     * 
     * @param isMultiValued whether or not to return the first or all values from the attribute resolver
     */
    public void setIsMultiValued(boolean isMultiValued) {
        this.isMultiValued = isMultiValued;
    }

    /**
     * Set the name of this attribute.
     * 
     * @param name the name of this attribute
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the id of the attribute definition referred to.
     * 
     * @param ref the id of the attribute definition referred to
     */
    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * Sets whether or not values are added and deleted or replaced during a modification.
     * 
     * @param replaceValues whether or not values are added and deleted or replaced during a modification
     */
    public void setReplaceValues(boolean replaceValues) {
        this.replaceValues = replaceValues;
    }

    /**
     * Sets whether or not existing values are deleted during a modification.
     * 
     * @param retainAll whether or not existing values are deleted during a modification
     */
    public void setRetainAll(boolean retainAll) {
        this.retainAll = retainAll;
    }

    /** {@inheritDoc} */
    public String toString() {
        ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        toStringBuilder.append("name", name);
        toStringBuilder.append("ref", ref);
        toStringBuilder.append("retainAll", retainAll);
        toStringBuilder.append("isMultiValued", isMultiValued);
        return toStringBuilder.toString();
    }
}

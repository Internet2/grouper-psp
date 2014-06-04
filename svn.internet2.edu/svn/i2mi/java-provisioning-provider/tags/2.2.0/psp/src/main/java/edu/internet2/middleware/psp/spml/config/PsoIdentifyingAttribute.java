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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.openspml.v2.profiles.dsml.DSMLProfileException;
import org.openspml.v2.profiles.dsml.EqualityMatch;
import org.openspml.v2.profiles.dsml.Filter;

/** Represents the attribute of the provisioned object which maps the object to the corresponding schema entity. */
public class PsoIdentifyingAttribute {

    /** The name of the identifying attribute. */
    private String name;

    /** The value of the identifying attribute. */
    private String value;

    /**
     * Gets the spmlv2 filter used to search for objects with this attribute on a target.
     * 
     * @return the spmlv2 filter used to search for objects with this attribute on a target
     * @throws DSMLProfileException if the value is DSMLv2 invalid
     */
    public Filter getFilter() throws DSMLProfileException {
        Filter filter = new Filter();
        filter.setItem(new EqualityMatch(name, value));
        return filter;
    }

    /**
     * Gets the name of the identifying attribute.
     * 
     * @return the name of the identifying attribute
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the value of the identifying attribute.
     * 
     * @return the value of the identifying attribute
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the name of the identifying attribute.
     * 
     * @param name name of the identifying attribute
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the value of the identifying attribute.
     * 
     * @param value the value of the identifying attribute
     */
    public void setValue(String value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    public String toString() {
        ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        toStringBuilder.append("name", name);
        toStringBuilder.append("value", value);
        return toStringBuilder.toString();
    }
}

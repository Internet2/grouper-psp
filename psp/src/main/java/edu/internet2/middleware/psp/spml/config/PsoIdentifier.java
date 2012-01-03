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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.psp.PspContext;
import edu.internet2.middleware.psp.PspException;
import edu.internet2.middleware.psp.util.PSPUtil;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;

/** Represents an spmlv2 provisioning service object identifier calculated from a shibboleth attribute resolver. */
public class PsoIdentifier {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PsoIdentifier.class);

    /** The container id. */
    private String containerId;

    /** The id of the attribute resolver definition referred to. */
    private String ref;

    /** The target id. */
    private String targetId;

    /**
     * Gets the container id.
     * 
     * @return the container id
     */
    public String getContainerId() {
        return containerId;
    }

    /**
     * Gets pso identifiers from the attribute resolver context.
     * 
     * @param context the attribute resolver context
     * @return the possibly empty list of pso identifiers
     * @throws PspException if a {@link PSOIdentifier} is not returned from the attribute resolution
     */
    public List<PSOIdentifier> getPSOIdentifier(PspContext context) throws PspException {

        return getPSOIdentifier(context, ref);
    }

    /**
     * Gets pso identifiers from the attribute resolver context.
     * 
     * @param context the attribute resolver context
     * @param attributeID the id of the attribute whose value is the pso identifier
     * @return the possibly empty list of pso identifiers
     * @throws PspException if a {@link PSOIdentifier} is not returned from the attribute resolution
     */
    public List<PSOIdentifier> getPSOIdentifier(PspContext context, String attributeID) throws PspException {

        List<PSOIdentifier> psoIDs = new ArrayList<PSOIdentifier>();

        Map<String, BaseAttribute<?>> attributes = context.getAttributes();

        if (!attributes.containsKey(attributeID)) {
            LOG.debug("PSO Identifier Definition '{}' - Source attribute '{}' does not exist", getRef(), attributeID);
            return psoIDs;
        }

        BaseAttribute<?> attribute = attributes.get(attributeID);

        if (attribute.getValues().isEmpty()) {
            LOG.debug("PSO Identifier Definition '{}' - No dependency values", getRef());
            return psoIDs;
        }

        for (Object value : attribute.getValues()) {
            if (!(value instanceof PSOIdentifier)) {
                LOG.error("PSO Identifier Definition '{}' - Unable to calculate identifier, returned object is not a "
                        + PSOIdentifier.class + " : {}", getRef(), value.getClass());
                throw new PspException("Unable to calculate identifier, returned object is not a "
                        + PSOIdentifier.class + " : " + value.getClass());
            }

            PSOIdentifier psoIdentifier = (PSOIdentifier) value;
            psoIdentifier.setTargetID(targetId);

            // TODO set container ID ?

            psoIDs.add(psoIdentifier);
            LOG.debug("PSO Identifier Definition '{}' - Returned '{}'", getRef(), PSPUtil.getString(psoIdentifier));
        }

        return psoIDs;
    }

    /**
     * Gets the id of the attribute resolver definition referred to.
     * 
     * @return the id of the attribute resolver definition referred to
     */
    public String getRef() {
        return ref;
    }

    /**
     * Gets the target id.
     * 
     * @return the target id
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * Sets the container id.
     * 
     * @param containerId the container id
     */
    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    /**
     * Sets the id of the attribute resolver definition referred to.
     * 
     * @param ref id of the attribute resolver definition referred to
     */
    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * Sets the target id.
     * 
     * @param targetId the target id
     */
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    /** {@inheritDoc} */
    public String toString() {
        ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        toStringBuilder.append("ref", ref);
        toStringBuilder.append("targetId", targetId);
        toStringBuilder.append("containerId", containerId);
        return toStringBuilder.toString();
    }
}

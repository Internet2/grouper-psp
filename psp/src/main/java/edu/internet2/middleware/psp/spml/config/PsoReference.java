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
import org.openspml.v2.msg.spml.ErrorCode;
import org.openspml.v2.msg.spml.PSO;
import org.openspml.v2.msg.spml.Request;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spml.SchemaEntityRef;
import org.openspml.v2.msg.spml.StatusCode;
import org.openspml.v2.msg.spmlref.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.psp.PspContext;
import edu.internet2.middleware.psp.PspException;
import edu.internet2.middleware.psp.spml.request.CalcRequest;
import edu.internet2.middleware.psp.spml.request.CalcResponse;
import edu.internet2.middleware.psp.util.OnNotFound;
import edu.internet2.middleware.psp.util.PSPUtil;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;

/** Represents an spmlv2 provisioning service object reference calculated from a shibboleth attribute resolver. */
public class PsoReference {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PsoReference.class);

    /** Whether or not to allow multiple provisioned objects on a target for a single subject. */
    private boolean multipleResults;

    /** The action performed when the object referred to is not found. */
    private OnNotFound onNotFound;

    /** The id of the attribute resolver definition referred to. */
    private String ref;

    /** The provisioning service object referred to. */
    private Pso toObject;

    /**
     * Gets the action performed when the object referred to is not found.
     * 
     * @return the action performed when the object referred to is not found
     */
    public OnNotFound getOnNotFound() {
        return onNotFound;
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
     * Gets the references from the context.
     * 
     * @param context the psp context
     * @param typeOfReference the name of the provisioned object attribute
     * @return the references
     * @throws PspException
     */
    public List<Reference> getReferences(PspContext context, String typeOfReference) throws PspException {

        LOG.debug("Pso reference '{}' - Get references for '{}'", getRef(), context.getProvisioningRequest().getId());

        ArrayList<Reference> references = new ArrayList<Reference>();

        Map<String, BaseAttribute<?>> attributes = context.getAttributes();

        if (!attributes.containsKey(ref)) {
            LOG.debug("Pso reference '{}' - Source attribute does not exist", getRef());
            return references;
        }

        // resolve identifiers
        BaseAttribute<?> referenceAttribute = attributes.get(ref);
        for (Object value : referenceAttribute.getValues()) {
            CalcRequest calcRequest = new CalcRequest();
            calcRequest.setReturnData(ReturnData.IDENTIFIER);
            calcRequest.setId(value.toString());
            SchemaEntityRef schemaEntityRef = new SchemaEntityRef();
            schemaEntityRef.setTargetID(getToObject().getPsoIdentifier().getTargetId());
            schemaEntityRef.setEntityName(getToObject().getId());
            calcRequest.addSchemaEntity(schemaEntityRef);

            CalcResponse calcResponse =
                    (CalcResponse) context.getProvisioningServiceProvider().execute((Request) calcRequest);

            List<PSO> psos = calcResponse.getPSOs();

            if (calcResponse.getStatus().equals(StatusCode.FAILURE)
                    || (calcResponse.getStatus().equals(StatusCode.SUCCESS) && psos.isEmpty())) {
                if (onNotFound.equals(OnNotFound.warn)) {
                    LOG.warn("Pso reference '{}' - Unable to resolve identifier '{}'", getRef(), value);
                } else if (onNotFound.equals(OnNotFound.fail)) {
                    LOG.error("Pso reference '{}' - Unable to resolve identifier '{}'", getRef(), value);
                    throw new PspException("Unable to resolve identifier '" + value + "'");
                }
            }

            if (calcResponse.getStatus().equals(StatusCode.SUCCESS)) {

                // TODO correct handling of multiple results ?
                if (!multipleResults && psos.size() > 1) {
                    LOG.error("Pso reference '{}' - Unable to resolve references {} results found", getRef(),
                            psos.size());
                    throw new PspException("Unable to resolve references, multiple results found.");
                }

                for (PSO pso : psos) {
                    Reference reference = new Reference();
                    reference.setToPsoID(pso.getPsoID());
                    reference.setTypeOfReference(typeOfReference);
                    references.add(reference);
                }
            }

            // TODO correct handling of status=pending ?
            if (calcResponse.getStatus().equals(StatusCode.PENDING)) {
                LOG.error("Pso reference '{}' - Unable to resolve identifier '{}' "
                        + ErrorCode.UNSUPPORTED_EXECUTION_MODE, getRef(), value);
                throw new PspException(ErrorCode.UNSUPPORTED_EXECUTION_MODE.toString());
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Pso reference '{}' - Found {} references", getRef(), references.size());
            for (Reference reference : references) {
                LOG.debug("Pso reference '{}' - Reference '{}'", getRef(), PSPUtil.getString(reference));
            }
        }

        return references;
    }

    /**
     * Gets the provisioning service object referred to.
     * 
     * @return the provisioning service object referred to
     */
    public Pso getToObject() {
        return toObject;
    }

    /**
     * Gets whether or not to allow multiple provisioned objects on a target for a single subject.
     * 
     * @return whether or not to allow multiple provisioned objects on a target for a single subject
     */
    public boolean isMultipleResults() {
        return multipleResults;
    }

    /**
     * Sets whether or not to allow multiple provisioned objects on a target for a single subject.
     * 
     * @param multipleResults whether or not to allow multiple provisioned objects on a target for a single subject
     */
    public void setMultipleResults(boolean multipleResults) {
        this.multipleResults = multipleResults;
    }

    /**
     * Sets the action performed when the object referred to is not found.
     * 
     * @param onNotFound the action performed when the object referred to is not found
     */
    public void setOnNotFound(OnNotFound onNotFound) {
        this.onNotFound = onNotFound;
    }

    /**
     * Sets the id of the attribute resolver definition referred to.
     * 
     * @param ref the id of the attribute resolver definition referred to
     */
    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * Sets the provisioning service object referred to.
     * 
     * @param toObject the provisioning service object referred to
     */
    public void setToObject(Pso toObject) {
        this.toObject = toObject;
    }

    /** {@inheritDoc} */
    public String toString() {
        ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        toStringBuilder.append("ref", ref);
        toStringBuilder.append("toObject", toObject.getId());
        toStringBuilder.append("onNotFound", onNotFound);
        toStringBuilder.append("multipleResults", multipleResults);
        return toStringBuilder.toString();
    }
}

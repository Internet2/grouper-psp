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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.openspml.v2.msg.spmlref.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.psp.PspContext;
import edu.internet2.middleware.psp.PspException;

/** Represents spmlv2 provisioning service object references calculated from a shibboleth attribute resolver. */
public class PsoReferences {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PsoReferences.class);

    /** The reference URI. */
    public static final URI REFERENCE_URI;

    /** The reference URI text. */
    public static final String REFERENCE_URI_STRING = "urn:oasis:names:tc:SPML:2:0:reference";

    static {
        try {
            REFERENCE_URI = new URI(REFERENCE_URI_STRING);
        } catch (URISyntaxException e) {
            LOG.error("Unable to parse URI", e);
            throw new RuntimeException("Unable to parse URI", e);
        }
    }

    /** Whether or not comparison of references is case sensitive. */
    private boolean caseSensitive = true;

    /** The value of the provisioned attribute if no references exist. */
    private String emptyValue = null;

    /** The name of the provisioned attribute whose values are references. */
    private String name;

    /** The pso references. */
    private List<PsoReference> psoReferences;

    /**
     * Gets the reference whose id is the empty value when no references from the context exist.
     * 
     * @return the reference whose id is the empty value when no references from the context exist
     */
    protected List<Reference> getEmptyReferences() {
        List<Reference> references = new ArrayList<Reference>();

        Set<String> targetIds = new LinkedHashSet<String>();
        for (PsoReference psoReferenceDefinition : psoReferences) {
            targetIds.add(psoReferenceDefinition.getToObject().getPsoIdentifier().getTargetId());
        }

        for (String targetId : targetIds) {
            PSOIdentifier psoID = new PSOIdentifier();
            psoID.setTargetID(targetId);
            psoID.setID(emptyValue);

            Reference reference = new Reference();
            reference.setToPsoID(psoID);
            reference.setTypeOfReference(name);
            references.add(reference);
        }

        return references;
    }

    /**
     * Gets the value of the provisioned attribute if no references exist.
     * 
     * @return the value of the provisioned attribute if no references exist
     */
    public String getEmptyValue() {
        return emptyValue;
    }

    /**
     * Gets the name of the provisioned attribute whose values are references.
     * 
     * @return the name of the provisioned attribute whose values are references
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the references which refer to the attribute resolver definition with the given id.
     * 
     * @param ref the id of the attribute resolver definition
     * @return the possibly null reference
     */
    public PsoReference getPsoReference(String ref) {
        for (PsoReference psoReference : psoReferences) {
            if (psoReference.getRef().equals(ref)) {
                return psoReference;
            }
        }
        return null;
    }

    /**
     * Gets the pso references.
     * 
     * @return the pso references
     */
    public List<PsoReference> getPsoReferences() {
        return psoReferences;
    }

    /**
     * Gets the references from the context.
     * 
     * @param context the psp context
     * @return the possibly empty list of references
     * @throws PspException
     */
    public List<Reference> getReferences(PspContext context) throws PspException {

        LOG.debug("Pso references '{}' - Get references for '{}'", getName(), context.getProvisioningRequest().getId());

        List<Reference> references = new ArrayList<Reference>();

        for (PsoReference psoReferenceDefinition : psoReferences) {
            references.addAll(psoReferenceDefinition.getReferences(context, name));
        }

        if (emptyValue != null && references.isEmpty()) {
            references.addAll(getEmptyReferences());
        }

        LOG.debug("Pso references '{}' - Returned {} references.", getName(), references.size());
        return references;
    }

    /**
     * Gets whether or not equality comparison of references should be case sensitive.
     * 
     * @return whether or not equality comparison of references should be case sensitive
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Sets whether or not equality comparison of references should be case sensitive.
     * 
     * @param caseSensitive whether or not equality comparison of references should be case sensitive.
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Sets the value of the provisioned attribute if no references exist.
     * 
     * @param emptyValue the value of the provisioned attribute if no references exist
     */
    public void setEmptyValue(String emptyValue) {
        this.emptyValue = emptyValue;
    }

    /**
     * Sets the name of the provisioned attribute whose values are references.
     * 
     * @param name the name of the provisioned attribute whose values are references
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the pso references.
     * 
     * @param psoReferences the pso references
     */
    public void setPsoReferences(List<PsoReference> psoReferences) {
        this.psoReferences = psoReferences;
    }

    /** {@inheritDoc} */
    public String toString() {
        ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        toStringBuilder.append("name", name);
        toStringBuilder.append("emptyValue", emptyValue);
        return toStringBuilder.toString();
    }
}

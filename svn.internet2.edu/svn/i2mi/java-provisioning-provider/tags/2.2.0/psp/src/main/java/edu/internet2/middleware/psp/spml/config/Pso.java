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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.openspml.v2.msg.spml.Extensible;
import org.openspml.v2.msg.spml.PSO;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spmlref.Reference;
import org.openspml.v2.msg.spmlsearch.Query;
import org.openspml.v2.msg.spmlsearch.Scope;
import org.openspml.v2.profiles.dsml.DSMLAttr;
import org.openspml.v2.profiles.dsml.DSMLProfileException;
import org.openspml.v2.util.Spml2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.psp.PspContext;
import edu.internet2.middleware.psp.PspException;
import edu.internet2.middleware.psp.spml.request.AlternateIdentifier;
import edu.internet2.middleware.psp.util.PSPUtil;

/** Represents an spmlv2 provisioning service object (pso) calculated from a shibboleth attribute resolver. */
public class Pso {

    /** The name of the attribute whose value is the schema entity name, which is also the id of a pso. */
    public static final String ENTITY_NAME_ATTRIBUTE = "entityName";

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(Pso.class);

    /** The id of the attribute definition whose values are all source identifiers. */
    private String allSourceIdentifiersRef;

    /** Whether or not this object should be included in bulk provisioning responses. */
    private boolean authoritative;

    /** The pso id, which is also the schema entity name. */
    private String id;

    /** The alternate identifiers used for object renaming. */
    private List<PsoAlternateIdentifier> psoAlternateIdentifiers;

    /** The pso attributes. */
    private List<PsoAttribute> psoAttributes;

    /** The pso identifier. */
    private PsoIdentifier psoIdentifier;

    /** Maps target objects to pso configurations and is used to search for all target identifiers. */
    private PsoIdentifyingAttribute psoIdentifyingAttribute;

    /** The pso references. */
    private List<PsoReferences> psoReferences;

    /**
     * Gets the id of the attribute definition whose values are all source identifiers.
     * 
     * @return the id of the attribute definition whose values are all source identifiers
     */
    public String getAllSourceIdentifiersRef() {
        return allSourceIdentifiersRef;
    }

    /**
     * Gets the spmlv2 query from the identifying attribute suitable for searching for all target identifiers .
     * 
     * @return the query to search for all target identifiers
     * @throws DSMLProfileException if the filter is invalid
     */
    public Query getAllTargetIdentifiersQuery() throws DSMLProfileException {

        PsoIdentifyingAttribute identifyingAttribute = getPsoIdentifyingAttribute();

        // if no identifying attribute return null
        if (identifyingAttribute == null) {
            return null;
        }

        // new query
        Query query = new Query();

        // target id
        query.setTargetID(getPsoIdentifier().getTargetId());

        // scope
        // TODO support other scopes besides subtree
        query.setScope(Scope.SUBTREE);

        // base
        if (getPsoIdentifier().getContainerId() != null) {
            PSOIdentifier basePsoId = new PSOIdentifier();
            basePsoId.setID(getPsoIdentifier().getContainerId());
            query.setBasePsoID(basePsoId);
        }

        // the filter from the identifying attribute
        query.addQueryClause(identifyingAttribute.getFilter());

        return query;
    }

    /**
     * Get the alternate identifiers from the psp context.
     * 
     * @param context the psp context
     * @return the possibly empty list of alternate identifiers
     * @throws PspException if a psp error occurs
     */
    public List<AlternateIdentifier> getAlternateIdentifier(PspContext context) throws PspException {

        List<AlternateIdentifier> alternateIdentifiers = new ArrayList<AlternateIdentifier>();

        if (psoAlternateIdentifiers != null) {
            for (PsoAlternateIdentifier psoAlternateIdentifier : psoAlternateIdentifiers) {
                if (psoAlternateIdentifier.getRef() != null) {
                    List<PSOIdentifier> psoIdentifiers =
                            getPsoIdentifier().getPSOIdentifier(context, psoAlternateIdentifier.getRef());
                    for (PSOIdentifier psoIdentifier : psoIdentifiers) {
                        AlternateIdentifier alternateIdentifier = new AlternateIdentifier();
                        alternateIdentifier.setID(psoIdentifier.getID());
                        alternateIdentifier.setTargetID(psoIdentifier.getTargetID());
                        alternateIdentifiers.add(alternateIdentifier);
                    }
                }
            }
        }

        return alternateIdentifiers;
    }

    /**
     * Get the pso attribute with the given name.
     * 
     * @param name the name of the pso attribute
     * @return the PSO attribute definition with the given name or null
     */
    public PsoAttribute getPsoAttribute(String name) {
        for (PsoAttribute psoAttribute : psoAttributes) {
            if (psoAttribute.getName().equals(name)) {
                return psoAttribute;
            }
        }
        return null;
    }

    /**
     * Get the names of all pso attributes.
     * 
     * @return the names of all pso attributes
     */
    public Set<String> getAttributeNames() {

        Set<String> names = new LinkedHashSet<String>();

        for (PsoAttribute psoAttribute : psoAttributes) {
            names.add(psoAttribute.getName());
        }

        return names;
    }

    /**
     * Get the attribute definition ids to which all pso attributes refer to.
     * 
     * @return the attribute definition ids to which all pso attributes refer to
     */
    public Set<String> getAttributeSourceIds() {

        Set<String> ids = new LinkedHashSet<String>();

        for (PsoAttribute psoAttribute : psoAttributes) {
            ids.add(psoAttribute.getRef());
        }

        return ids;
    }

    /**
     * Gets the schema entity name.
     * 
     * @return the schema entity name
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the spmlv2 pso objects from the context.
     * 
     * @param context the psp context
     * @return the spmlv2 pso
     * @throws PspException if a psp error occurs
     * @throws Spml2Exception if an spml error occurs
     */
    public List<PSO> getPSO(PspContext context) throws PspException, Spml2Exception {

        String principalName = context.getProvisioningRequest().getId();

        LOG.debug("Pso '{}' - Get pso for '{}'", getId(), principalName);

        ArrayList<PSO> psos = new ArrayList<PSO>();

        // must have an identifier
        List<PSOIdentifier> psoIdentifiers = getPsoIdentifier().getPSOIdentifier(context);
        if (psoIdentifiers.isEmpty()) {
            LOG.debug("Pso '{}' - Unable to calculate pso identifier for '{}'", getId(), principalName);
            return psos;
        }

        // data
        Extensible data = null;
        ReturnData returnData = context.getProvisioningRequest().getReturnData();
        if (returnData.equals(ReturnData.DATA) || returnData.equals(ReturnData.EVERYTHING)) {
            for (PsoAttribute psoAttribute : getPsoAttributes()) {
                DSMLAttr dsmlAttr = psoAttribute.getAttribute(context);
                if (dsmlAttr != null) {
                    if (data == null) {
                        data = new Extensible();
                    }
                    data.addOpenContentElement(dsmlAttr);
                }
            }
        }

        // references
        List<Reference> references = null;
        if (returnData.equals(ReturnData.EVERYTHING)) {
            references = new ArrayList<Reference>();
            for (PsoReferences psoReference : getReferences()) {
                references.addAll(psoReference.getReferences(context));
            }
        }

        // alternate identifier
        List<AlternateIdentifier> alternateIdentifiers = getAlternateIdentifier(context);

        for (PSOIdentifier psoIdentifier : psoIdentifiers) {
            // pso
            PSO pso = new PSO();
            pso.setPsoID(psoIdentifier);

            pso.addOpenContentAttr(ENTITY_NAME_ATTRIBUTE, getId());

            if (data != null) {
                pso.setData(data);
            }

            if (references != null && !references.isEmpty()) {
                PSPUtil.setReferences(pso, references);
            }

            for (AlternateIdentifier alternateIdentifier : alternateIdentifiers) {
                pso.addOpenContentElement(alternateIdentifier);
            }

            psos.add(pso);
        }

        LOG.debug("Pso '{}' - Get pso for '{}' returned {} objects.",
                new Object[] {getId(), principalName, psos.size()});
        return psos;
    }

    /**
     * Gets the pso alternate identifiers.
     * 
     * @return the pso alternate identifiers
     */
    public List<PsoAlternateIdentifier> getPsoAlternateIdentifiers() {
        return psoAlternateIdentifiers;
    }

    /**
     * Gets the pso attributes.
     * 
     * @return the pso attributes
     */
    public List<PsoAttribute> getPsoAttributes() {
        return psoAttributes;
    }

    /**
     * Gets the pso identifier.
     * 
     * @return the pso identifier
     */
    public PsoIdentifier getPsoIdentifier() {
        return psoIdentifier;
    }

    /**
     * Get the identifying attribute.
     * 
     * @return the identifying attribute
     */
    public PsoIdentifyingAttribute getPsoIdentifyingAttribute() {
        return psoIdentifyingAttribute;
    }

    /**
     * Gets the names of all references.
     * 
     * @return the names of all references
     */
    public Set<String> getReferenceNames() {

        Set<String> names = new LinkedHashSet<String>();

        for (PsoReferences psoReference : psoReferences) {
            names.add(psoReference.getName());
        }

        return names;
    }

    /**
     * Gets the pso references.
     * 
     * @return the pso references.s
     */
    public List<PsoReferences> getReferences() {
        return psoReferences;
    }

    /**
     * Gets the pso references with the given name.
     * 
     * @param name the name of the pso references
     * @return the pso references with the given name or null
     */
    public PsoReferences getReferences(String name) {
        for (PsoReferences psoReference : psoReferences) {
            if (psoReference.getName().equals(name)) {
                return psoReference;
            }
        }
        return null;
    }

    /**
     * Gets the ids of attribute definitions to which all references refer to.
     * 
     * @return the ids of attribute definitions to which all references refer to.
     */
    public Set<String> getReferenceSourceIds() {

        Set<String> ids = new LinkedHashSet<String>();

        for (PsoReferences psoReference : psoReferences) {
            for (PsoReference psoReferenceDefinition : psoReference.getPsoReferences()) {
                ids.add(psoReferenceDefinition.getRef());
            }
        }

        return ids;
    }

    /**
     * Gets the ids of all attribute definitions referred to by this pso, including identifiers, alternate identifiers,
     * attributes, and references.
     * 
     * @param returnData includes the identifier and/or data and/or references
     * @return the ids of all attribute definitions referred to
     */
    public Set<String> getSourceIds(ReturnData returnData) {
        Set<String> set = new LinkedHashSet<String>();
        set.add(getPsoIdentifier().getRef());
        // TODO should alternate identifier be IDENTIFIER or DATA ... not sure
        if (getPsoAlternateIdentifiers() != null) {
            for (PsoAlternateIdentifier altIdDef : getPsoAlternateIdentifiers()) {
                if (altIdDef.getRef() != null) {
                    set.add(altIdDef.getRef());
                }
            }
        }
        if (returnData.equals(ReturnData.DATA) || returnData.equals(ReturnData.EVERYTHING)) {
            set.addAll(getAttributeSourceIds());
        }
        if (returnData.equals(ReturnData.EVERYTHING)) {
            set.addAll(getReferenceSourceIds());
        }
        return set;
    }

    /**
     * Gets whether or not this object should be included in bulk provisioning requests.
     * 
     * @return whether or not this object should be included in bulk provisioning requests
     */
    public boolean isAuthoritative() {
        return authoritative;
    }

    /**
     * Sets the id of the attribute definition which returns all source identifiers.
     * 
     * @param allSourceIdentifiersRef the id of the attribute definition which returns all source identifiers
     */
    public void setAllSourceIdentifiersRef(String allSourceIdentifiersRef) {
        this.allSourceIdentifiersRef = allSourceIdentifiersRef;
    }

    /**
     * Sets whether or not this object should be included in bulk provisioning requests.
     * 
     * @param authoritative whether or not this object should be included in bulk provisioning requests
     */
    public void setAuthoritative(boolean authoritative) {
        this.authoritative = authoritative;
    }

    /**
     * Sets the schema entity name.
     * 
     * @param id the schema entity name
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the alternate identifiers.
     * 
     * @param psoAlternateIdentifiers the alternate identifiers
     */
    public void setPsoAlternateIdentifiers(List<PsoAlternateIdentifier> psoAlternateIdentifiers) {
        this.psoAlternateIdentifiers = psoAlternateIdentifiers;
    }

    /**
     * Sets the pso attributes.
     * 
     * @param psoAttributes the pso attributes
     */
    public void setPsoAttributes(List<PsoAttribute> psoAttributes) {
        this.psoAttributes = psoAttributes;
    }

    /**
     * Sets the pso identifier.
     * 
     * @param psoIdentifier the pso identifier
     */
    public void setPsoIdentifier(PsoIdentifier psoIdentifier) {
        this.psoIdentifier = psoIdentifier;

    }

    /**
     * Sets the pso identifying attribute.
     * 
     * @param psoIdentifyingAttribute the pso identifying attribute
     */
    public void setPsoIdentifyingAttribute(PsoIdentifyingAttribute psoIdentifyingAttribute) {
        this.psoIdentifyingAttribute = psoIdentifyingAttribute;
    }

    /**
     * Sets the pso references.
     * 
     * @param psoReferences the pso references
     */
    public void setPsoReferences(List<PsoReferences> psoReferences) {
        this.psoReferences = psoReferences;
    }

    /** {@inheritDoc} */
    public String toString() {
        ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        toStringBuilder.append("id", id);
        toStringBuilder.append("authoritative", authoritative);
        return toStringBuilder.toString();
    }
}

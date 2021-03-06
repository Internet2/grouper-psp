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

package edu.internet2.middleware.psp.shibboleth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.openspml.v2.msg.spml.PSOIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.grouper.Stem;
import edu.internet2.middleware.grouper.util.GrouperUtil;
import edu.internet2.middleware.psp.ldap.LdapSpmlTarget;
import edu.internet2.middleware.psp.util.PSPUtil;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.BaseAttributeDefinition;

/**
 * An {@link AttributeDefinition} which returns a {@link PSOIdentifier} whose ID is an LDAP DN computed from
 * dependencies.
 */
public class LdapDnFromGrouperNamePSOIdentifierAttributeDefinition extends BaseAttributeDefinition {

    /** The logger. */
    private static Logger LOG = LoggerFactory.getLogger(LdapDnFromGrouperNamePSOIdentifierAttributeDefinition.class);

    /** The LDAP DN base. */
    private String baseDn;

    /** The Grouper base stem. */
    private String baseStem;

    /** The LDAP RDN attribute name. */
    private String rdnAttributeName;

    /** The LDAP RDN attribute name for stems. */
    private String stemRdnAttributeName;

    /** The Grouper DN structure. */
    private GroupDnStructure structure;

    /**
     * Get the LDAP DN base.
     * 
     * @return the base DN
     */
    public String getBaseDn() {
        return baseDn;
    }

    /**
     * Set the LDAP DN base.
     * 
     * @param base the base DN
     */
    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    /**
     * Get the base stem.
     * 
     * @return the base stem
     */
    public String getBaseStem() {
        return baseStem;
    }

    /**
     * Set the base stem.
     * 
     * @param base the base stem
     */
    public void setBaseStem(String baseStem) {
        this.baseStem = baseStem;
    }

    /**
     * Get the LDAP RDN attribute name.
     * 
     * @return the RDN attribute name
     */
    public String getRdnAttributeName() {
        return rdnAttributeName;
    }

    /**
     * Set the LDAP RDN attribute name.
     * 
     * @param rdnAttributeName the RDN attribute name
     */
    public void setRdnAttributeName(String rdnAttributeName) {
        this.rdnAttributeName = rdnAttributeName;
    }

    /**
     * Get the LDAP RDN attribute name for stems.
     * 
     * @return the RDN attribute name for stems
     */
    public String getStemRdnAttributeName() {
        return stemRdnAttributeName;
    }

    /**
     * Set the LDAP RDN attribute name for stems.
     * 
     * @param rdnAttributeName the RDN attribute name for stems
     */
    public void setStemRdnAttributeName(String stemRdnAttributeName) {
        this.stemRdnAttributeName = stemRdnAttributeName;
    }

    /**
     * Get the Grouper DN structure.
     * 
     * @return the DN structure
     */
    public GroupDnStructure getStructure() {
        return structure;
    }

    /**
     * Set the Grouper DN structure.
     * 
     * @param structure the DN structure
     */
    public void setStructure(GroupDnStructure structure) {
        this.structure = structure;
    }

    /**
     * Given a string of the form a:b:c, convert this to LDAP RDNs.
     * 
     * @param stemName the string of the form a:b:c
     * @return the list of LDAP RDNs
     * @throws AttributeResolutionException if an attribute resolution error occurs
     */
    public List<Rdn> getRdnsFromStemName(String stemName) throws AttributeResolutionException {

        ArrayList<Rdn> rdns = new ArrayList<Rdn>();

        StringTokenizer stemTokens = new StringTokenizer(stemName, Stem.DELIM);

        while (stemTokens.hasMoreTokens()) {
            try {
                rdns.add(new Rdn(getStemRdnAttributeName(), stemTokens.nextToken()));
            } catch (InvalidNameException e) {
                LOG.error("An error occurred creating an rdn.", e);
                throw new AttributeResolutionException("An error occurred creating an rdn.", e);
            }
        }

        return rdns;
    }

    /** {@inheritDoc} */
    protected BaseAttribute<PSOIdentifier> doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {

        String principalName = resolutionContext.getAttributeRequestContext().getPrincipalName();

        LOG.debug("Ldap dn from grouper name attribute definition '{}' - Resolve principal '{}'", getId(),
                principalName);

        BasicAttribute<PSOIdentifier> attribute = new BasicAttribute<PSOIdentifier>(getId());

        Collection<?> values = getValuesFromAllDependencies(resolutionContext, getSourceAttributeID());

        if (values.isEmpty()) {
            LOG.debug("Ldap dn from grouper name attribute definition '{}' - Resolve principal '{}' No dependencies",
                    getId(), principalName);
            return attribute;
        }

        for (Object value : values) {

            // the rdn attribute value
            String rdnAttributeValue = value.toString();

            // build RDNs
            List<Rdn> rdns = new ArrayList<Rdn>();
            try {
                // base
                LdapName baseDnLdapName = new LdapName(baseDn);
                rdns.addAll(baseDnLdapName.getRdns());

                if (getStructure().equals(GroupDnStructure.bushy)) {
                    String parentStemName = GrouperUtil.parentStemNameFromName(rdnAttributeValue, true);

                    // remove baseStem from name
                    if (parentStemName != null && !parentStemName.isEmpty()) {
                        if (baseStem != null && !baseStem.isEmpty()) {
                            if (parentStemName.startsWith(baseStem)) {
                                parentStemName = parentStemName.replaceFirst(baseStem, "");
                            }
                        }
                        rdns.addAll(getRdnsFromStemName(parentStemName));
                    }

                    String extension = GrouperUtil.extensionFromName(rdnAttributeValue);
                    rdns.add(new Rdn(rdnAttributeName, extension));
                } else {
                    rdns.add(new Rdn(rdnAttributeName, rdnAttributeValue));
                }

            } catch (InvalidNameException e) {
                LOG.error("Ldap dn from grouper name attribute definition '" + getId() + "' - Resolve principal '"
                        + principalName + "' Invalid name.", e);
                throw new AttributeResolutionException("Unable to resolve identifier.", e);
            }

            // dn
            LdapName dn = new LdapName(rdns);

            // pso id
            PSOIdentifier psoIdentifier = new PSOIdentifier();

            // TODO container id
            // PSOIdentifier containerID = new PSOIdentifier();
            // containerID.setID(base);
            // psoIdentifier.setContainerID(containerID);

            // canonicalize
            try {
                psoIdentifier.setID(LdapSpmlTarget.canonicalizeDn(dn.toString()));
            } catch (InvalidNameException e) {
                LOG.error("Ldap dn from grouper name attribute definition '" + getId() + "' - Resolve principal '"
                        + principalName + "' Unable to canonicalize identifier.", e);
                throw new AttributeResolutionException("Unable to canonicalize identifier.", e);
            }

            attribute.getValues().add(psoIdentifier);
        }

        if (LOG.isTraceEnabled()) {
            for (Object value : attribute.getValues()) {
                LOG.trace(
                        "Ldap dn from grouper name attribute definition '{}' - Resolve principal '{}' Returned value '{}'",
                        new Object[] {getId(), principalName, PSPUtil.getString(value),});
            }
        }

        return attribute;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {

    }
}

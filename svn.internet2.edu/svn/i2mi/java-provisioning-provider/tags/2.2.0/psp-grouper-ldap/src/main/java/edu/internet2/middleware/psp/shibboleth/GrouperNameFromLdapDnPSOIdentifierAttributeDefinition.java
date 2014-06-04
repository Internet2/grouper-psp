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

import java.util.Collection;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.openspml.v2.msg.spml.PSOIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.psp.util.PSPUtil;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.BaseAttributeDefinition;

/**
 * An {@link AttributeDefinition} which returns a {@link PSOIdentifier} whose ID is a Grouper name computed from LDAP DN
 * dependencies.
 */
public class GrouperNameFromLdapDnPSOIdentifierAttributeDefinition extends BaseAttributeDefinition {

    /** The logger. */
    private static Logger LOG = LoggerFactory.getLogger(GrouperNameFromLdapDnPSOIdentifierAttributeDefinition.class);

    /** The LDAP DN base. */
    private String baseDn;

    /** The Grouper base stem. */
    private String baseStem;

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

    /** {@inheritDoc} */
    protected BaseAttribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {

        String principalName = resolutionContext.getAttributeRequestContext().getPrincipalName();

        LOG.debug("Grouper name from ldap dn attribute definition '{}' - Resolve principal '{}'", getId(),
                principalName);

        BasicAttribute<PSOIdentifier> attribute = new BasicAttribute<PSOIdentifier>(getId());

        Collection<?> values = getValuesFromAllDependencies(resolutionContext, getSourceAttributeID());

        if (values.isEmpty()) {
            LOG.debug("Grouper name from ldap dn attribute definition '{}' - Resolve principal '{}' No dependencies",
                    getId(), principalName);
            return attribute;
        }

        for (Object value : values) {
            try {

                LdapName baseName = new LdapName(baseDn);

                LdapName dn = new LdapName(value.toString());

                if (dn.startsWith(baseName)) {
                    for (int i = 0; i < baseName.size(); i++) {
                        dn.remove(0);
                    }
                }

                StringBuffer buffer = new StringBuffer();
                for (Rdn rdn : dn.getRdns()) {
                    if (buffer.length() != 0) {
                        buffer.append(":");
                    }
                    buffer.append(rdn.getValue());
                }

                String name = buffer.toString();

                // remove baseStem from name
                if (name != null && !name.isEmpty()) {
                    if (baseStem != null && !baseStem.isEmpty()) {
                        if (name.startsWith(baseStem + ":")) {
                            name = name.replaceFirst(baseStem + ":", "");
                        }
                    }
                }

                PSOIdentifier psoIdentifier = new PSOIdentifier();
                psoIdentifier.setID(name);

                attribute.getValues().add(psoIdentifier);

            } catch (InvalidNameException e) {
                LOG.error("Grouper name from ldap dn attribute definition '" + getId() + "' - Resolve principal '"
                        + principalName + "' Invalid name.", e);
                throw new AttributeResolutionException("Unable to resolve identifier.", e);
            }
        }

        if (LOG.isTraceEnabled()) {
            for (Object value : attribute.getValues()) {
                LOG.trace(
                        "Grouper name from ldap dn attribute definition '{}' - Resolve principal '{}' Returned value '{}'",
                        new Object[] {getId(), principalName, PSPUtil.getString(value),});
            }
        }

        return attribute;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {

    }

}

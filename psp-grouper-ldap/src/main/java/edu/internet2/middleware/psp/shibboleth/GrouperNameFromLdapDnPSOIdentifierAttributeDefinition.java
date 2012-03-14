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
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.BaseAttributeDefinition;

/**
 *
 */
public class GrouperNameFromLdapDnPSOIdentifierAttributeDefinition extends BaseAttributeDefinition {

    /** The logger. */
    private static Logger LOG = LoggerFactory.getLogger(GrouperNameFromLdapDnPSOIdentifierAttributeDefinition.class);

    /** The LDAP DN base. */
    private String base;

    /**
     * Get the LDAP DN base.
     * 
     * @return the base DN
     */
    public String getBase() {
        return base;
    }

    /**
     * Set the LDAP DN base.
     * 
     * @param base the base DN
     */
    public void setBase(String base) {
        this.base = base;
    }

    /** {@inheritDoc} */
    protected BaseAttribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        String principalName = resolutionContext.getAttributeRequestContext().getPrincipalName();

        String msg =
                "Grouper Name PSOIdentifier attribute definition '" + getId() + "' - Resolve principal '"
                        + principalName + "'";
        LOG.debug("{}", msg);

        BasicAttribute<PSOIdentifier> attribute = new BasicAttribute<PSOIdentifier>(getId());

        Collection<?> values = getValuesFromAllDependencies(resolutionContext, getSourceAttributeID());

        LOG.debug("{} Dependency '{}'", msg, values);

        if (values.isEmpty()) {
            LOG.debug("{} No dependencies.", msg);
            return attribute;
        }

        for (Object value : values) {
            try {

                LdapName baseName = new LdapName(base);

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

                PSOIdentifier psoIdentifier = new PSOIdentifier();
                psoIdentifier.setID(buffer.toString());

                attribute.getValues().add(psoIdentifier);

            } catch (InvalidNameException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new AttributeResolutionException("TODO");
            }
        }

        if (LOG.isTraceEnabled()) {
            for (Object value : attribute.getValues()) {
                LOG.trace("{} value '{}'", msg, PSPUtil.getString(value));
            }
        }

        return attribute;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {

    }

}

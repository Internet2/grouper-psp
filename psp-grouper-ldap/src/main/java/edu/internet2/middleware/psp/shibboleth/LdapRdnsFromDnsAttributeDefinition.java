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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.psp.util.PSPUtil;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.BaseAttributeDefinition;

/** Returns RDNs from LDAP DN dependencies. */
public class LdapRdnsFromDnsAttributeDefinition extends BaseAttributeDefinition {

    /** The logger. */
    private static Logger LOG = LoggerFactory.getLogger(LdapRdnsFromDnsAttributeDefinition.class);

    /** {@inheritDoc} */
    protected BaseAttribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {

        String principalName = resolutionContext.getAttributeRequestContext().getPrincipalName();

        LOG.debug("Ldap rdns from dns attribute definition '{}' - Resolve principal '{}'", getId(), principalName);

        BasicAttribute<Rdn> attribute = new BasicAttribute<Rdn>(getId());

        Collection<?> values = getValuesFromAllDependencies(resolutionContext, getSourceAttributeID());

        if (values.isEmpty()) {
            LOG.debug("Ldap rdns from dns attribute definition '{}' - Resolve principal '{}' No dependencies", getId(),
                    principalName);
            return attribute;
        }

        for (Object value : values) {
            try {
                LdapName ldapName = new LdapName(value.toString());
                attribute.getValues().addAll(ldapName.getRdns());
            } catch (InvalidNameException e) {
                LOG.error("Ldap rdns from dns attribute definition '" + getId() + "' - Resolve principal '"
                        + principalName + "' Invalid name.", e);
                throw new AttributeResolutionException("Ldap rdns from dns attribute definition '" + getId()
                        + "' - Resolve principal '" + principalName + "' Invalid name.", e);
            }
        }

        if (LOG.isTraceEnabled()) {
            for (Object value : attribute.getValues()) {
                LOG.trace("Ldap rdns from dns attribute definition '{}' - Resolve principal '{}' value '{}'",
                        new Object[] {getId(), principalName, PSPUtil.getString(value)});
            }
        }

        return attribute;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {

    }

}

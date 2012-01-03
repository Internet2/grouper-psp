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
import java.util.Set;
import java.util.TreeSet;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.psp.util.PSPUtil;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.BaseAttributeDefinition;

/** Sort values as LDAP names. */
public class SortedLdapNameAttributeDefinition extends BaseAttributeDefinition {

    /** The logger. */
    private static Logger LOG = LoggerFactory.getLogger(SortedLdapNameAttributeDefinition.class);

    /** {@inheritDoc} */
    protected BaseAttribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {

        String principalName = resolutionContext.getAttributeRequestContext().getPrincipalName();

        LOG.debug("Sorted ldap name attribute definition '{}' - Resolve principal '{}'", getId(), principalName);

        BasicAttribute<String> attribute = new BasicAttribute<String>(getId());

        Collection<?> values = getValuesFromAllDependencies(resolutionContext, getSourceAttributeID());

        if (values.isEmpty()) {
            LOG.debug("Sorted ldap name attribute definition '{}' - Resolve principal '{}' No dependencies", getId(),
                    principalName);
            return attribute;
        }

        Set<LdapName> ldapNames = new TreeSet<LdapName>();

        for (Object value : values) {
            try {
                ldapNames.add(new LdapName(value.toString()));
            } catch (InvalidNameException e) {
                LOG.error("Sorted ldap name attribute definition '" + getId() + "' - Resolve principal '"
                        + principalName + "' Invalid name.", e);
                throw new AttributeResolutionException("Sorted ldap name attribute definition '" + getId()
                        + "' - Resolve principal '" + principalName + "' Invalid name.", e);
            }
        }

        for (LdapName ldapName : ldapNames) {
            attribute.getValues().add(ldapName.toString());
        }

        if (LOG.isTraceEnabled()) {
            for (Object value : attribute.getValues()) {
                LOG.trace("Sorted ldap name attribute definition '{}' - Resolve principal '{}' value '{}'",
                        new Object[] {getId(), principalName, PSPUtil.getString(value)});
            }
        }

        return attribute;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {

    }

}

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

package edu.internet2.middleware.psp.spring;

import edu.internet2.middleware.psp.shibboleth.GroupDnStructure;
import edu.internet2.middleware.psp.shibboleth.LdapDnFromGrouperNamePSOIdentifierAttributeDefinition;
import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.attributeDefinition.BaseAttributeDefinitionFactoryBean;

/** Spring bean factory that produces {@link LdapDnFromGrouperNamePSOIdentifierAttributeDefinition}s. */
public class LdapDnFromGrouperNamePSOIdentifierAttributeDefinitionFactoryBean extends
        BaseAttributeDefinitionFactoryBean {

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

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        LdapDnFromGrouperNamePSOIdentifierAttributeDefinition definition =
                new LdapDnFromGrouperNamePSOIdentifierAttributeDefinition();
        populateAttributeDefinition(definition);
        definition.setBaseDn(baseDn);
        definition.setBaseStem(baseStem);
        definition.setStructure(structure);
        definition.setRdnAttributeName(rdnAttributeName);
        definition.setStemRdnAttributeName(stemRdnAttributeName);
        return definition;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return LdapDnFromGrouperNamePSOIdentifierAttributeDefinition.class;
    }

}

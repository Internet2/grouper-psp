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

import edu.internet2.middleware.psp.shibboleth.GrouperNameFromLdapDnPSOIdentifierAttributeDefinition;
import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.attributeDefinition.BaseAttributeDefinitionFactoryBean;

/** Spring bean factory that produces {@link GrouperNameFromLdapDnPSOIdentifierAttributeDefinition}s. */
public class GrouperNameFromLdapDnPSOIdentifierAttributeDefinitionFactoryBean extends
    BaseAttributeDefinitionFactoryBean {

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
  protected Object createInstance() throws Exception {
    GrouperNameFromLdapDnPSOIdentifierAttributeDefinition definition = new GrouperNameFromLdapDnPSOIdentifierAttributeDefinition();
    populateAttributeDefinition(definition);
    definition.setBase(base);
    return definition;
  }

  /** {@inheritDoc} */
  public Class getObjectType() {
    return GrouperNameFromLdapDnPSOIdentifierAttributeDefinition.class;
  }

}

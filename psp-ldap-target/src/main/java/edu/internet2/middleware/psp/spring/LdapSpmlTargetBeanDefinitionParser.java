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

import javax.xml.namespace.QName;

import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import edu.internet2.middleware.psp.ldap.LdapSpmlTarget;

/** Spring bean definition parser for configuring a {@link LdapSpmlTarget}. */
public class LdapSpmlTargetBeanDefinitionParser extends BaseSpmlProviderBeanDefinitionParser {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(LdapSpmlTargetBeanDefinitionParser.class);

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(LdapSpmlTargetNamespaceHandler.NAMESPACE, "LdapTarget");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return LdapSpmlTarget.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element configElement, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(configElement, parserContext, builder);

        String ldapPoolId = configElement.getAttributeNS(null, "ldapPoolId");
        LOG.debug("Setting ldapPoolId to '{}'", ldapPoolId);
        builder.addPropertyValue("ldapPoolId", ldapPoolId);

        String ldapPoolIdSource = configElement.getAttributeNS(null, "ldapPoolIdSource");
        LOG.debug("Setting ldapPoolIdSource to '{}'", ldapPoolIdSource);
        builder.addPropertyValue("ldapPoolIdSource", ldapPoolIdSource);

        if (configElement.hasAttributeNS(null, "logLdif")) {
            Attr attr = configElement.getAttributeNodeNS(null, "logLdif");
            LOG.debug("Setting logLdif to '{}'", XMLHelper.getAttributeValueAsBoolean(attr));
            builder.addPropertyValue("logLdif", XMLHelper.getAttributeValueAsBoolean(attr));
        }
    }
}

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

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.psp.spml.config.Pso;
import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/** Spring bean definition parser for configuring an {@link Pso}. */
public class PsoBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(PspNamespaceHandler.NAMESPACE, "pso");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return Pso.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, builder);

        String id = element.getAttributeNS(null, "id");
        builder.addPropertyValue("id", id);

        String authoritative = element.getAttributeNS(null, "authoritative");
        builder.addPropertyValue("authoritative", authoritative);

        String allSourceIdentifiersRef = element.getAttributeNS(null, "allSourceIdentifiersRef");
        builder.addPropertyValue("allSourceIdentifiersRef", allSourceIdentifiersRef);

        Map<QName, List<Element>> configChildren = XMLHelper.getChildElements(element);

        builder.addPropertyValue(
                "psoIdentifier",
                SpringConfigurationUtils.parseInnerCustomElement(
                        configChildren.get(PsoIdentifierBeanDefinitionParser.TYPE_NAME).get(0), parserContext));

        if (configChildren.get(PsoIdentifyingAttributeBeanDefinitionParser.TYPE_NAME) != null) {
            builder.addPropertyValue("psoIdentifyingAttribute", SpringConfigurationUtils.parseInnerCustomElement(
                    configChildren.get(PsoIdentifyingAttributeBeanDefinitionParser.TYPE_NAME).get(0), parserContext));
        }

        builder.addPropertyValue(
                "psoAlternateIdentifiers",
                SpringConfigurationUtils.parseInnerCustomElements(
                        configChildren.get(PsoAlternateIdentifierBeanDefinitionParser.TYPE_NAME), parserContext));

        builder.addPropertyValue(
                "psoAttributes",
                SpringConfigurationUtils.parseInnerCustomElements(
                        configChildren.get(PsoAttributeBeanDefinitionParser.TYPE_NAME), parserContext));

        builder.addPropertyValue(
                "psoReferences",
                SpringConfigurationUtils.parseInnerCustomElements(
                        configChildren.get(PsoReferencesBeanDefinitionParser.TYPE_NAME), parserContext));
    }
}

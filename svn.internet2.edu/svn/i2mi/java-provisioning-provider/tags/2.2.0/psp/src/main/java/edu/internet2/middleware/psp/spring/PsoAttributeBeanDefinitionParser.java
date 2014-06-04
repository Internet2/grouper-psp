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
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.psp.spml.config.PsoAttribute;

/** Spring bean definition parser for configuring an {@link PsoAttribute}. */
public class PsoAttributeBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(PspNamespaceHandler.NAMESPACE, "attribute");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return PsoAttribute.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, builder);

        String name = element.getAttributeNS(null, "name");
        builder.addPropertyValue("name", name);

        String ref = element.getAttributeNS(null, "ref");
        if (ref.equals("")) {
            ref = name;
        }
        builder.addPropertyValue("ref", ref);

        boolean isMultiValued = false;
        if (element.hasAttributeNS(null, "isMultiValued")) {
            isMultiValued = XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null, "isMultiValued"));
        }
        builder.addPropertyValue("isMultiValued", isMultiValued);

        boolean replaceValues = false;
        if (element.hasAttributeNS(null, "replaceValues")) {
            replaceValues = XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null, "replaceValues"));
        }
        builder.addPropertyValue("replaceValues", replaceValues);

        boolean retainAll = false;
        if (element.hasAttributeNS(null, "retainAll")) {
            retainAll = XMLHelper.getAttributeValueAsBoolean(element.getAttributeNodeNS(null, "retainAll"));
        }
        builder.addPropertyValue("retainAll", retainAll);
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}

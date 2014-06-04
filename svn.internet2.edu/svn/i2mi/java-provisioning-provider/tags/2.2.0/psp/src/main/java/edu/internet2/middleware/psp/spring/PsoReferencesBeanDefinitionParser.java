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

import edu.internet2.middleware.psp.spml.config.PsoReferences;
import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/** Spring bean definition parser for configuring a {@link PsoReferences}. */
public class PsoReferencesBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(PspNamespaceHandler.NAMESPACE, "references");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return PsoReferences.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, builder);

        String name = element.getAttributeNS(null, "name");
        builder.addPropertyValue("name", name);

        if (element.hasAttributeNS(null, "emptyValue")) {
            String emptyValue = element.getAttributeNS(null, "emptyValue");
            builder.addPropertyValue("emptyValue", emptyValue);
        }

        if (element.hasAttributeNS(null, "caseSensitive")) {
            String caseSensitive = element.getAttributeNS(null, "caseSensitive");
            builder.addPropertyValue("caseSensitive", caseSensitive);
        }

        Map<QName, List<Element>> configChildren = XMLHelper.getChildElements(element);

        builder.addPropertyValue(
                "psoReferences",
                SpringConfigurationUtils.parseInnerCustomElements(
                        configChildren.get(PsoReferenceBeanDefinitionParser.TYPE_NAME), parserContext));
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}

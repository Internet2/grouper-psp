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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.psp.spml.config.PsoReference;

/** Spring bean definition parser for configuring a {@link PsoReference}. */
public class PsoReferenceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(PspNamespaceHandler.NAMESPACE, "reference");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return PsoReference.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, builder);

        String ref = element.getAttributeNS(null, "ref");
        builder.addPropertyValue("ref", ref);

        String toObject = element.getAttributeNS(null, "toObject");
        builder.addPropertyReference("toObject", toObject);

        String onNotFound = element.getAttributeNS(null, "onNotFound");
        builder.addPropertyValue("onNotFound", onNotFound);

        String multipleResults = element.getAttributeNS(null, "multipleResults");
        builder.addPropertyValue("multipleResults", multipleResults);
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}

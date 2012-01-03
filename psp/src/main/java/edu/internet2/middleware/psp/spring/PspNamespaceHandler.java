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

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/** Spring namespace handler for the PSP namespace. */
public class PspNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "http://grouper.internet2.edu/psp";

    /** {@inheritDoc} */
    public void init() {

        registerBeanDefinitionParser(PsoAlternateIdentifierBeanDefinitionParser.TYPE_NAME,
                new PsoAlternateIdentifierBeanDefinitionParser());

        registerBeanDefinitionParser(PspBeanDefinitionParser.TYPE_NAME, new PspBeanDefinitionParser());

        registerBeanDefinitionParser(PsoIdentifyingAttributeBeanDefinitionParser.TYPE_NAME,
                new PsoIdentifyingAttributeBeanDefinitionParser());

        registerBeanDefinitionParser(EmptyResourceBeanDefinitionParser.TYPE_NAME,
                new EmptyResourceBeanDefinitionParser());

        registerBeanDefinitionParser(PsoAttributeBeanDefinitionParser.TYPE_NAME,
                new PsoAttributeBeanDefinitionParser());

        registerBeanDefinitionParser(PsoBeanDefinitionParser.TYPE_NAME,
                new PsoBeanDefinitionParser());

        registerBeanDefinitionParser(PsoIdentifierBeanDefinitionParser.TYPE_NAME,
                new PsoIdentifierBeanDefinitionParser());

        registerBeanDefinitionParser(PsoReferenceBeanDefinitionParser.TYPE_NAME,
                new PsoReferenceBeanDefinitionParser());

        registerBeanDefinitionParser(PsoReferencesBeanDefinitionParser.TYPE_NAME,
                new PsoReferencesBeanDefinitionParser());

        registerBeanDefinitionParser(PsoIdentifierAttributeDefinitionBeanDefinitionParser.TYPE_NAME,
                new PsoIdentifierAttributeDefinitionBeanDefinitionParser());

        registerBeanDefinitionParser(PspServiceBeanDefinitionParser.TYPE_NAME, new PspServiceBeanDefinitionParser());

        registerBeanDefinitionParser(SimpleAttributeAuthorityBeanDefinitionParser.TYPE_NAME,
                new SimpleAttributeAuthorityBeanDefinitionParser());

    }
}

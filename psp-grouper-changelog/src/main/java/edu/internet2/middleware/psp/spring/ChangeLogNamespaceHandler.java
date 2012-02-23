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

/** Spring namespace handler for the Grouper change log namespace. */
public class ChangeLogNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "http://grouper.internet2.edu/psp-grouper-changelog";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(ChangeLogDataConnectorBeanDefinitionParser.TYPE_NAME,
                new ChangeLogDataConnectorBeanDefinitionParser());

        registerBeanDefinitionParser(ChangeLogAuditFilterBeanDefinitionParser.TYPE_NAME,
                new ChangeLogAuditFilterBeanDefinitionParser());

        registerBeanDefinitionParser(ChangeLogEntryFilterBeanDefinitionParser.TYPE_NAME,
                new ChangeLogEntryFilterBeanDefinitionParser());

        registerBeanDefinitionParser(ChangeLogExactAttributeFilterBeanDefinitionParser.TYPE_NAME,
                new ChangeLogExactAttributeFilterBeanDefinitionParser());

        registerBeanDefinitionParser(ChangeLogAttributeAssignTypeFilterBeanDefinitionParser.TYPE_NAME,
                new ChangeLogAttributeAssignTypeFilterBeanDefinitionParser());
    }
}

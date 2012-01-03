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

import edu.internet2.middleware.grouper.shibboleth.dataConnector.config.BaseGrouperDataConnectorFactoryBean;
import edu.internet2.middleware.psp.shibboleth.AllMemberSubjectIdsDataConnector;

/** Spring bean factory that produces {@link AllMemberSubjectIdsDataConnector}s. */
public class AllMemberSubjectIdsDataConnectorFactoryBean extends BaseGrouperDataConnectorFactoryBean {

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        AllMemberSubjectIdsDataConnector connector = new AllMemberSubjectIdsDataConnector();
        populateDataConnector(connector);
        return connector;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return AllMemberSubjectIdsDataConnector.class;
    }

}

/*
 *Licensed to the University Corporation for Advanced Internet Development, 
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

package edu.internet2.middleware.psp.shibboleth;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.textui.TestRunner;

import org.springframework.context.support.GenericApplicationContext;

import edu.internet2.middleware.grouper.Stem;
import edu.internet2.middleware.grouper.Stem.Scope;
import edu.internet2.middleware.grouper.StemFinder;
import edu.internet2.middleware.grouper.shibboleth.dataConnector.BaseDataConnectorTest;
import edu.internet2.middleware.psp.spml.request.BulkProvisioningRequest;

/** Tests for {@link StemSourceDataConnector}. */
public class AllStemNamesDataConnectorTest extends BaseDataConnectorTest {

    /** Path to attribute resolver configuration. */
    public static final String RESOLVER_CONFIG = "AllStemNamesDataConnectorTest-resolver.xml";

    /**
     * Constructor
     * 
     * @param name
     */
    public AllStemNamesDataConnectorTest(String name) {
        super(name);
    }

    /**
     * Run tests.
     * 
     * @param args
     */
    public static void main(String[] args) {
        TestRunner.run(AllStemNamesDataConnectorTest.class);
        // TestRunner.run(new AllStemNamesDataConnectorTest("testGetAllIdentifiers"));
    }

    /**
     * Test get all identifiers.
     */
    public void testGetAllIdentifiers() {

        try {
            GenericApplicationContext gContext = BaseDataConnectorTest.createSpringContext(RESOLVER_CONFIG);
            AllStemNamesDataConnector sdc = (AllStemNamesDataConnector) gContext.getBean("testAll");
            AttributeMap currentMap =
                    new AttributeMap(sdc.resolve(getShibContext(BulkProvisioningRequest.BULK_REQUEST_ID)));

            Stem etc = StemFinder.findByName(grouperSession, "etc", true);

            Set<Stem> etcChildStems = etc.getChildStems(Scope.SUB);

            List<String> values = new ArrayList<String>();
            values.add(parentStem.getName());
            values.add(childStem.getName());

            for (Stem stem : etcChildStems) {
                values.add(stem.getName());
            }

            values.add(etc.getName());

            AttributeMap correctMap = new AttributeMap();
            correctMap.setAttribute(AllStemNamesDataConnector.ALL_IDENTIFIERS_ATTRIBUTE_ID,
                    values.toArray(new String[] {}));
            assertEquals(correctMap, currentMap);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Test ignore principal names except for {@link BulkProvisioningRequest.BULK_REQUEST_ID}.
     */
    public void testIgnorePrincipalName() {

        try {
            GenericApplicationContext gContext = BaseDataConnectorTest.createSpringContext(RESOLVER_CONFIG);
            AllStemNamesDataConnector sdc = (AllStemNamesDataConnector) gContext.getBean("testAll");
            AttributeMap currentMap = new AttributeMap(sdc.resolve(getShibContext("principalName")));

            assertTrue(currentMap.getMap().isEmpty());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}

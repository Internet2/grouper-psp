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

package edu.internet2.middleware.psp;

import junit.textui.TestRunner;

import org.opensaml.util.resource.ResourceException;

import edu.internet2.middleware.psp.spml.request.BulkCalcRequest;
import edu.internet2.middleware.psp.spml.request.BulkCalcResponse;
import edu.internet2.middleware.psp.spml.request.BulkDiffRequest;
import edu.internet2.middleware.psp.spml.request.BulkDiffResponse;
import edu.internet2.middleware.psp.spml.request.BulkSyncRequest;
import edu.internet2.middleware.psp.spml.request.BulkSyncResponse;

/** Test provisioning Grouper to a multiple OpenLDAP directories. */
public class GrouperToOpenLdapMultipleTest extends BaseGrouperLdapTest {

    /**
     * Constructor.
     * 
     * @param string
     */
    public GrouperToOpenLdapMultipleTest(String name) {
        super(name);
    }

    /**
     * Run tests.
     * 
     * @param args
     */
    public static void main(String[] args) {
        // TestRunner.run(GrouperToLdapTest.class);
        TestRunner.run(new GrouperToOpenLdapMultipleTest("testBulkDiffBushyAdd"));
    }

    public void setUp() {

        super.setUp();

        setUpMailLocalAddressAttributeDef();

        setUpSeeAlsoAddressAttributeDef();

        try {
            setUpPSP();
        } catch (ResourceException e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        }

        try {
            setUpLdap("ldap1");
            setUpLdap("ldap2");
            setUpLdap("ldap3");
        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        }

        // setup test grouper objects
        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();

        // setup attribute framework attributes
        edu.getAttributeValueDelegate().assignValue("etc:attribute:seeAlso", "dc=example,dc=edu");
        groupA.getAttributeValueDelegate().assignValue("etc:attribute:mailLocalAddress", "mail@example.edu");
    }

    public void testBulkCalcBushyAdd() throws Exception {

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapMultipleTest.testBulkCalcBushyAdd.response.xml");
    }

    public void testBulkDiffBushyAdd() throws Exception {

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapMultipleTest.testBulkDiffBushyAdd.response.xml");
    }

    public void testBulkSyncBushyAdd() throws Exception {

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapMultipleTest.testBulkSyncBushyAdd.response.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleTest.testBulkSyncBushyAdd.after.ldif", "ldap1");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleTest.testBulkSyncBushyAdd.after.ldif", "ldap2");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleTest.testBulkSyncBushyAdd.after.3.ldif", "ldap3");
    }

}

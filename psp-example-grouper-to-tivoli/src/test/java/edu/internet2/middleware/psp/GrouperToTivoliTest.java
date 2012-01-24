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

/** Test provisioning Grouper to an OpenLDAP directory. */
public class GrouperToTivoliTest extends BaseGrouperLdapTest {

    public static void main(String[] args) {
        TestRunner.run(GrouperToTivoliTest.class);
        // TestRunner.run(new GrouperToTivoliTest("testBulkCalcFlatAddSubgroup"));      
    }

    /**
     * 
     * Constructor.
     * 
     * @param name
     */
    public GrouperToTivoliTest(String name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     * 
     * Setup the psp. Setup ldap. Setup the edu stem. Setup edu:groupA with test.subject.0 as member. Setup edu:groupB
     * with test.subject.1 as member.
     * 
     */
    public void setUp() {

        super.setUp();

        try {
            setUpPSP();
        } catch (ResourceException e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        }

        try {
            setUpLdap();
        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        }

        // setup test grouper objects
        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();
    }

    /**
     * Test calculating provisioning for the edu stem, groupA, and groupB.
     * 
     * @throws Exception
     */
    public void testBulkCalcFlatAdd() throws Exception {

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToTivoliTest.testBulkCalcFlatAdd.response.xml");
    }

    /**
     * Test diff'ing provisioning for the edu stem, groupA, and groupB.
     * 
     * @throws Exception
     */
    public void testBulkDiffFlatAdd() throws Exception {

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToTivoliTest.testBulkDiffFlatAdd.response.xml");
    }

    /**
     * Test provisioning the edu stem, groupA, and groupB.
     * 
     * @throws Exception
     */
    public void testBulkSyncFlatAdd() throws Exception {

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToTivoliTest.testBulkSyncFlatAdd.response.xml");
        verifyLdif(DATA_PATH + "GrouperToTivoliTest.testBulkSyncFlatAdd.after.ldif");
    }

    /**
     * Test calculating provisioning for the edu stem, groupA, and groupB where groupA is a member of groupB.
     * 
     * @throws Exception
     */
    public void testBulkCalcFlatAddSubgroup() throws Exception {

        groupB.addMember(groupA.toSubject());

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToTivoliTest.testBulkCalcFlatAddSubgroup.response.xml");
    }

    /**
     * Test diff'ing provisioning for the edu stem, groupA, and groupB where groupA is a member of groupB.
     * 
     * @throws Exception
     */
    public void testBulkDiffFlatAddSubgroup() throws Exception {

        groupB.addMember(groupA.toSubject());

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToTivoliTest.testBulkDiffFlatAddSubgroup.response.xml");
    }

    /**
     * Test provisioning the edu stem, groupA, and groupB where groupA is a member of groupB.
     * 
     * @throws Exception
     */
    public void testBulkSyncFlatAddSubgroup() throws Exception {

        groupB.addMember(groupA.toSubject());

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToTivoliTest.testBulkSyncFlatAddSubgroup.response.xml");
        verifyLdif(DATA_PATH + "GrouperToTivoliTest.testBulkSyncFlatAddSubgroup.after.ldif");
    }

    /**
     * Test calculating provisioning for the edu stem, groupA, and groupB as well as course group structure.
     * 
     * @throws Exception
     */
    public void testBulkCalcFlatAddChildStems() throws Exception {

        setUpCourseTest();

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToTivoliTest.testBulkCalcFlatAddChildStems.response.xml");
    }

    public void testBulkDiffFlatAddChildStems() throws Exception {

        setUpCourseTest();

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToTivoliTest.testBulkDiffFlatAddChildStems.response.xml");
    }

    public void testBulkSyncFlatAddChildStems() throws Exception {

        setUpCourseTest();

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToTivoliTest.testBulkSyncFlatAddChildStems.response.xml");
        verifyLdif(DATA_PATH + "GrouperToTivoliTest.testBulkSyncFlatAddChildStems.after.ldif");
    }

}

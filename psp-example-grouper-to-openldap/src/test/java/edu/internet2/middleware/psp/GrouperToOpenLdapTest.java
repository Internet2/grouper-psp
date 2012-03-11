/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.internet2.middleware.psp;

import junit.textui.TestRunner;

import org.opensaml.util.resource.ResourceException;
import org.openspml.v2.msg.spml.ReturnData;

import edu.internet2.middleware.grouper.SubjectFinder;
import edu.internet2.middleware.psp.spml.request.BulkCalcRequest;
import edu.internet2.middleware.psp.spml.request.BulkCalcResponse;
import edu.internet2.middleware.psp.spml.request.BulkDiffRequest;
import edu.internet2.middleware.psp.spml.request.BulkDiffResponse;
import edu.internet2.middleware.psp.spml.request.BulkSyncRequest;
import edu.internet2.middleware.psp.spml.request.BulkSyncResponse;
import edu.internet2.middleware.psp.spml.request.CalcRequest;
import edu.internet2.middleware.psp.spml.request.CalcResponse;
import edu.internet2.middleware.psp.spml.request.DiffRequest;
import edu.internet2.middleware.psp.spml.request.DiffResponse;
import edu.internet2.middleware.psp.spml.request.SyncRequest;
import edu.internet2.middleware.psp.spml.request.SyncResponse;
import edu.internet2.middleware.subject.provider.LdapSourceAdapter;

/** Test provisioning Grouper to an OpenLDAP directory. */
public class GrouperToOpenLdapTest extends BaseGrouperLdapTest {

    public static void main(String[] args) {
        // TestRunner.run(GrouperToLdapNotADTest.class);
        TestRunner.run(new GrouperToOpenLdapTest("testBulkSyncBushyAdd"));
    }

    /**
     * 
     * Constructor.
     * 
     * @param name
     */
    public GrouperToOpenLdapTest(String name) {
        super(name);
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
            setUpLdap();
        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        }

        // setup test grouper objects
        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();
        
        // setup attribute framework attributes
        edu.getAttributeValueDelegate().assignValue("etc:attribute:seeAlso", getLdapBaseDn());
        groupA.getAttributeValueDelegate().assignValue("etc:attribute:mailLocalAddress", "mail@example.edu");
    }

    public void testBulkCalcBushyAdd() throws Exception {

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkCalcBushyAdd.response.xml");
    }

    public void testBulkCalcBushyAddSubgroup() throws Exception {

        groupA.addMember(groupB.toSubject());

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkCalcBushyAddSubgroup.response.xml");
    }

    public void testBulkDiffBushyAdd() throws Exception {

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkDiffBushyAdd.response.xml");
    }

    public void testBulkDiffBushyAddSubgroup() throws Exception {

        groupA.addMember(groupB.toSubject());

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkDiffBushyAddSubgroup.response.xml");
    }

    public void testBulkSyncBushyAdd() throws Exception {

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkSyncBushyAdd.response.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapTest.testBulkSyncBushyAdd.after.ldif");
    }

    public void testBulkSyncBushyAddSubgroup() throws Exception {

        groupA.addMember(groupB.toSubject());

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkSyncBushyAddSubgroup.response.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapTest.testBulkSyncBushyAddSubgroup.after.ldif");
    }

}

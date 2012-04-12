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

import edu.internet2.middleware.grouper.GroupType;
import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.privs.AccessPrivilege;
import edu.internet2.middleware.psp.spml.request.BulkCalcRequest;
import edu.internet2.middleware.psp.spml.request.BulkCalcResponse;
import edu.internet2.middleware.psp.spml.request.BulkDiffRequest;
import edu.internet2.middleware.psp.spml.request.BulkDiffResponse;
import edu.internet2.middleware.psp.spml.request.BulkSyncRequest;
import edu.internet2.middleware.psp.spml.request.BulkSyncResponse;

/** Test provisioning Grouper for UChicago. */
public class GrouperUChicagoTest extends BaseGrouperLdapTest {

    public static void main(String[] args) {
        TestRunner.run(GrouperUChicagoTest.class);
        // TestRunner.run(new GrouperUChicagoTest("testBulkCalcBushyAdd"));
    }

    /**
     * 
     * Constructor.
     * 
     * @param name
     */
    public GrouperUChicagoTest(String name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     * 
     * Create custom attributes "Send To", "Type", and "Containers".
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
        // groupB = setUpGroupB();

        GroupType groupType = GroupType.createType(GrouperSession.startRootSession(), "groupType");
        groupType.addAttribute(GrouperSession.startRootSession(), "Send To", AccessPrivilege.VIEW,
                AccessPrivilege.UPDATE, false);
        groupType.addAttribute(GrouperSession.startRootSession(), "Type", AccessPrivilege.VIEW, AccessPrivilege.UPDATE,
                false);
        groupType.addAttribute(GrouperSession.startRootSession(), "Containers", AccessPrivilege.VIEW,
                AccessPrivilege.UPDATE, false);

        groupA.addType(groupType);
        groupA.setAttribute("Send To", "ldap");
        groupA.setAttribute("Type", "LDAPSync");
        // The 'Containers' attribute contains container DNs separated by a ';'.
        groupA.setAttribute("Containers", "ou=groups," + getLdapBaseDn() + ";ou=applications," + getLdapBaseDn());
    }

    public void testBulkCalcBushyAdd() throws Exception {

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkCalcBushyAdd.response.xml");
    }

    public void testBulkDiffBushyAdd() throws Exception {

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkDiffBushyAdd.response.xml");
    }

    public void testBulkSyncBushyAdd() throws Exception {

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperUChicagoTest.testBulkSyncBushyAdd.response.xml");
        verifyLdif(DATA_PATH + "GrouperUChicagoTest.testBulkSyncBushyAdd.after.ldif");
    }

}

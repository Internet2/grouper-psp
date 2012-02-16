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
import edu.internet2.middleware.grouper.changeLog.ChangeLogTempToEntity;
import edu.internet2.middleware.psp.grouper.PspChangeLogConsumer;

/** Tests for {@link PspChangeLogConsumer} */
public class GrouperToOpenLdapChangeLogTest extends BaseGrouperToLdapChangeLogTest {

    /**
     * Run tests.
     * 
     * @param args
     */
    public static void main(String[] args) {
        TestRunner.run(GrouperToOpenLdapChangeLogTest.class);
        // TestRunner.run(new GrouperToOpenLdapChangeLogTest("testMembershipAdd"));
    }

    /**
     * 
     * Constructor
     * 
     * @param name
     */
    public GrouperToOpenLdapChangeLogTest(String name) {
        super(name);
    }

    /**
     * Test provisioning resulting from the adding of a membership.
     * 
     * @throws Exception
     */
    public void testMembershipAdd() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAdd.before.ldif");
        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.subject0.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();

        clearChangeLog();

        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAdd.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAdd.after.ldif");
    }

    /**
     * Test provisioning resulting from the adding of a membership which has already been added on the provisioned
     * target.
     * 
     * @throws Exception
     */
    public void testMembershipAddAlreadyExists() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAddAlreadyExists.before.ldif");
        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.subject0.before.ldif");
        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAddAlreadyExists.subject1.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();

        clearChangeLog();

        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAddAlreadyExists.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAddAlreadyExists.after.ldif");
    }

    /**
     * Test provisioning resulting from the adding of a group membership.
     * 
     * @throws Exception
     */
    public void testMembershipAddGroup() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAddGroup.before.ldif");
        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.subject0.before.ldif");
        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.subject1.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();

        clearChangeLog();

        groupB.addMember(groupA.toSubject());

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAddGroup.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAddGroup.after.ldif");
    }

    /**
     * Test provisioning resulting from the deletion of a membership.
     * 
     * @throws Exception
     */
    public void testMembershipDelete() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipDelete.before.ldif");
        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.subject0.before.ldif");
        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAddAlreadyExists.subject1.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        clearChangeLog();

        groupA.deleteMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipDelete.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipDelete.after.ldif");
    }

    /**
     * Test provisioning resulting from the deletion of a membership which has already been deleted from the provisioned
     * target.
     * 
     * @throws Exception
     */
    public void testMembershipDeleteAlreadyDeleted() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipDeleteAlreadyDeleted.before.ldif");
        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.subject0.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        clearChangeLog();

        groupA.deleteMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipDeleteAlreadyDeleted.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipDeleteAlreadyDeleted.after.ldif");
    }

    /**
     * Test provisioning resulting from the deletion of a group membership.
     * 
     * @throws Exception
     */
    public void testMembershipDeleteGroup() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipDeleteGroup.before.ldif");
        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.subject0.before.ldif");
        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.subject1.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();
        groupB.addMember(groupA.toSubject());

        clearChangeLog();

        groupB.deleteMember(groupA.toSubject());

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipDeleteGroup.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipDeleteGroup.after.ldif");
    }

    /**
     * Test provisioning resulting from adding an attribute assignment value to a group.
     * 
     * @throws Exception
     */
    public void testGroupAttributeAssignValueAdd() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testGroupAttributeAssignValueAdd.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();

        clearChangeLog();

        groupA.getAttributeValueDelegate().assignValue("etc:attribute:mailLocalAddress", "mail@example.edu");

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testGroupAttributeAssignValueAdd.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testGroupAttributeAssignValueAdd.after.ldif");
    }

    /**
     * Test provisioning resulting from deleting an attribute assignment value from a group.
     * 
     * @throws Exception
     */
    public void testGroupAttributeAssignValueDelete() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testGroupAttributeAssignValueDelete.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupA.getAttributeValueDelegate().assignValue("etc:attribute:mailLocalAddress", "mail@example.edu");

        clearChangeLog();

        groupA.getAttributeValueDelegate().deleteValue("etc:attribute:mailLocalAddress", "mail@example.edu");

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testGroupAttributeAssignValueDelete.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testGroupAttributeAssignValueDelete.after.ldif");
    }

}

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
import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.Stem;
import edu.internet2.middleware.grouper.StemFinder;
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
        // TestRunner.run(GrouperToOpenLdapChangeLogTest.class);
        TestRunner.run(new GrouperToOpenLdapChangeLogTest("testMembershipDeleteGroup"));
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
     * Initialize the ldap directory.
     * 
     * {@inheritDoc}
     */
    public void setUp() {

        super.setUp();

        try {
            setUpLdap();
        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        }
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
        verifyLdif(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAdd.after.ldif");
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
     * Test provisioning resulting from the adding of a group membership.
     * 
     * @throws Exception
     */
    public void testMembershipAddGroupIgnore() throws Exception {

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();

        Stem etc = StemFinder.findByName(GrouperSession.staticGrouperSession(), "etc", true);
        Group ignoreGroup1 = etc.addChildGroup("ignoreGroup1", "ignoreGroup1");
        Group ignoreGroup2 = etc.addChildGroup("ignoreGroup2", "ignoreGroup2");

        clearChangeLog();

        ignoreGroup1.addMember(ignoreGroup2.toSubject());

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapChangeLogTest.testMembershipAddGroupIgnore.xml");
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

}

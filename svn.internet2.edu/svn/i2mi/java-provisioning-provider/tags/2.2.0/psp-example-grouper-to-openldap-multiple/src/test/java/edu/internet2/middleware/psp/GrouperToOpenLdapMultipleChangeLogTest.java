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
public class GrouperToOpenLdapMultipleChangeLogTest extends BaseGrouperToLdapChangeLogTest {

    /**
     * Run tests.
     * 
     * @param args
     */
    public static void main(String[] args) {
        // TestRunner.run(GrouperToOpenLdapChangeLogTest.class);
        TestRunner.run(new GrouperToOpenLdapMultipleChangeLogTest("testMembershipDeleteGroup"));
    }

    /**
     * 
     * Constructor
     * 
     * @param name
     */
    public GrouperToOpenLdapMultipleChangeLogTest(String name) {
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
            setUpLdap("ldap1");
            setUpLdap("ldap2");
            setUpLdap("ldap3");
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

        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAdd.before.ldif", "ldap1");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAdd.before.ldif", "ldap2");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAdd.before.ldif", "ldap3");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject0.before.ldif", "ldap1");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject0.before.ldif", "ldap2");

        edu = setUpEdu();
        groupA = setUpGroupA();

        clearChangeLog();

        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAdd.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAdd.after.ldif", "ldap1");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAdd.after.ldif", "ldap2");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAdd.after.3.ldif", "ldap3");
    }

    /**
     * Test provisioning resulting from the adding of a group membership.
     * 
     * @throws Exception
     */
    public void testMembershipAddGroup() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAddGroup.before.ldif", "ldap1");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAddGroup.before.ldif", "ldap2");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAddGroup.before.ldif", "ldap3");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject0.before.ldif", "ldap1");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject0.before.ldif", "ldap2");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject1.before.ldif", "ldap1");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject1.before.ldif", "ldap2");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();

        clearChangeLog();

        groupB.addMember(groupA.toSubject());

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAddGroup.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAddGroup.after.ldif", "ldap1");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAddGroup.after.ldif", "ldap2");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAddGroup.after.3.ldif", "ldap3");
    }

    /**
     * Test provisioning resulting from the deletion of a membership.
     * 
     * @throws Exception
     */
    public void testMembershipDelete() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDelete.before.ldif", "ldap1");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDelete.before.ldif", "ldap2");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDelete.before.ldif", "ldap3");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject0.before.ldif", "ldap1");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject0.before.ldif", "ldap2");
        loadLdif(DATA_PATH
                + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAddAlreadyExists.subject1.before.ldif", "ldap1");
        loadLdif(DATA_PATH
                + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipAddAlreadyExists.subject1.before.ldif", "ldap2");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        clearChangeLog();

        groupA.deleteMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDelete.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDelete.after.ldif", "ldap1");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDelete.after.ldif", "ldap2");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDelete.after.3.ldif", "ldap3");
    }

    /**
     * Test provisioning resulting from the deletion of a group membership.
     * 
     * @throws Exception
     */
    public void testMembershipDeleteGroup() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDeleteGroup.before.ldif", "ldap1");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDeleteGroup.before.ldif", "ldap2");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDeleteGroup.before.ldif", "ldap3");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject0.before.ldif", "ldap1");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject0.before.ldif", "ldap2");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject1.before.ldif", "ldap1");
        loadLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.subject1.before.ldif", "ldap2");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();
        groupB.addMember(groupA.toSubject());

        clearChangeLog();

        groupB.deleteMember(groupA.toSubject());

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDeleteGroup.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDeleteGroup.after.ldif", "ldap1");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDeleteGroup.after.ldif", "ldap2");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapMultipleChangeLogTest.testMembershipDeleteGroup.after.3.ldif", "ldap3");
    }

}

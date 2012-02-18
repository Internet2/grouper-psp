/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import edu.internet2.middleware.grouper.Stem;
import edu.internet2.middleware.grouper.changeLog.ChangeLogTempToEntity;
import edu.internet2.middleware.grouper.helper.StemHelper;
import edu.internet2.middleware.psp.grouper.PspChangeLogConsumer;
import edu.internet2.middleware.psp.helper.LdapTestHelper;

/** Tests for {@link PspChangeLogConsumer} */
public class GrouperToLdapChangeLogTest extends BaseGrouperToLdapChangeLogTest {

    /**
     * Run tests.
     * 
     * @param args
     */
    public static void main(String[] args) {
        // TestRunner.run(LdappcngConsumerTest.class);
        TestRunner.run(new GrouperToLdapChangeLogTest("testGroupWithMembersDeleteAlreadyDeleted"));
    }

    /**
     * 
     * Constructor
     * 
     * @param name
     */
    public GrouperToLdapChangeLogTest(String name) {
        super(name);
    }

    /**
     * Determine if subtree renames are supported by attempting to rename a non-empty ou.
     * 
     * This method deletes the test target ldap directory and re-adds the basic test ldif.
     * 
     * @return true if subtree renames are supported, false otherwise
     */
    public boolean isSubtreeRenameSupported() {

        try {
            LdapTestHelper.deleteChildren(getLdapBaseDn(), ldap);

            // create an ou with a child ou and rename it
            loadLdif(DATA_PATH + "GrouperLdapTest.before.ldif");
            loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemRenameChildGroups.before.ldif");
        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occured : " + e.getMessage());
        }

        String oldDn = "ou=child1,ou=edu,ou=groups," + getLdapBaseDn();
        String newDn = "ou=newChild1,ou=edu,ou=groups," + getLdapBaseDn();

        try {
            ldap.rename(oldDn, newDn);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // clean up and re add basic ldif
                LdapTestHelper.deleteChildren(getLdapBaseDn(), ldap);
                loadLdif(DATA_PATH + "GrouperLdapTest.before.ldif");
            } catch (Exception e) {
                e.printStackTrace();
                fail("An error occured : " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * Test provisioning resulting from the adding of a group. The change log events include a membership addition.
     * 
     * @throws Exception
     */
    public void testGroupAdd() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupAdd.before.ldif");

        edu = setUpEdu();

        clearChangeLog();

        groupA = setUpGroupA();

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupAdd.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupAdd.after.ldif");
    }

    /**
     * Test provisioning resulting from copying a group to a stem.
     * 
     * @throws Exception
     */
    public void testGroupCopy() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupCopy.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        Stem child = StemHelper.addChildStem(edu, "child", "Child");

        clearChangeLog();

        groupA.copy(child);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupCopy.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupCopy.after.ldif");
    }

    /**
     * Test provisioning resulting from deleting a group.
     * 
     * @throws Exception
     */
    public void testGroupDelete() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDelete.before.ldif");

        edu = setUpEdu();

        groupA = StemHelper.addChildGroup(edu, "groupA", "Group A");

        clearChangeLog();

        groupA.delete();

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDelete.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDelete.after.ldif");
    }

    /**
     * Test provisioning resulting from deleting a group which has already been deleted from the provisioned target.
     * 
     * @throws Exception
     */
    public void testGroupDeleteAlreadyDeleted() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDeleteAlreadyDeleted.before.ldif");

        edu = setUpEdu();

        groupA = StemHelper.addChildGroup(edu, "groupA", "Group A");

        clearChangeLog();

        groupA.delete();

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDeleteAlreadyDeleted.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDeleteAlreadyDeleted.after.ldif");
    }

    /**
     * Test provisioning resulting from adding a description to a group.
     * 
     * @throws Exception
     */
    public void testGroupDescriptionAdd() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDescriptionAdd.before.ldif");

        edu = setUpEdu();

        groupA = StemHelper.addChildGroup(edu, "groupA", "Group A");

        clearChangeLog();

        groupA.setDescription("descriptionA");
        groupA.store();

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDescriptionAdd.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDescriptionAdd.after.ldif");
        
        // some postgres timing issue ?
        Thread.sleep(1000);
    }

    /**
     * Test provisioning resulting from deleting a description from a group.
     * 
     * @throws Exception
     */
    public void testGroupDescriptionDelete() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDescriptionDelete.before.ldif");

        edu = setUpEdu();

        groupA = StemHelper.addChildGroup(edu, "groupA", "Group A");
        groupA.setDescription("descriptionA");
        groupA.store();

        clearChangeLog();

        groupA.setDescription("");
        groupA.store();

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDescriptionDelete.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDescriptionDelete.after.ldif");
    }

    /**
     * Test provisioning resulting from deleting a group with members which has already been deleted from the
     * provisioned target.
     * 
     * @throws Exception
     */
    public void testGroupWithMembersDeleteAlreadyDeleted() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDeleteAlreadyDeleted.before.ldif");

        edu = setUpEdu();

        groupA = StemHelper.addChildGroup(edu, "groupA", "Group A");
        groupA.addMember(LdapSubjectTestHelper.SUBJ0);
        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        clearChangeLog();

        groupA.deleteMember(LdapSubjectTestHelper.SUBJ0);
        groupA.deleteMember(LdapSubjectTestHelper.SUBJ1);
        groupA.delete();

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupWithMembersDeleteAlreadyDeleted.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupDeleteAlreadyDeleted.after.ldif");
    }

    /**
     * Test provisioning resulting from moving a group.
     * 
     * @throws Exception
     */
    public void testGroupMove() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupMove.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        Stem child = StemHelper.addChildStem(edu, "child", "Child");

        clearChangeLog();

        groupA.move(child);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupMove.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupMove.after.ldif");
    }

    /**
     * Test provisioning resulting from renaming a group.
     * 
     * @throws Exception
     */
    public void testGroupRename() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupRename.before.ldif");

        edu = setUpEdu();
        groupB = setUpGroupB();

        clearChangeLog();

        groupB.setExtension("newExtensionGroupB");
        groupB.store();

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupRename.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testGroupRename.after.ldif");
    }

    /**
     * Test provisioning resulting from the adding of a membership.
     * 
     * @throws Exception
     */
    public void testMembershipAdd() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipAdd.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();

        clearChangeLog();

        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipAdd.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipAdd.after.ldif");
    }

    /**
     * Test provisioning resulting from the adding of a membership which has already been added on the provisioned
     * target.
     * 
     * @throws Exception
     */
    public void testMembershipAddAlreadyExists() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipAddAlreadyExists.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();

        clearChangeLog();

        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipAddAlreadyExists.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipAddAlreadyExists.after.ldif");
    }

    /**
     * Test provisioning resulting from the adding of a group membership.
     * 
     * @throws Exception
     */
    public void testMembershipAddGroup() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipAddGroup.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();

        clearChangeLog();

        groupB.addMember(groupA.toSubject());

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipAddGroup.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipAddGroup.after.ldif");
    }

    /**
     * Test provisioning resulting from the deletion of a membership.
     * 
     * @throws Exception
     */
    public void testMembershipDelete() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipDelete.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        clearChangeLog();

        groupA.deleteMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipDelete.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipDelete.after.ldif");
    }

    /**
     * Test provisioning resulting from the deletion of a membership which has already been deleted from the provisioned
     * target.
     * 
     * @throws Exception
     */
    public void testMembershipDeleteAlreadyDeleted() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipDeleteAlreadyDeleted.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        clearChangeLog();

        groupA.deleteMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipDeleteAlreadyDeleted.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipDeleteAlreadyDeleted.after.ldif");
    }

    /**
     * Test provisioning resulting from the deletion of a group membership.
     * 
     * @throws Exception
     */
    public void testMembershipDeleteGroup() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipDeleteGroup.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();
        groupB.addMember(groupA.toSubject());

        clearChangeLog();

        groupB.deleteMember(groupA.toSubject());

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipDeleteGroup.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testMembershipDeleteGroup.after.ldif");
    }

    /**
     * Test provisioning resulting from the addition of a stem.
     * 
     * @throws Exception
     */
    public void testStemAdd() throws Exception {

        clearChangeLog();

        edu = setUpEdu();

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testStemAdd.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemAdd.after.ldif");
    }

    /**
     * Test provisioning resulting from copying a stem.
     * 
     * @throws Exception
     */
    public void testStemCopy() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemCopy.before.ldif");

        edu = setUpEdu();

        Stem child1 = StemHelper.addChildStem(edu, "child1", "Child 1");

        Group group1 = child1.addChildGroup("group1", "Group1");
        group1.addMember(LdapSubjectTestHelper.SUBJ0);

        Group group2 = child1.addChildGroup("group2", "Group2");
        group2.addMember(LdapSubjectTestHelper.SUBJ1);

        Stem child2 = StemHelper.addChildStem(edu, "child2", "Child 2");

        clearChangeLog();

        child1.copy(child2);

        ChangeLogTempToEntity.convertRecords();

        printChangeLogEntries();

        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testStemCopy.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemCopy.after.ldif");
    }

    /**
     * Test provisioning resulting from the deletion of a stem.
     * 
     * @throws Exception
     */
    public void testStemDelete() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemDelete.before.ldif");

        edu = setUpEdu();

        clearChangeLog();

        edu.delete();

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testStemDelete.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemDelete.after.ldif");
    }

    /**
     * Test provisioning resulting from the deletion of a stem which has already been deleted.
     * 
     * @throws Exception
     */
    public void testStemDeleteAlreadyDeleted() throws Exception {

        edu = setUpEdu();

        clearChangeLog();

        edu.delete();

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testStemDeleteAlreadyDeleted.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemDelete.after.ldif");
    }

    /**
     * Test provisioning resulting from moving a stem.
     * 
     * @throws Exception
     */
    public void testStemMove() throws Exception {

        boolean isSubtreeRenameSupported = isSubtreeRenameSupported();

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemMove.before.ldif");

        edu = setUpEdu();

        Stem child1 = StemHelper.addChildStem(edu, "child1", "Child 1");

        Group group1 = child1.addChildGroup("group1", "Group1");
        group1.addMember(LdapSubjectTestHelper.SUBJ0);

        Group group2 = child1.addChildGroup("group2", "Group2");
        group2.addMember(LdapSubjectTestHelper.SUBJ1);

        Stem child2 = StemHelper.addChildStem(edu, "child2", "Child 2");

        clearChangeLog();

        child1.move(child2);

        ChangeLogTempToEntity.convertRecords();

        runChangeLog();

        if (isSubtreeRenameSupported) {
            verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testStemMove.subtreeRenameSupported.xml");
            verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemMove.subtreeRenameSupported.after.ldif");
        } else {
            verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testStemMove.subtreeRenameNotSupported.xml");
            verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemMove.subtreeRenameNotSupported.after.ldif");
        }
    }

    /**
     * Test provisioning resulting from renaming a stem.
     * 
     * @throws Exception
     */
    public void testStemRename() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemRename.before.ldif");

        edu = setUpEdu();

        clearChangeLog();

        edu.setExtension("newEdu");
        edu.store();

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testStemRename.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemRename.after.ldif");
    }

    /**
     * Test provisioning resulting from renaming a stem with child groups.
     * 
     * @throws Exception
     */
    public void testStemRenameChildGroups() throws Exception {

        boolean isSubtreeRenameSupported = isSubtreeRenameSupported();

        loadLdif(DATA_PATH + "GrouperToLdapChangeLogTest.testStemRenameChildGroups.before.ldif");

        edu = setUpEdu();

        Stem child1 = StemHelper.addChildStem(edu, "child1", "Child 1");

        Group group1 = child1.addChildGroup("group1", "Group1");
        group1.addMember(LdapSubjectTestHelper.SUBJ0);

        Group group2 = child1.addChildGroup("group2", "Group2");
        group2.addMember(LdapSubjectTestHelper.SUBJ1);

        Stem child2 = StemHelper.addChildStem(child1, "child2", "Child 2");

        Group group3 = child2.addChildGroup("group3", "Group3");
        group3.addMember(LdapSubjectTestHelper.SUBJ2);

        clearChangeLog();

        child1.setExtension("newChild1");
        child1.store();

        ChangeLogTempToEntity.convertRecords();

        printChangeLogEntries();

        runChangeLog();

        if (isSubtreeRenameSupported) {
            verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testStemRenameChildGroups.subtreeRenameSupported.xml");
            verifyLdif(DATA_PATH
                    + "GrouperToLdapChangeLogTest.testStemRenameChildGroups.subtreeRenameSupported.after.ldif");
        } else {
            verifySpml(DATA_PATH + "GrouperToLdapChangeLogTest.testStemRenameChildGroups.subtreeRenameNotSupported.xml");
            verifyLdif(DATA_PATH
                    + "GrouperToLdapChangeLogTest.testStemRenameChildGroups.subtreeRenameNotSupported.after.ldif");
        }
    }

}

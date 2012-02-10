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
public class GrouperToTivoliChangeLogTest extends BaseGrouperToLdapChangeLogTest {

    /**
     * Run tests.
     * 
     * @param args
     */
    public static void main(String[] args) {
        // TestRunner.run(GrouperToTivoliChangeLogTest.class);
        TestRunner.run(new GrouperToTivoliChangeLogTest("testChangeLogMembershipGroupName"));
    }

    /**
     * 
     * Constructor
     * 
     * @param name
     */
    public GrouperToTivoliChangeLogTest(String name) {
        super(name);
    }

    /**
     * Test provisioning resulting from the adding of a membership.
     * 
     * @throws Exception
     */
    public void testMembershipAdd() throws Exception {

        loadLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipAdd.before.ldif");
        loadLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.subject0.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();

        clearChangeLog();

        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipAdd.xml");
        verifyLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipAdd.after.ldif");
    }

    /**
     * Test provisioning resulting from the adding of a group membership.
     * 
     * @throws Exception
     */
    public void testMembershipAddGroup() throws Exception {

        loadLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipAddGroup.before.ldif");
        loadLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.subject0.before.ldif");
        loadLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.subject1.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();

        clearChangeLog();

        groupB.addMember(groupA.toSubject());

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipAddGroup.xml");
        verifyLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipAddGroup.after.ldif");
    }

    /**
     * Test provisioning resulting from the deletion of a membership.
     * 
     * @throws Exception
     */
    public void testMembershipDelete() throws Exception {

        loadLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipDelete.before.ldif");
        loadLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.subject0.before.ldif");
        loadLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipDelete.subject1.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupA.addMember(LdapSubjectTestHelper.SUBJ1);

        clearChangeLog();

        groupA.deleteMember(LdapSubjectTestHelper.SUBJ1);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipDelete.xml");
        verifyLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipDelete.after.ldif");
    }

    /**
     * Test provisioning resulting from the deletion of a group membership.
     * 
     * @throws Exception
     */
    public void testMembershipDeleteGroup() throws Exception {

        loadLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipDeleteGroup.before.ldif");
        loadLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.subject0.before.ldif");
        loadLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.subject1.before.ldif");

        edu = setUpEdu();
        groupA = setUpGroupA();
        groupB = setUpGroupB();
        groupB.addMember(groupA.toSubject());

        clearChangeLog();

        groupB.deleteMember(groupA.toSubject());

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipDeleteGroup.xml");
        verifyLdif(DATA_PATH + "GrouperToTivoliChangeLogTest.testMembershipDeleteGroup.after.ldif");
    }

    /**
     * Test provisioning resulting from the adding of a membership to a group outside of the stem to be provisioned.
     * 
     * @throws Exception
     */
    public void testChangeLogMembershipGroupName() throws Exception {

        clearChangeLog();

        Stem etc = StemFinder.findByName(GrouperSession.staticGrouperSession(), "etc", true);        
        Group group = etc.addChildGroup("childGroup", "childGroup");
        group.addMember(LdapSubjectTestHelper.SUBJ0);

        ChangeLogTempToEntity.convertRecords();
        runChangeLog();

        verifySpml(DATA_PATH + "GrouperToTivoliChangeLogTest.testChangeLogMembershipGroupName.xml");
    }

}

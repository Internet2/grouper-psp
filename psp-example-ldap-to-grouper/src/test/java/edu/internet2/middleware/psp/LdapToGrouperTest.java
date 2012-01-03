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
import org.openspml.v2.msg.spml.ReturnData;

import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.GroupFinder;
import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.Stem;
import edu.internet2.middleware.grouper.Stem.Scope;
import edu.internet2.middleware.grouper.StemFinder;
import edu.internet2.middleware.grouper.helper.StemHelper;
import edu.internet2.middleware.grouper.helper.SubjectTestHelper;
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
import edu.vt.middleware.ldap.bean.LdapBeanProvider;
import edu.vt.middleware.ldap.bean.SortedLdapBeanFactory;

/** Test provisioning an LDAP directory to Grouper. */
public class LdapToGrouperTest extends BaseGrouperLdapTest {

    /**
     * Constructor.
     * 
     * @param string
     */
    public LdapToGrouperTest(String name) {
        super(name);

        // Sort ldap attributes and values. See http://code.google.com/p/vt-middleware/wiki/vtldapSearching
        LdapBeanProvider.setLdapBeanFactory(new SortedLdapBeanFactory());
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TestRunner.run(LdapToGrouperTest.class);
        TestRunner.run(new LdapToGrouperTest("testBulkCalcBushy"));
    }

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

        GrouperSession.startRootSession();
        Stem root = StemHelper.findRootStem(GrouperSession.staticGrouperSession());
        edu = StemHelper.addChildStem(root, "edu", "edu");
    }

    public void testBulkCalcBushy() throws Exception {

        loadLdif(DATA_PATH + "LdapToGrouperTest.testBulkBushy.before.ldif");

        // to be deleted
        edu.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteParent = edu.addChildStem("stemDeleteParent", "stemDeleteParent");
        stemDeleteParent.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteChild = stemDeleteParent.addChildStem("stemDeleteChild", "stemDeleteChild");
        stemDeleteChild.addChildGroup("groupDelete", "groupDelete");

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapToGrouperTest.testBulkCalcBushy.response.xml");
    }

    public void testBulkCalcBushyData() throws Exception {

        loadLdif(DATA_PATH + "LdapToGrouperTest.testBulkBushy.before.ldif");

        // to be deleted
        edu.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteParent = edu.addChildStem("stemDeleteParent", "stemDeleteParent");
        stemDeleteParent.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteChild = stemDeleteParent.addChildStem("stemDeleteChild", "stemDeleteChild");
        stemDeleteChild.addChildGroup("groupDelete", "groupDelete");

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setReturnData(ReturnData.DATA);
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapToGrouperTest.testBulkCalcBushyData.response.xml");
    }

    public void testBulkDiffBushy() throws Exception {

        loadLdif(DATA_PATH + "LdapToGrouperTest.testBulkBushy.before.ldif");

        // to be deleted
        edu.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteParent = edu.addChildStem("stemDeleteParent", "stemDeleteParent");
        stemDeleteParent.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteChild = stemDeleteParent.addChildStem("stemDeleteChild", "stemDeleteChild");
        stemDeleteChild.addChildGroup("groupDelete", "groupDelete");

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapToGrouperTest.testBulkDiffBushy.response.xml");
    }

    public void testBulkDiffBushyData() throws Exception {

        loadLdif(DATA_PATH + "LdapToGrouperTest.testBulkBushy.before.ldif");

        // to be deleted
        edu.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteParent = edu.addChildStem("stemDeleteParent", "stemDeleteParent");
        stemDeleteParent.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteChild = stemDeleteParent.addChildStem("stemDeleteChild", "stemDeleteChild");
        stemDeleteChild.addChildGroup("groupDelete", "groupDelete");

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setReturnData(ReturnData.DATA);
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapToGrouperTest.testBulkDiffBushyData.response.xml");
    }

    public void testBulkDiffBushyPartial() throws Exception {

        loadLdif(DATA_PATH + "LdapToGrouperTest.testBulkBushy.before.ldif");

        // to be deleted
        Group eduGroupDelete = edu.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteParent = edu.addChildStem("stemDeleteParent", "stemDeleteParent");
        Group parentGroupDelete = stemDeleteParent.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteChild = stemDeleteParent.addChildStem("stemDeleteChild", "stemDeleteChild");
        Group stemGroupDelete = stemDeleteChild.addChildGroup("groupDelete", "groupDelete");

        // first sync data
        BulkSyncRequest requestSyncData = new BulkSyncRequest();
        requestSyncData.setRequestID("REQUESTID_TEST");
        requestSyncData.setReturnData(ReturnData.DATA);
        BulkSyncResponse responseSyncData = psp.execute(requestSyncData);

        verifySpml(responseSyncData, DATA_PATH + "LdapToGrouperTest.testBulkDiffBushyPartialData.response.xml");

        edu = StemFinder.findByName(GrouperSession.staticGrouperSession(), "edu", true);
        assertEquals("edu", edu.getDisplayExtension());
        assertEquals(3, edu.getChildStems(Scope.SUB).size());

        Stem courses = StemFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses", true);
        assertEquals("courses", courses.getDisplayExtension());
        assertEquals(2, courses.getChildStems(Scope.SUB).size());
        assertEquals(4, courses.getChildGroups(Scope.SUB).size());

        Stem fall = StemFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:fall", true);
        assertEquals("fall", fall.getDisplayExtension());
        assertEquals(0, fall.getChildStems(Scope.SUB).size());
        assertEquals(2, fall.getChildGroups(Scope.SUB).size());

        Stem spring = StemFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:spring", true);
        assertEquals("spring", spring.getDisplayExtension());
        assertEquals(0, spring.getChildStems(Scope.SUB).size());
        assertEquals(2, spring.getChildGroups(Scope.SUB).size());

        Group groupA = GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:groupA", true);
        assertEquals("", groupA.getDescription());
        assertEquals(0, groupA.getMembers().size());

        Group groupB = GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:groupB", true);
        assertEquals("groupB", groupB.getDisplayExtension());
        assertEquals("descriptionB", groupB.getDescription());
        assertEquals(0, groupB.getMembers().size());

        Group fallCourseA =
                GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:fall:courseA", true);
        assertEquals("courseA", fallCourseA.getDisplayExtension());
        assertEquals(0, fallCourseA.getMembers().size());

        Group springCourseA =
                GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:spring:courseA", true);
        assertEquals("courseA", springCourseA.getDisplayExtension());
        assertEquals(0, springCourseA.getMembers().size());

        Group fallCourseB =
                GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:fall:courseB", true);
        assertEquals("courseB", fallCourseB.getDisplayExtension());
        assertEquals(0, fallCourseB.getMembers().size());

        Group springCourseB =
                GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:spring:courseB", true);
        assertEquals("courseB", springCourseB.getDisplayExtension());
        assertEquals(0, springCourseB.getMembers().size());

        Group groupC = GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:groupC", true);
        assertEquals("", groupC.getDescription());
        assertEquals(0, groupC.getMembers().size());

        Group groupD = GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:groupD", true);
        assertEquals("", groupC.getDescription());
        assertEquals(0, groupD.getMembers().size());

        assertNull(StemFinder.findByName(GrouperSession.staticGrouperSession(), stemDeleteParent.getName(), false));
        assertNull(StemFinder.findByName(GrouperSession.staticGrouperSession(), stemDeleteChild.getName(), false));
        assertNull(GroupFinder.findByName(GrouperSession.staticGrouperSession(), eduGroupDelete.getName(), false));
        assertNull(GroupFinder.findByName(GrouperSession.staticGrouperSession(), parentGroupDelete.getName(), false));
        assertNull(GroupFinder.findByName(GrouperSession.staticGrouperSession(), stemGroupDelete.getName(), false));

        // then diff everything
        BulkDiffRequest diffRequest = new BulkDiffRequest();
        diffRequest.setRequestID("REQUESTID_TEST");
        diffRequest.setReturnData(ReturnData.EVERYTHING);
        BulkDiffResponse diffResponse = psp.execute(diffRequest);

        verifySpml(diffResponse, DATA_PATH + "LdapToGrouperTest.testBulkDiffBushyPartialEverything.response.xml");
    }

    public void testBulkSyncBushy() throws Exception {

        loadLdif(DATA_PATH + "LdapToGrouperTest.testBulkBushy.before.ldif");

        // to be deleted
        Group eduGroupDelete = edu.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteParent = edu.addChildStem("stemDeleteParent", "stemDeleteParent");
        Group parentGroupDelete = stemDeleteParent.addChildGroup("groupDelete", "groupDelete");
        Stem stemDeleteChild = stemDeleteParent.addChildStem("stemDeleteChild", "stemDeleteChild");
        Group stemGroupDelete = stemDeleteChild.addChildGroup("groupDelete", "groupDelete");

        // first sync data
        BulkSyncRequest requestSyncData = new BulkSyncRequest();
        requestSyncData.setRequestID("REQUESTID_TEST");
        requestSyncData.setReturnData(ReturnData.DATA);
        BulkSyncResponse responseSyncData = psp.execute(requestSyncData);

        verifySpml(responseSyncData, DATA_PATH + "LdapToGrouperTest.testBulkSyncBushyData.response.xml");

        edu = StemFinder.findByName(GrouperSession.staticGrouperSession(), "edu", true);
        assertEquals("edu", edu.getDisplayExtension());
        assertEquals(3, edu.getChildStems(Scope.SUB).size());

        Stem courses = StemFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses", true);
        assertEquals("courses", courses.getDisplayExtension());
        assertEquals(2, courses.getChildStems(Scope.SUB).size());
        assertEquals(4, courses.getChildGroups(Scope.SUB).size());

        Stem fall = StemFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:fall", true);
        assertEquals("fall", fall.getDisplayExtension());
        assertEquals(0, fall.getChildStems(Scope.SUB).size());
        assertEquals(2, fall.getChildGroups(Scope.SUB).size());

        Stem spring = StemFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:spring", true);
        assertEquals("spring", spring.getDisplayExtension());
        assertEquals(0, spring.getChildStems(Scope.SUB).size());
        assertEquals(2, spring.getChildGroups(Scope.SUB).size());

        Group groupA = GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:groupA", true);
        assertEquals("", groupA.getDescription());
        assertEquals(0, groupA.getMembers().size());

        Group groupB = GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:groupB", true);
        assertEquals("groupB", groupB.getDisplayExtension());
        assertEquals("descriptionB", groupB.getDescription());
        assertEquals(0, groupB.getMembers().size());

        Group fallCourseA =
                GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:fall:courseA", true);
        assertEquals("courseA", fallCourseA.getDisplayExtension());
        assertEquals(0, fallCourseA.getMembers().size());

        Group springCourseA =
                GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:spring:courseA", true);
        assertEquals("courseA", springCourseA.getDisplayExtension());
        assertEquals(0, springCourseA.getMembers().size());

        Group fallCourseB =
                GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:fall:courseB", true);
        assertEquals("courseB", fallCourseB.getDisplayExtension());
        assertEquals(0, fallCourseB.getMembers().size());

        Group springCourseB =
                GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:courses:spring:courseB", true);
        assertEquals("courseB", springCourseB.getDisplayExtension());
        assertEquals(0, springCourseB.getMembers().size());

        Group groupC = GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:groupC", true);
        assertEquals("", groupC.getDescription());
        assertEquals(0, groupC.getMembers().size());

        Group groupD = GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:groupD", true);
        assertEquals("", groupC.getDescription());
        assertEquals(0, groupD.getMembers().size());

        assertNull(StemFinder.findByName(GrouperSession.staticGrouperSession(), stemDeleteParent.getName(), false));
        assertNull(StemFinder.findByName(GrouperSession.staticGrouperSession(), stemDeleteChild.getName(), false));
        assertNull(GroupFinder.findByName(GrouperSession.staticGrouperSession(), eduGroupDelete.getName(), false));
        assertNull(GroupFinder.findByName(GrouperSession.staticGrouperSession(), parentGroupDelete.getName(), false));
        assertNull(GroupFinder.findByName(GrouperSession.staticGrouperSession(), stemGroupDelete.getName(), false));

        // then sync everything
        BulkSyncRequest requestSyncEverything = new BulkSyncRequest();
        requestSyncEverything.setRequestID("REQUESTID_TEST");
        requestSyncEverything.setReturnData(ReturnData.EVERYTHING);
        BulkSyncResponse responseSyncEverything = psp.execute(requestSyncEverything);

        verifySpml(responseSyncEverything, DATA_PATH + "LdapToGrouperTest.testBulkSyncBushyEverything.response.xml");

        assertEquals(1, groupA.getMembers().size());
        assertTrue(groupA.hasImmediateMember(SubjectTestHelper.SUBJ0));

        assertEquals(3, groupB.getMembers().size());
        assertTrue(groupB.hasEffectiveMember(SubjectTestHelper.SUBJ0));
        assertTrue(groupB.hasImmediateMember(groupA.toSubject()));
        assertTrue(groupB.hasImmediateMember(SubjectTestHelper.SUBJ1));

        assertEquals(2, fallCourseA.getMembers().size());
        assertTrue(fallCourseA.hasImmediateMember(SubjectTestHelper.SUBJ0));
        assertTrue(fallCourseA.hasImmediateMember(SubjectTestHelper.SUBJ1));

        assertEquals(2, springCourseA.getMembers().size());
        assertTrue(springCourseA.hasImmediateMember(SubjectTestHelper.SUBJ2));
        assertTrue(springCourseA.hasImmediateMember(SubjectTestHelper.SUBJ3));

        assertEquals(1, fallCourseB.getMembers().size());
        assertTrue(fallCourseB.hasImmediateMember(SubjectTestHelper.SUBJ1));

        assertEquals(1, springCourseB.getMembers().size());
        assertTrue(springCourseB.hasImmediateMember(SubjectTestHelper.SUBJ3));

        assertEquals(2, groupC.getMembers().size());
        assertTrue(groupC.hasEffectiveMember(groupC.toSubject()));
        assertTrue(groupC.hasImmediateMember(groupD.toSubject()));

        assertEquals(2, groupD.getMembers().size());
        assertTrue(groupD.hasEffectiveMember(groupD.toSubject()));
        assertTrue(groupD.hasImmediateMember(groupC.toSubject()));
    }

    public void testRenameGroupCalc() throws Exception {
        loadLdif(DATA_PATH + "LdapToGrouperTest.testRenameGroup.before.ldif");

        edu.addChildGroup("groupOLD", "groupOLD");

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId("cn=groupNEW,ou=edu,ou=testgroups," + getLdapBaseDn());
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapToGrouperTest.testRenameGroupCalc.response.xml");
    }

    public void testRenameGroupDiff() throws Exception {
        loadLdif(DATA_PATH + "LdapToGrouperTest.testRenameGroup.before.ldif");

        edu.addChildGroup("groupOLD", "groupOLD");

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId("cn=groupNEW,ou=edu,ou=testgroups," + getLdapBaseDn());
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapToGrouperTest.testRenameGroupDiff.response.xml");
    }

    public void testRenameGroupSync() throws Exception {
        loadLdif(DATA_PATH + "LdapToGrouperTest.testRenameGroup.before.ldif");

        edu.addChildGroup("groupOLD", "groupOLD");

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId("cn=groupNEW,ou=edu,ou=testgroups," + getLdapBaseDn());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapToGrouperTest.testRenameGroupSync.response.xml");

        // assert equality since the group will be known by both names
        Group groupOLD = GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:groupOLD", true);
        Group groupNEW = GroupFinder.findByName(GrouperSession.staticGrouperSession(), "edu:groupNEW", true);
        assertEquals(groupOLD, groupNEW);
    }

    public void testRenameStemCalc() throws Exception {
        loadLdif(DATA_PATH + "LdapToGrouperTest.testRenameStem.before.ldif");

        Stem newStem = edu.addChildStem("stemOLD", "stemOLD");
        newStem.addChildGroup("group", "group");

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId("ou=stemNEW,ou=edu,ou=testgroups," + getLdapBaseDn());
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapToGrouperTest.testRenameStemCalc.response.xml");
    }

    public void testRenameStemDiff() throws Exception {
        loadLdif(DATA_PATH + "LdapToGrouperTest.testRenameStem.before.ldif");

        Stem newStem = edu.addChildStem("stemOLD", "stemOLD");
        newStem.addChildGroup("group", "group");

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId("ou=stemNEW,ou=edu,ou=testgroups," + getLdapBaseDn());
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapToGrouperTest.testRenameStemDiff.response.xml");
    }

    public void testRenameStemSync() throws Exception {
        loadLdif(DATA_PATH + "LdapToGrouperTest.testRenameStem.before.ldif");

        Stem stem = edu.addChildStem("stemOLD", "stemOLD");
        stem.addChildGroup("group", "group");

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId("ou=stemNEW,ou=edu,ou=testgroups," + getLdapBaseDn());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapToGrouperTest.testRenameStemSync.response.xml");

        // assert equality since the stem will be known by both names
        Stem stemNEW = StemFinder.findByName(GrouperSession.staticGrouperSession(), "edu:stemNEW", true);
        assertEquals(stem.getUuid(), stemNEW.getUuid());
    }

}

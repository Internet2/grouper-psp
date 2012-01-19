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

import junit.framework.AssertionFailedError;
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
        TestRunner.run(new GrouperToOpenLdapTest("testBulkCalcBushyAddMultipleSubjects"));
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

    public void testBulkCalcBushyAdd() throws Exception {

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkCalcBushyAdd.response.xml");
    }

    public void testBulkCalcBushyAddMultipleSubjects() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapTest.testMultipleSubjects.before.ldif");

        ((LdapSourceAdapter) SubjectFinder.getSource("ldap")).setMultipleResults(true);

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkCalcBushyAddMultipleSubjects.response.xml");
    }

    public void testBulkCalcBushyAddMultipleSubjectsTrue() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapTest.testMultipleSubjects.before.ldif");

        psp.getPso("ldap", "group").getReferences("member").getPsoReference("membersLdap")
                .setMultipleResults(true);

        ((LdapSourceAdapter) SubjectFinder.getSource("ldap")).setMultipleResults(true);

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkCalcBushyAddMultipleSubjectsTrue.response.xml");
    }

    public void testBulkCalcBushyAddSubgroupPhasing() throws Exception {

        groupA.addMember(groupB.toSubject());

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkCalcBushyAddSubgroupPhasing.response.xml");
    }

    public void testBulkCalcBushyAddSubgroupPhasingTwoStep() throws Exception {

        groupA.addMember(groupB.toSubject());

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkCalcBushyAddSubgroupPhasing.response.xml");
    }

    public void testBulkDiffBushyAdd() throws Exception {

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkDiffBushyAdd.response.xml");
    }

    public void testBulkDiffBushyAddMultipleSubjects() throws Exception {

        loadLdif(DATA_PATH + "GrouperToOpenLdapTest.testMultipleSubjects.before.ldif");

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkDiffBushyAddMultipleSubjects.response.xml");
    }

    public void testBulkDiffBushyAddMultipleSubjectsTrue() throws Exception {

        psp.getPso("ldap", "group").getReferences("member").getPsoReference("membersLdap")
                .setMultipleResults(true);

        loadLdif(DATA_PATH + "GrouperToOpenLdapTest.testMultipleSubjects.before.ldif");

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkDiffBushyAddMultipleSubjectsTrue.response.xml");
    }

    public void testBulkDiffBushyAddSubgroupPhasing() throws Exception {

        groupA.addMember(groupB.toSubject());

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkDiffBushyAddSubgroupPhasing.response.xml");
    }

    public void testBulkSyncBushyAdd() throws Exception {

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkSyncBushyAdd.response.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapTest.testBulkSyncBushyAdd.after.ldif");
    }

    public void testBulkSyncBushyAddMultipleSubjectsTrue() throws Exception {

        psp.getPso("ldap", "group").getReferences("member").getPsoReference("membersLdap")
                .setMultipleResults(true);

        ((LdapSourceAdapter) SubjectFinder.getSource("ldap")).setMultipleResults(true);

        loadLdif(DATA_PATH + "GrouperToOpenLdapTest.testMultipleSubjects.before.ldif");

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testBulkSyncBushyAddMultipleSubjectsTrue.response.xml");

        verifyLdif(DATA_PATH + "GrouperToOpenLdapTest.testBulkSyncBushyAddMultipleSubjectsTrue.after.ldif");
    }

    // target ldap directory must not support referential integrity of dns
    // TODO implement
    public void testBulkSyncBushyAddSubgroupPhasing() throws Exception {

        // loadLdif(PSPLdapTest.DATA_PATH + "PSPTest.before.ldif");

        groupA.addMember(groupB.toSubject());

        // CalcRequest request = new CalcRequest();
        // SyncRequest request = new SyncRequest();
        // request.setRequestID("REQUESTID_TEST");
        // request.setReturnData(ReturnData.DATA);
        // request.setId(groupA.getName());
        // CalcResponse response = psp.execute(request);
        // Response response = psp.execute(request);
        // System.out.println(psp.toXML(response));

        // BulkSyncRequest request = new BulkSyncRequest();
        // request.setRequestID("REQUESTID_TEST");
        // request.setReturnData(ReturnData.DATA);
        // BulkSyncResponse response = psp.execute(request);
        // System.out.println(psp.toXML(response));

        // verifySpml(response, DATA_PATH +
        // "GrouperToOpenLdapTest.testBulkSyncBushyAddSubgroupPhasing.response.xml");
        // verifyLdif(DATA_PATH +
        // "GrouperToOpenLdapTest.testBulkSyncBushyAddSubgroupPhasing.after.ldif");
    }

    public void testCalcFlatAddEmptyList() throws Exception {

        makeGroupDNStructureFlat();

        groupB.deleteMember(LdapSubjectTestHelper.SUBJ1);

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testCalcFlatAddEmptyList.response.xml");
    }

    public void testCalcFlatAddEmptyListReturnData() throws Exception {

        makeGroupDNStructureFlat();

        groupB.deleteMember(LdapSubjectTestHelper.SUBJ1);

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        request.setReturnData(ReturnData.DATA);
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testCalcFlatAddEmptyListReturnData.response.xml");
    }

    public void testDiffFlatAddEmptyList() throws Exception {

        makeGroupDNStructureFlat();

        groupB.deleteMember(LdapSubjectTestHelper.SUBJ1);

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testDiffFlatAddEmptyList.response.xml");
    }

    public void testDiffFlatAddEmptyListReturnData() throws Exception {

        makeGroupDNStructureFlat();

        groupB.deleteMember(LdapSubjectTestHelper.SUBJ1);

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        request.setReturnData(ReturnData.DATA);
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testDiffFlatAddEmptyListReturnData.response.xml");
    }

    public void testDiffFlatModifyEmptyListAddMember() throws Exception {

        makeGroupDNStructureFlat();

        loadLdif(DATA_PATH + "GrouperToOpenLdapTest.testDiffFlatModifyEmptyListAddMember.before.ldif");

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testDiffFlatModifyEmptyListAddMember.response.xml");
    }

    public void testDiffFlatModifyEmptyListDeleteMember() throws Exception {

        makeGroupDNStructureFlat();

        groupB.deleteMember(LdapSubjectTestHelper.SUBJ1);

        loadLdif(DATA_PATH + "GrouperToOpenLdapTest.testDiffFlatModifyEmptyListDeleteMember.before.ldif");

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testDiffFlatModifyEmptyListDeleteMember.response.xml");
    }

    public void testSyncFlatAddEmptyList() throws Exception {

        makeGroupDNStructureFlat();

        groupB.deleteMember(LdapSubjectTestHelper.SUBJ1);

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testSyncFlatAddEmptyList.response.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapTest.testSyncFlatAddEmptyList.after.ldif");
    }

    public void testSyncFlatAddEmptyListReturnData() throws Exception {

        makeGroupDNStructureFlat();

        groupB.deleteMember(LdapSubjectTestHelper.SUBJ1);

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        request.setReturnData(ReturnData.DATA);
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testSyncFlatAddEmptyListReturnData.response.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapTest.testSyncFlatAddEmptyList.after.ldif");
    }

    public void testSyncFlatModifyEmptyListAddMember() throws Exception {

        makeGroupDNStructureFlat();

        // if (this.useEmbedded()) {
        // psp.getTargetDefinitions().get("ldap").setBundleModifications(false);
        // }

        loadLdif(DATA_PATH + "GrouperToOpenLdapTest.testSyncFlatModifyEmptyListAddMember.before.ldif");

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        SyncResponse response = psp.execute(request);

        try {
            verifySpml(response, DATA_PATH + "GrouperToOpenLdapTest.testSyncFlatModifyEmptyListAddMember.response.xml");
        } catch (AssertionFailedError e) {
            // if (useEmbedded()) {
            // OK
            // } else {
            throw e;
            // }

        }

        verifyLdif(DATA_PATH + "GrouperToOpenLdapTest.testSyncFlatModifyEmptyListAddMember.after.ldif");
    }

    public void testSyncFlatModifyEmptyListAddMemberUnbundled() throws Exception {

        makeGroupDNStructureFlat();

        psp.getTarget("ldap").setBundleModifications(false);

        loadLdif(DATA_PATH + "GrouperToOpenLdapTest.testSyncFlatModifyEmptyListAddMember.before.ldif");

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH
                + "GrouperToOpenLdapTest.testSyncFlatModifyEmptyListAddMemberUnbundled.response.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapTest.testSyncFlatModifyEmptyListAddMember.after.ldif");
    }

    public void testSyncFlatModifyEmptyListDeleteMember() throws Exception {

        makeGroupDNStructureFlat();

        groupB.deleteMember(LdapSubjectTestHelper.SUBJ1);

        loadLdif(DATA_PATH + "GrouperToOpenLdapTest.testDiffFlatModifyEmptyListDeleteMember.before.ldif");

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        SyncResponse response = psp.execute(request);

        try {
            verifySpml(response, DATA_PATH
                    + "GrouperToOpenLdapTest.testSyncFlatModifyEmptyListDeleteMember.response.xml");
        } catch (AssertionFailedError e) {
            // if (useEmbedded()) {
            // OK
            // } else {
            throw e;
            // }
        }

        if (!psp.getTarget("ldap").isBundleModifications()) {
            verifyLdif(DATA_PATH + "GrouperToOpenLdapTest.testSyncFlatAddEmptyList.after.ldif");
        }
    }

    public void testSyncFlatModifyEmptyListDeleteMemberUnbundled() throws Exception {

        makeGroupDNStructureFlat();

        psp.getTarget("ldap").setBundleModifications(false);

        groupB.deleteMember(LdapSubjectTestHelper.SUBJ1);

        loadLdif(DATA_PATH + "GrouperToOpenLdapTest.testDiffFlatModifyEmptyListDeleteMember.before.ldif");

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH
                + "GrouperToOpenLdapTest.testSyncFlatModifyEmptyListDeleteMemberUnbundled.response.xml");
        verifyLdif(DATA_PATH + "GrouperToOpenLdapTest.testSyncFlatAddEmptyList.after.ldif");
    }

}

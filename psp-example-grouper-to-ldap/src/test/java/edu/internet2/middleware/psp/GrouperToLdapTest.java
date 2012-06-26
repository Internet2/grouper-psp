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
import org.openspml.v2.msg.spml.LookupRequest;
import org.openspml.v2.msg.spml.LookupResponse;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.openspml.v2.msg.spml.Response;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spml.SchemaEntityRef;
import org.openspml.v2.msg.spmlref.HasReference;
import org.openspml.v2.msg.spmlsearch.Query;
import org.openspml.v2.msg.spmlsearch.Scope;
import org.openspml.v2.msg.spmlsearch.SearchRequest;
import org.openspml.v2.msg.spmlsearch.SearchResponse;
import org.openspml.v2.profiles.dsml.EqualityMatch;
import org.openspml.v2.profiles.dsml.Filter;

import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.SubjectFinder;
import edu.internet2.middleware.grouper.helper.StemHelper;
import edu.internet2.middleware.psp.helper.LdapTestHelper;
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
import edu.internet2.middleware.psp.util.OnNotFound;
import edu.internet2.middleware.subject.Subject;

/** Test provisioning Grouper to an LDAP directory. */
public class GrouperToLdapTest extends BaseGrouperLdapTest {

    /**
     * Constructor.
     * 
     * @param string
     */
    public GrouperToLdapTest(String name) {
        super(name);
    }

    /**
     * Run tests.
     * 
     * @param args
     */
    public static void main(String[] args) {
        // TestRunner.run(GrouperToLdapTest.class);
        TestRunner.run(new GrouperToLdapTest("testBulkCalcBushyAddSubjectNotFoundFail"));
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

    public void testBulkCalcBushyAddChildStems() throws Exception {

        setUpCourseTest();

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkCalcBushyAddChildStems.response.xml");
    }

    public void testBulkCalcBushyAddForwardSlash() throws Exception {

        Group groupF = StemHelper.addChildGroup(this.edu, "group/F", "Group/F");
        groupF.addMember(LdapSubjectTestHelper.SUBJ1);
        groupB.addMember(groupF.toSubject());

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkCalcBushyAddForwardSlash.response.xml");
    }

    public void testBulkCalcBushyAddSubgroup() throws Exception {

        groupB.addMember(groupA.toSubject());

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkCalcBushyAddSubgroup.response.xml");
    }

    public void testBulkCalcBushyAddSubjectNotFoundFail() throws Exception {

        psp.getPso("ldap", "group").getReferences("member").getPsoReference("membersLdap")
                .setOnNotFound(OnNotFound.fail);

        groupA.addMember(LdapSubjectTestHelper.SUBJ2);

        LdapTestHelper.deleteCn(LdapSubjectTestHelper.SUBJ2_ID, ldap);

        SubjectFinder.flushCache();

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkCalcBushyAddSubjectNotFoundFail.response.xml");
    }

    public void testBulkCalcBushyAddSubjectNotFoundIgnore() throws Exception {

        psp.getPso("ldap", "group").getReferences("member").getPsoReference("membersLdap")
                .setOnNotFound(OnNotFound.ignore);

        groupA.addMember(LdapSubjectTestHelper.SUBJ2);

        LdapTestHelper.deleteCn(LdapSubjectTestHelper.SUBJ2_ID, ldap);

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkCalcBushyAddSubjectNotFound.response.xml");
    }

    public void testBulkCalcBushyAddSubjectNotFoundWarn() throws Exception {

        groupA.addMember(LdapSubjectTestHelper.SUBJ2);

        LdapTestHelper.deleteCn(LdapSubjectTestHelper.SUBJ2_ID, ldap);

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkCalcBushyAddSubjectNotFound.response.xml");
    }

    public void testBulkCalcBushyAddSubjectWhitespace() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.subjectWhitespace.before.ldif");

        Subject subjA = createSubject("test subject a", "my name is test subject a");
        groupA.addMember(subjA);

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkCalcBushyAddSubjectWhitespace.response.xml");
    }

    public void testBulkCalcBushyDeleteForwardSlash() throws Exception {

        groupA.delete();
        groupB.delete();

        loadLdif(DATA_PATH + "GrouperToLdapTest.testBulkCalcBushyDeleteForwardSlash.before.ldif");

        BulkCalcRequest request = new BulkCalcRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkCalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkCalcBushyDeleteForwardSlash.response.xml");
    }

    public void testBulkDiffBushyAdd() throws Exception {

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkDiffBushyAdd.response.xml");
    }

    public void testBulkDiffBushyAddChildStems() throws Exception {

        setUpCourseTest();

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkDiffBushyAddChildStems.response.xml");
    }

    public void testBulkDiffBushyAddForwardSlash() throws Exception {

        Group groupF = StemHelper.addChildGroup(this.edu, "group/F", "Group/F");
        groupF.addMember(LdapSubjectTestHelper.SUBJ1);
        groupB.addMember(groupF.toSubject());

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkDiffBushyAddForwardSlash.response.xml");
    }

    public void testBulkDiffBushyAddSubgroup() throws Exception {

        groupB.addMember(groupA.toSubject());

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkDiffBushyAddSubgroup.response.xml");
    }

    public void testBulkDiffBushyAddSubjectWhitespace() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.subjectWhitespace.before.ldif");

        Subject subjA = createSubject("test subject a", "my name is test subject a");
        groupA.addMember(subjA);

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkDiffBushyAddSubjectWhitespace.response.xml");
    }

    public void testBulkDiffBushyDeleteForwardSlash() throws Exception {

        groupA.delete();
        groupB.delete();

        loadLdif(DATA_PATH + "GrouperToLdapTest.testBulkCalcBushyDeleteForwardSlash.before.ldif");

        BulkDiffRequest request = new BulkDiffRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkDiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkDiffBushyDeleteForwardSlash.response.xml");
    }

    public void testBulkSyncBushyAdd() throws Exception {

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyAdd.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyAdd.after.ldif");
    }

    public void testBulkSyncBushyAddChildStems() throws Exception {

        setUpCourseTest();

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyAddChildStems.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyAddChildStems.after.ldif");
    }

    public void testBulkSyncBushyAddForwardSlash() throws Exception {

        Group groupF = StemHelper.addChildGroup(this.edu, "group/F", "Group/F");
        groupF.addMember(LdapSubjectTestHelper.SUBJ1);
        groupB.addMember(groupF.toSubject());

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyAddForwardSlash.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyAddForwardSlash.after.ldif");
    }

    public void testBulkSyncBushyAddSubgroup() throws Exception {

        groupB.addMember(groupA.toSubject());

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyAddSubgroup.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyAddSubgroup.after.ldif");
    }

    public void testBulkSyncBushyAddSubjectWhitespace() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.subjectWhitespace.before.ldif");

        Subject subjA = createSubject("test subject a", "my name is test subject a");
        groupA.addMember(subjA);

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyAddSubjectWhitespace.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyAddSubjectWhitespace.after.ldif");
    }

    public void testBulkSyncBushyDeleteForwardSlash() throws Exception {

        groupA.delete();
        groupB.delete();

        loadLdif(DATA_PATH + "GrouperToLdapTest.testBulkCalcBushyDeleteForwardSlash.before.ldif");

        BulkSyncRequest request = new BulkSyncRequest();
        request.setRequestID("REQUESTID_TEST");
        BulkSyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyDeleteForwardSlash.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testBulkSyncBushyDeleteForwardSlash.after.ldif");
    }

    public void testCalcBushyAdd() throws Exception {

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testCalcBushyAdd.response.xml");
    }

    public void testCalcBushyAddScoped() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testCalcBushyAddScoped.before.ldif");

        groupB.addMember(SubjectFinder.findById("test.subject.10", true));

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testCalcBushyAddScoped.response.xml");
    }

    public void testCalcFlatAdd() throws Exception {

        makeGroupDNStructureFlat();

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        CalcResponse response = (CalcResponse) psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testCalcFlatAdd.response.xml");
    }

    public void testCalcFlatAddSchemaEntity() throws Exception {

        makeGroupDNStructureFlat();

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        request.addSchemaEntity(new SchemaEntityRef("ldap", "group"));
        CalcResponse response = (CalcResponse) psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testCalcFlatAdd.response.xml");
    }

    public void testDiffBushyModifyDescription() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushy.before.ldif");

        groupB.setDescription("new description");
        groupB.store();

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testDiffBushyModifyDescription.response.xml");
    }

    public void testDiffBushyModifyMember() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushy.before.ldif");

        groupB.addMember(groupA.toSubject());

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testDiffBushyModifyMember.response.xml");
    }

    public void testDiffBushyModifyMemberCaseInsensitive() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushyCaseInsensitive.before.ldif");

        psp.getPso("ldap", "group").getReferences("member").setCaseSensitive(false);

        groupB.addMember(groupA.toSubject());

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testDiffBushyModifyMember.response.xml");
    }

    public void testDiffBushyModifyMemberForwardSlash() throws Exception {

        Group groupF = StemHelper.addChildGroup(this.edu, "group/F", "Group/F");
        groupF.addMember(LdapSubjectTestHelper.SUBJ0);

        loadLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushyForwardSlash.before.ldif");

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupF.getName());
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testDiffBushyModifyMemberForwardSlash.response.xml");
    }

    public void testDiffBushyModifyMemberUnbundled() throws Exception {

        psp.getTarget("ldap").setBundleModifications(false);

        loadLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushy.before.ldif");

        groupB.addMember(groupA.toSubject());

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testDiffBushyModifyMemberUnbundled.response.xml");
    }

    public void testLookupData() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testLookup.before.ldif");

        LookupRequest request = new LookupRequest();
        request.setReturnData(ReturnData.DATA);
        request.setRequestID("REQUESTID_TEST");
        request.setPsoID(new PSOIdentifier("cn=groupB,ou=edu,ou=groups," + getLdapBaseDn(), null, "ldap"));
        Response response = psp.execute(request);
        assertTrue(response instanceof LookupResponse);
        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testLookupData.response.xml");
    }

    public void testLookupDataForwardSlash() throws Exception {

        Group groupF = StemHelper.addChildGroup(this.edu, "group/F", "Group/F");
        groupF.addMember(LdapSubjectTestHelper.SUBJ1);

        loadLdif(DATA_PATH + "GrouperToLdapTest.testLookupForwardSlash.before.ldif");

        LookupRequest request = new LookupRequest();
        request.setReturnData(ReturnData.DATA);
        request.setRequestID("REQUESTID_TEST");
        request.setPsoID(new PSOIdentifier("cn=group/F,ou=edu,ou=groups," + getLdapBaseDn(), null, "ldap"));
        Response response = psp.execute(request);
        assertTrue(response instanceof LookupResponse);
        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testLookupDataForwardSlash.response.xml");
    }

    public void testLookupEverything() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testLookup.before.ldif");

        LookupRequest request = new LookupRequest();
        request.setReturnData(ReturnData.EVERYTHING);
        request.setRequestID("REQUESTID_TEST");
        request.setPsoID(new PSOIdentifier("cn=groupB,ou=edu,ou=groups," + getLdapBaseDn(), null, "ldap"));
        Response response = psp.execute(request);
        assertTrue(response instanceof LookupResponse);
        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testLookupEverything.response.xml");
    }

    public void testLookupIdentifier() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testLookup.before.ldif");

        LookupRequest request = new LookupRequest();
        request.setReturnData(ReturnData.IDENTIFIER);
        request.setRequestID("REQUESTID_TEST");
        request.setPsoID(new PSOIdentifier("cn=groupB,ou=edu,ou=groups," + getLdapBaseDn(), null, "ldap"));
        Response response = psp.execute(request);
        assertTrue(response instanceof LookupResponse);
        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testLookupIdentifier.response.xml");
    }

    public void testLookupIdentifierForwardSlash() throws Exception {

        Group groupF = StemHelper.addChildGroup(this.edu, "group/F", "Group/F");
        groupF.addMember(LdapSubjectTestHelper.SUBJ1);

        loadLdif(DATA_PATH + "GrouperToLdapTest.testLookupForwardSlash.before.ldif");

        LookupRequest request = new LookupRequest();
        request.setReturnData(ReturnData.IDENTIFIER);
        request.setRequestID("REQUESTID_TEST");
        request.setPsoID(new PSOIdentifier("cn=group/F,ou=edu,ou=groups," + getLdapBaseDn(), null, "ldap"));
        Response response = psp.execute(request);
        assertTrue(response instanceof LookupResponse);
        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testLookupIdentifierForwardSlash.response.xml");
    }

    public void testLookupIdentifierForwardSlashEscaped() throws Exception {

        Group groupF = StemHelper.addChildGroup(this.edu, "group/F", "Group/F");
        groupF.addMember(LdapSubjectTestHelper.SUBJ1);

        loadLdif(DATA_PATH + "GrouperToLdapTest.testLookupForwardSlash.before.ldif");

        LookupRequest request = new LookupRequest();
        request.setReturnData(ReturnData.IDENTIFIER);
        request.setRequestID("REQUESTID_TEST");
        request.setPsoID(new PSOIdentifier("cn=group\\/F,ou=edu,ou=groups," + getLdapBaseDn(), null, "ldap"));
        Response response = psp.execute(request);
        assertTrue(response instanceof LookupResponse);
        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testLookupIdentifierForwardSlash.response.xml");
    }

    public void testLookupNoSuchIdentifier() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testLookup.before.ldif");

        LookupRequest request = new LookupRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setPsoID(new PSOIdentifier("cn=UnknownGroup,ou=edu,ou=groups," + getLdapBaseDn(), null, "ldap"));
        Response response = psp.execute(request);
        assertTrue(response instanceof LookupResponse);
        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testLookupNoSuchIdentifier.response.xml");
    }

    public void testSearchRequest() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testSearchRequest.before.ldif");

        Query query = new Query();
        query.setTargetID("ldap");
        PSOIdentifier basePsoID = new PSOIdentifier();
        basePsoID.setID("ou=groups," + getLdapBaseDn());
        basePsoID.setTargetID("ldap");
        query.setBasePsoID(basePsoID);
        query.addQueryClause(new Filter(new EqualityMatch("cn", "groupB")));
        SearchRequest request = new SearchRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setQuery(query);

        SearchResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSearchRequest.response.xml");
    }

    public void testSearchRequestData() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testSearchRequest.before.ldif");

        Query query = new Query();
        query.setTargetID("ldap");
        PSOIdentifier basePsoID = new PSOIdentifier();
        basePsoID.setID("ou=groups," + getLdapBaseDn());
        basePsoID.setTargetID("ldap");
        query.setBasePsoID(basePsoID);
        query.addQueryClause(new Filter(new EqualityMatch("cn", "groupB")));
        SearchRequest request = new SearchRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setQuery(query);
        request.setReturnData(ReturnData.DATA);

        SearchResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSearchRequestData.response.xml");
    }

    public void testSearchRequestEverything() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testSearchRequest.before.ldif");

        Query query = new Query();
        query.setTargetID("ldap");
        PSOIdentifier basePsoID = new PSOIdentifier();
        basePsoID.setID("ou=groups," + getLdapBaseDn());
        basePsoID.setTargetID("ldap");
        query.setBasePsoID(basePsoID);
        query.addQueryClause(new Filter(new EqualityMatch("cn", "groupB")));
        SearchRequest request = new SearchRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setQuery(query);
        request.setReturnData(ReturnData.EVERYTHING);

        SearchResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSearchRequest.response.xml");
    }

    public void testSearchRequestIdentifier() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testSearchRequest.before.ldif");

        Query query = new Query();
        query.setTargetID("ldap");
        PSOIdentifier basePsoID = new PSOIdentifier();
        basePsoID.setID("ou=groups," + getLdapBaseDn());
        basePsoID.setTargetID("ldap");
        query.setBasePsoID(basePsoID);
        query.addQueryClause(new Filter(new EqualityMatch("cn", "groupB")));
        SearchRequest request = new SearchRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setQuery(query);
        request.setReturnData(ReturnData.IDENTIFIER);

        SearchResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSearchRequestIdentifier.response.xml");
    }

    public void testSearchRequestNotFound() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testSearchRequest.before.ldif");

        Query query = new Query();
        query.setTargetID("ldap");
        PSOIdentifier basePsoID = new PSOIdentifier();
        basePsoID.setID("ou=groups," + getLdapBaseDn());
        basePsoID.setTargetID("ldap");
        query.setBasePsoID(basePsoID);
        query.addQueryClause(new Filter(new EqualityMatch("cn", "NOT_FOUND")));
        SearchRequest request = new SearchRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setQuery(query);
        request.setReturnData(ReturnData.EVERYTHING);

        SearchResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSearchRequestNotFound.response.xml");
    }

    public void testSearchRequestHasReference() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testSearchRequestHasReference.before.ldif");

        PSOIdentifier memberID = new PSOIdentifier();
        memberID.setID("cn=test.subject.0,ou=people," + getLdapBaseDn());

        HasReference hasReference = new HasReference();
        hasReference.setToPsoID(memberID);
        hasReference.setTypeOfReference("member");

        Query query = new Query();
        PSOIdentifier groupID = new PSOIdentifier();
        groupID.setID("cn=groupB,ou=groups," + getLdapBaseDn());
        // TODO not necessary ? groupID.setTargetID("ldap");
        query.setBasePsoID(groupID);
        query.setTargetID("ldap");
        query.addQueryClause(hasReference);
        query.setScope(Scope.PSO);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setReturnData(ReturnData.EVERYTHING);
        searchRequest.setQuery(query);

        SearchResponse response = psp.execute(searchRequest);
        System.out.println(response.toXML(psp.getXMLMarshaller()));

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSearchRequestHasReference.response.xml");
    }

    public void testSyncBushyModifyDescription() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushy.before.ldif");

        groupB.setDescription("new description");
        groupB.store();

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSyncBushyModifyDescription.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testModifyDescriptionBushy.after.ldif");
    }

    public void testSyncBushyModifyMember() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushy.before.ldif");

        groupB.addMember(groupA.toSubject());

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSyncBushyModifyMember.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushy.after.ldif");
    }

    public void testSyncBushyModifyMemberCaseInsensitive() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushyCaseInsensitive.before.ldif");

        psp.getPso("ldap", "group").getReferences("member").setCaseSensitive(false);

        groupB.addMember(groupA.toSubject());

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSyncBushyModifyMemberCaseInsensitive.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testSyncBushyModifyMemberCaseInsensitive.after.ldif");
    }

    public void testSyncBushyModifyMemberForwardSlash() throws Exception {

        Group groupF = StemHelper.addChildGroup(this.edu, "group/F", "Group/F");
        groupF.addMember(LdapSubjectTestHelper.SUBJ0);

        loadLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushyForwardSlash.before.ldif");

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupF.getName());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSyncBushyModifyMemberForwardSlash.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushyForwardSlash.after.ldif");
    }

    public void testSyncBushyModifyMemberUnbundled() throws Exception {

        psp.getTarget("ldap").setBundleModifications(false);

        loadLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushy.before.ldif");

        groupB.addMember(groupA.toSubject());

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSyncBushyModifyMemberUnbundled.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testModifyMemberBushy.after.ldif");
    }

    public void testSyncFlatAdd() throws Exception {

        makeGroupDNStructureFlat();

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSyncFlatAdd.response.xml");
        verifyLdif(DATA_PATH + "GrouperToLdapTest.testSyncFlatAdd.after.ldif");
    }

    public void testCalcAlternateName() throws Exception {

        groupB.setExtension("newExtensionGroupB");
        groupB.store();

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testCalcAlternateName.response.xml");
    }

    public void testDiffAlternateName() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testDiffAlternateName.before.ldif");

        psp.setLogSpml(true);

        groupB.setExtension("newExtensionGroupB");
        groupB.store();

        DiffRequest request = new DiffRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        DiffResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testDiffAlternateName.response.xml");
    }

    public void testSyncAlternateName() throws Exception {

        loadLdif(DATA_PATH + "GrouperToLdapTest.testDiffAlternateName.before.ldif");

        psp.setLogSpml(true);

        groupB.setExtension("newExtensionGroupB");
        groupB.store();

        SyncRequest request = new SyncRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupB.getName());
        SyncResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperToLdapTest.testSyncAlternateName.response.xml");

        verifyLdif(DATA_PATH + "GrouperToLdapTest.testSyncAlternateName.after.ldif");
    }

}

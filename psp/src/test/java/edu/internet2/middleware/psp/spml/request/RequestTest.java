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

package edu.internet2.middleware.psp.spml.request;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.textui.TestRunner;

import org.custommonkey.xmlunit.XMLTestCase;
import org.openspml.v2.msg.Marshallable;
import org.openspml.v2.msg.spml.AddRequest;
import org.openspml.v2.msg.spml.AddResponse;
import org.openspml.v2.msg.spml.DeleteRequest;
import org.openspml.v2.msg.spml.DeleteResponse;
import org.openspml.v2.msg.spml.ExecutionMode;
import org.openspml.v2.msg.spml.Extensible;
import org.openspml.v2.msg.spml.Modification;
import org.openspml.v2.msg.spml.ModificationMode;
import org.openspml.v2.msg.spml.ModifyRequest;
import org.openspml.v2.msg.spml.ModifyResponse;
import org.openspml.v2.msg.spml.PSO;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.openspml.v2.msg.spml.QueryClause;
import org.openspml.v2.msg.spml.Request;
import org.openspml.v2.msg.spml.Response;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spml.SchemaEntityRef;
import org.openspml.v2.msg.spml.StatusCode;
import org.openspml.v2.msg.spmlsearch.Query;
import org.openspml.v2.msg.spmlsearch.Scope;
import org.openspml.v2.msg.spmlsearch.SearchRequest;
import org.openspml.v2.profiles.dsml.DSMLProfileException;
import org.openspml.v2.profiles.dsml.EqualityMatch;
import org.openspml.v2.profiles.dsml.Filter;
import org.openspml.v2.profiles.dsml.FilterItem;
import org.openspml.v2.util.Spml2Exception;

import edu.internet2.middleware.psp.Psp;
import edu.internet2.middleware.psp.PSPTestHelper;
import edu.internet2.middleware.psp.spml.request.AlternateIdentifier;
import edu.internet2.middleware.psp.spml.request.BulkCalcRequest;
import edu.internet2.middleware.psp.spml.request.BulkCalcResponse;
import edu.internet2.middleware.psp.spml.request.BulkDiffResponse;
import edu.internet2.middleware.psp.spml.request.BulkSyncResponse;
import edu.internet2.middleware.psp.spml.request.CalcRequest;
import edu.internet2.middleware.psp.spml.request.CalcResponse;
import edu.internet2.middleware.psp.spml.request.DiffResponse;
import edu.internet2.middleware.psp.spml.request.SyncResponse;
import edu.internet2.middleware.psp.spml.request.SynchronizedResponse;
import edu.internet2.middleware.psp.util.PSPUtil;

public class RequestTest extends XMLTestCase {

    // private static final Logger LOG = LoggerFactory.getLogger(RequestTests.class);

    /** resource location for test data */
    // public static final String TEST_PATH = "/test/edu/internet2/middleware/ldappc/spml/request";
    public static final String TEST_PATH = "request/";

    private PSOIdentifier id1;

    private PSOIdentifier id2;

    private PSOIdentifier id3;

    private PSOIdentifier id4;

    private PSO pso1;

    private PSO pso2;

    private PSO pso3;

    private PSO pso4;

    private Extensible data1;

    private Extensible data2;

    private Psp psp;

    public RequestTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        // TestRunner.run(RequestTest.class);
        TestRunner.run(new RequestTest("testSearch"));
    }

    public void setUp() {

        // try {
        // PSPOptions pspOptions = new PSPOptions(null);
        // pspOptions.setConfDir(GrouperUtil.fileFromResourceName("/test/edu/internet2/middleware/ldappc/spml")
        // .getAbsolutePath());
        // psp = PSP.getPSP(pspOptions);
        // } catch (ResourceException e) {
        // throw new RuntimeException(e);
        // }

        // setUpPSP();
        psp = new Psp();
        psp.setId(RequestTest.class.getName());
        psp.initializeSpmlToolkit();

        pso1 = new PSO();
        id1 = new PSOIdentifier("id1", null, "target1");
        pso1.setPsoID(id1);

        pso2 = new PSO();
        id2 = new PSOIdentifier("id2", null, "target1");
        pso2.setPsoID(id2);

        pso3 = new PSO();
        id3 = new PSOIdentifier("id3", null, "target2");
        pso3.setPsoID(id3);

        pso4 = new PSO();
        id4 = new PSOIdentifier("id4", null, "target2");
        pso4.setPsoID(id4);

        data1 = new Extensible();
        data1.addOpenContentAttr("attr1", "value1");
        pso1.setData(data1);
        pso3.setData(data1);

        data2 = new Extensible();
        data2.addOpenContentAttr("attr2", "value2");
        pso2.setData(data2);
        pso4.setData(data2);
    }

    public void testCalculateRequest() {

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setExecutionMode(ExecutionMode.SYNCHRONOUS);
        request.setId("id");
        request.addSchemaEntity(new SchemaEntityRef("target1", null));
        request.addSchemaEntity(new SchemaEntityRef("target2", null));
        request.setReturnData(ReturnData.EVERYTHING);

        String string =
                "CalcRequest[id=id,requestID=REQUESTID_TEST,returnData=everything,schemaEntityRef=SchemaEntityRef[targetID=target1,entityName=<null>,isContainer=false],schemaEntityRef=SchemaEntityRef[targetID=target2,entityName=<null>,isContainer=false]]";
        assertEquals(string, request.toString());

        Marshallable object = verifySpml(request, TEST_PATH + "RequestTests.testCalculateRequest.xml", true);

        assertTrue(object instanceof CalcRequest);
        CalcRequest that = (CalcRequest) object;

        assertEquals(request.getId(), that.getId());
        assertEquals(request.getSchemaEntities(), that.getSchemaEntities());
    }

    public void testCalculateResponse() {

        CalcResponse response = new CalcResponse();
        response.setId("id");
        response.addPSO(pso1);
        response.addPSO(pso2);

        Marshallable object = verifySpml(response, TEST_PATH + "RequestTests.testCalculateResponse.xml", true);

        assertTrue(object instanceof CalcResponse);
        CalcResponse that = (CalcResponse) object;

        assertEquals(response.getPSOs(), that.getPSOs());
    }

    public void testDiffResponse() throws Spml2Exception {

        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.setPsoID(id1);
        deleteRequest.setRecursive(true);

        AddRequest addRequest = new AddRequest();
        addRequest.setPsoID(id2);
        addRequest.setData(data1);

        ModifyRequest modifyRequest = new ModifyRequest();
        modifyRequest.setPsoID(id3);
        Modification mod = new Modification();
        mod.setData(data2);
        mod.setModificationMode(ModificationMode.REPLACE);
        modifyRequest.addModification(mod);

        SynchronizedResponse synchronizedResponse = new SynchronizedResponse();
        synchronizedResponse.setPsoID(id4);

        DiffResponse diffResponse = new DiffResponse();
        diffResponse.setId("id");
        diffResponse.addRequest(deleteRequest);
        diffResponse.addRequest(addRequest);
        diffResponse.addRequest(modifyRequest);
        diffResponse.addResponse(synchronizedResponse);

        Marshallable object = verifySpml(diffResponse, TEST_PATH + "RequestTests.testDiffResponse.xml", true);

        assertTrue(object instanceof DiffResponse);
        DiffResponse that = (DiffResponse) object;

        testDiffResponse(diffResponse, that);
    }

    public void testSyncResponse() throws Spml2Exception {

        DeleteResponse deleteResponse = new DeleteResponse();
        deleteResponse.setStatus(StatusCode.SUCCESS);

        AddResponse addResponse = new AddResponse();
        addResponse.setPso(pso1);

        ModifyResponse modifyResponse = new ModifyResponse();
        modifyResponse.setPso(pso2);

        SyncResponse syncResponse = new SyncResponse();
        syncResponse.setId("id");
        syncResponse.addResponse(addResponse);
        syncResponse.addResponse(deleteResponse);
        syncResponse.addResponse(modifyResponse);

        Marshallable object = verifySpml(syncResponse, TEST_PATH + "RequestTests.testSyncResponse.xml", true);

        assertTrue(object instanceof SyncResponse);
        SyncResponse that = (SyncResponse) object;

        testSyncResponse(syncResponse, that);
    }

    public void testBulkCalcResponse() {

        BulkCalcResponse bulk = new BulkCalcResponse();

        System.err.println(psp.toXML(bulk));
        System.err.println(psp.toXML(bulk));

        CalcResponse calcResponse1 = new CalcResponse();
        calcResponse1.setId(pso1.getPsoID().getID());
        calcResponse1.addOpenContentAttr("ID", pso1.getPsoID().getID());
        calcResponse1.addPSO(pso1);
        bulk.addResponse(calcResponse1);

        CalcResponse calcResponse2 = new CalcResponse();
        calcResponse2.setId(pso2.getPsoID().getID());
        calcResponse2.addOpenContentAttr("ID", pso2.getPsoID().getID());
        calcResponse2.addPSO(pso2);
        bulk.addResponse(calcResponse2);

        Marshallable object = verifySpml(bulk, TEST_PATH + "RequestTests.testBulkCalcResponse.xml", true);

        assertTrue(object instanceof BulkCalcResponse);
        BulkCalcResponse that = (BulkCalcResponse) object;

        Map<PSOIdentifier, CalcResponse> map = that.getResponseMap();
        assertEquals(map.get(pso1.getPsoID()), calcResponse1);
        assertEquals(map.get(pso2.getPsoID()), calcResponse2);
    }

    public void testBulkCalcRequest() {
        BulkCalcRequest bulk = new BulkCalcRequest();

        System.err.println(psp.toXML(bulk));
        System.err.println(psp.toXML(bulk));
    }

    public void testBulkDiffResponse() throws Spml2Exception {

        BulkDiffResponse bulk = new BulkDiffResponse();

        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.setPsoID(id1);
        deleteRequest.setRecursive(true);

        AddRequest addRequest = new AddRequest();
        addRequest.setPsoID(id2);
        addRequest.setData(data1);

        ModifyRequest modifyRequest = new ModifyRequest();
        modifyRequest.setPsoID(id3);
        Modification mod = new Modification();
        mod.setData(data2);
        mod.setModificationMode(ModificationMode.REPLACE);
        modifyRequest.addModification(mod);

        SynchronizedResponse synchronizedResponse = new SynchronizedResponse();
        synchronizedResponse.setPsoID(id4);

        DiffResponse diffResponse1 = new DiffResponse();
        diffResponse1.setId("id1");
        diffResponse1.addRequest(deleteRequest);
        bulk.addResponse(diffResponse1);

        DiffResponse diffResponse2 = new DiffResponse();
        diffResponse2.setId("id2");
        diffResponse2.addRequest(addRequest);
        bulk.addResponse(diffResponse2);

        DiffResponse diffResponse3 = new DiffResponse();
        diffResponse3.setId("id3");
        diffResponse3.addRequest(modifyRequest);
        bulk.addResponse(diffResponse3);

        Marshallable object = verifySpml(bulk, TEST_PATH + "RequestTests.testBulkDiffResponse.xml", true);

        assertTrue(object instanceof BulkDiffResponse);
        BulkDiffResponse that = (BulkDiffResponse) object;

        Map<String, DiffResponse> map = new HashMap<String, DiffResponse>();
        for (DiffResponse response : that.getResponses()) {
            map.put(response.getId(), response);
        }

        assertEquals(diffResponse1, map.get("id1"));
        assertEquals(diffResponse2, map.get("id2"));
        assertEquals(diffResponse3, map.get("id3"));
    }

    public void testBulkSyncResponse() throws Spml2Exception {

        BulkSyncResponse bulk = new BulkSyncResponse();

        DeleteResponse deleteResponse = new DeleteResponse();
        deleteResponse.setStatus(StatusCode.SUCCESS);

        AddResponse addResponse = new AddResponse();
        addResponse.setPso(pso1);

        ModifyResponse modifyResponse = new ModifyResponse();
        modifyResponse.setPso(pso2);

        SyncResponse syncResponse1 = new SyncResponse();
        syncResponse1.setId("id1");
        syncResponse1.addResponse(addResponse);
        bulk.addResponse(syncResponse1);

        SyncResponse syncResponse2 = new SyncResponse();
        syncResponse2.setId("id2");
        syncResponse2.addResponse(modifyResponse);
        bulk.addResponse(syncResponse2);

        SyncResponse syncResponse3 = new SyncResponse();
        syncResponse3.setId("id3");
        syncResponse3.addResponse(deleteResponse);
        bulk.addResponse(syncResponse3);

        Marshallable object = verifySpml(bulk, TEST_PATH + "RequestTests.testBulkSyncResponse.xml", false);

        assertTrue(object instanceof BulkSyncResponse);
        BulkSyncResponse that = (BulkSyncResponse) object;

        Map<String, SyncResponse> map = new HashMap<String, SyncResponse>();
        for (SyncResponse response : that.getResponses()) {
            map.put(response.getId(), response);
        }

        assertEquals(syncResponse1, map.get("id1"));
        assertEquals(syncResponse2, map.get("id2"));
        assertEquals(syncResponse3, map.get("id3"));
    }

    public void testAlternateIdentifier() {

        AlternateIdentifier alternateIdentifier = new AlternateIdentifier();
        alternateIdentifier.setID("altID");
        alternateIdentifier.setTargetID("targetID");

        PSOIdentifier psoIdentifier = new PSOIdentifier();
        psoIdentifier.setID("ID");
        psoIdentifier.setTargetID("targetID");

        PSO pso = new PSO();
        pso.setPsoID(psoIdentifier);
        pso.addOpenContentElement(alternateIdentifier);

        CalcResponse calcResponse = new CalcResponse();
        calcResponse.setPSOs(Arrays.asList(new PSO[] {pso}));

        Marshallable object = verifySpml(calcResponse, TEST_PATH + "RequestTests.testAlternateIdentifier.xml", false);

        assertTrue(object instanceof CalcResponse);
        CalcResponse that = (CalcResponse) object;

        List<AlternateIdentifier> fromResponse =
                calcResponse.getPSOs().get(0).getOpenContentElements(AlternateIdentifier.class);
        List<AlternateIdentifier> fromMarshall =
                that.getPSOs().get(0).getOpenContentElements(AlternateIdentifier.class);

        assertEquals(alternateIdentifier, fromResponse.get(0));
        assertEquals(alternateIdentifier, fromMarshall.get(0));
        assertEquals(fromResponse.get(0), fromMarshall.get(0));
    }

    private Marshallable verifySpml(Marshallable testObject, String correctXMLFileName, boolean testEquality) {
        return PSPTestHelper.verifySpml(psp.getXMLMarshaller(), psp.getXmlUnmarshaller(), testObject,
                PSPUtil.getFile(correctXMLFileName), testEquality, null);
    }

    public static void testRequestMap(Map<PSOIdentifier, Request> correct, Map<PSOIdentifier, Request> current) {
        for (PSOIdentifier psoID : correct.keySet()) {
            Request other = current.get(psoID);
            assertNotNull(other);
            assertEquals(correct.get(psoID), other);
        }
        for (PSOIdentifier psoID : current.keySet()) {
            Request other = correct.get(psoID);
            assertNotNull(other);
            assertEquals(current.get(psoID), other);
        }
    }

    public static void testResponseMap(Map<PSOIdentifier, Response> correct, Map<PSOIdentifier, Response> current) {
        for (PSOIdentifier psoID : correct.keySet()) {
            Response other = current.get(psoID);
            assertNotNull(other);
            assertEquals(correct.get(psoID), other);
        }
        for (PSOIdentifier psoID : current.keySet()) {
            Response other = correct.get(psoID);
            assertNotNull(other);
            assertEquals(current.get(psoID), other);
        }
    }

    public static void testDiffResponse(DiffResponse correct, DiffResponse current) {
        // TODO OCEtoMarshallable testing
        assertEquals(correct.getId(), current.getId());
        testRequestMap(correct.getRequestMap(), current.getRequestMap());
        testResponseMap(correct.getResponseMap(), current.getResponseMap());
    }

    public static void testSyncResponse(SyncResponse correct, SyncResponse current) {
        // TODO OCEtoMarshallable testing
        assertEquals(correct.getId(), current.getId());
        testResponseMap(correct.getResponseMap(), current.getResponseMap());
        assertEquals(correct.getDeleteResponses(), current.getDeleteResponses());
    }

    public void testSearch() throws DSMLProfileException {

        SearchRequest request = new SearchRequest();
        request.setReturnData(ReturnData.IDENTIFIER);

        Query query = new Query();
        query.setTargetID("targetID");
        query.setScope(Scope.SUBTREE);

        PSOIdentifier basePsoId = new PSOIdentifier();
        basePsoId.setID("ou=groups");
        query.setBasePsoID(basePsoId);

        EqualityMatch equalityMatch = new EqualityMatch("name", "value");

        Filter filter = new Filter();
        filter.setItem(equalityMatch);

        query.addQueryClause(filter);

        // query.addQueryClause(filterQC);

        request.setQuery(query);

        System.out.println(psp.toXML(request));

        // Marshallable object =
        // verifySpml(request, TEST_PATH + "RequestTests.testLdapFilterQueryClauseSearchRequest.xml", true);

        // assertTrue(object instanceof SearchRequest);
        // SearchRequest that = (SearchRequest) object;

        // assertEquals(request.getQuery(), that.getQuery());

        Query rq = request.getQuery();

        for (QueryClause qc : rq.getQueryClauses()) {
            if (qc instanceof Filter) {
                FilterItem filterItem = ((Filter) qc).getItem();

                if (filterItem instanceof EqualityMatch) {
                    System.err.println(((EqualityMatch) filterItem).getName());
                    System.err.println(((EqualityMatch) filterItem).getValue());
                }
            }
        }

    }
}

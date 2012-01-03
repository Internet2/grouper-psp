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

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.openspml.v2.msg.Marshallable;
import org.openspml.v2.msg.spml.AddRequest;
import org.openspml.v2.msg.spml.AddResponse;
import org.openspml.v2.msg.spml.ExecutionMode;
import org.openspml.v2.msg.spml.LookupRequest;
import org.openspml.v2.msg.spml.LookupResponse;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.openspml.v2.msg.spml.Request;
import org.openspml.v2.msg.spml.Response;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spmlsearch.Query;
import org.openspml.v2.msg.spmlsearch.SearchRequest;
import org.openspml.v2.msg.spmlsearch.SearchResponse;
import org.openspml.v2.msg.spmlupdates.UpdatesRequest;

import edu.internet2.middleware.psp.spml.request.CalcRequest;
import edu.internet2.middleware.psp.spml.request.CalcResponse;
import edu.internet2.middleware.psp.util.PSPUtil;

public class PSPTest extends TestCase {

    public static final String CONFIG_PATH = "psp/";

    public static final String DATA_PATH = "psp/data/";

    private Psp psp;

    public static void main(String[] args) {
        // TestRunner.run(PSPTest.class);
        TestRunner.run(new PSPTest("testLookupMalformedRequest"));
    }

    public PSPTest(String name) {
        super(name);
    }

    public void setUp() {
        psp = new Psp();
        psp.setId(PSPTest.class.getName());
        psp.initializeSpmlToolkit();
    }

    public void verifySpml(Marshallable testObject, String correctXMLFileName) {
        PSPTestHelper.verifySpml(psp.getXMLMarshaller(), psp.getXmlUnmarshaller(), testObject,
                PSPUtil.getFile(correctXMLFileName));
    }

    public void testAddRequestNoPSOId() throws Exception {

        AddRequest request = new AddRequest();
        request.setRequestID("REQUESTID_TEST");
        AddResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "PSPTest.testAddRequestNoPSOId.response.xml");
    }

    public void testAddRequestNoTargetId() throws Exception {

        AddRequest request = new AddRequest();
        request.setRequestID("REQUESTID_TEST");
        PSOIdentifier psoID = new PSOIdentifier();
        psoID.setID("ID");
        request.setPsoID(psoID);
        AddResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "PSPTest.testAddRequestNoTargetId.response.xml");
    }

    public void testCalcMalformedRequest() throws Exception {

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "PSPTest.testCalcMalformedRequest.response.xml");
    }

    public void testLookupMalformedRequest() throws Exception {

        LookupRequest request = new LookupRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setPsoID(new PSOIdentifier());
        Response response = psp.execute(request);
        assertTrue(response instanceof LookupResponse);
        verifySpml(response, DATA_PATH + "PSPTest.testLookupMalformedRequest.response.xml");
    }

    public void testSearchRequestUnknownTarget() throws Exception {

        SearchRequest request = new SearchRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setReturnData(ReturnData.IDENTIFIER);
        Query query = new Query();
        query.setTargetID("UNKNOWN");
        request.setQuery(query);        
        SearchResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "PSPTest.testSearchRequestUnknownTarget.response.xml");
    }

    public void testSearchRequestNoQuery() throws Exception {

        SearchRequest request = new SearchRequest();
        request.setRequestID("REQUESTID_TEST");
        SearchResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "PSPTest.testSearchRequestNoQuery.response.xml");
    }

    public void testSearchRequestNoTargetId() throws Exception {

        SearchRequest request = new SearchRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setReturnData(ReturnData.IDENTIFIER);
        Query query = new Query();
        request.setQuery(query);
        SearchResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "PSPTest.testSearchRequestNoTargetId.response.xml");
    }

    public void testUnsupportedExecutionMode() {

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setRequestID("REQUESTID_TEST");
        searchRequest.setExecutionMode(ExecutionMode.ASYNCHRONOUS);
        Response response = psp.execute((Request) searchRequest);
        verifySpml(response, DATA_PATH + "PSPTest.unsupportedExecutionMode.response.xml");
    }

    public void testUnsupportedOperation() {

        UpdatesRequest updatesRequest = new UpdatesRequest();
        updatesRequest.setRequestID("REQUESTID_TEST");
        Response response = psp.execute(updatesRequest);
        verifySpml(response, DATA_PATH + "PSPTest.unsupportedOperation.response.xml");
    }
}

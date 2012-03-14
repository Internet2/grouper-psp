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

package edu.internet2.middleware.psp.shibboleth;

import junit.textui.TestRunner;

import org.opensaml.util.resource.ResourceException;

import edu.internet2.middleware.psp.BaseGrouperLdapTest;
import edu.internet2.middleware.psp.spml.request.CalcRequest;
import edu.internet2.middleware.psp.spml.request.CalcResponse;

/** Test creation of Ldap DNs from Grouper names. */
public class LdapDnFromGrouperNameTest extends BaseGrouperLdapTest {

    /**
     * Constructor.
     * 
     * @param name
     */
    public LdapDnFromGrouperNameTest(String name) {
        super(name);
    }

    /**
     * Run tests.
     * 
     * @param args
     */
    public static void main(String[] args) {
        //TestRunner.run(LdapDnFromGrouperNameTest.class);
        TestRunner.run(new LdapDnFromGrouperNameTest("testGroupA"));
    }

    public void setUp() {

        super.setUp();

        try {
            setUpPSP();
        } catch (ResourceException e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        }

        edu = setUpEdu();
    }

    public void testGroupA() throws Exception {

        groupA = setUpGroupA();

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId(groupA.getName());
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapDnFromGrouperNameTest.testGroupA.response.xml");
    }

    public void testCourseA() throws Exception {

        setUpCourseTest();

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId("edu:courses:spring:courseA");
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "LdapDnFromGrouperNameTest.testCourseA.response.xml");
    }

}

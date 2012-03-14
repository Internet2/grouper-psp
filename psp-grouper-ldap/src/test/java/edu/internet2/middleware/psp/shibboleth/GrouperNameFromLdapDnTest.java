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

/** Test creation of Grouper names from Ldap DNs. */
public class GrouperNameFromLdapDnTest extends BaseGrouperLdapTest {

    /**
     * Constructor.
     * 
     * @param name
     */
    public GrouperNameFromLdapDnTest(String name) {
        super(name);
    }

    /**
     * Run tests.
     * 
     * @param args
     */
    public static void main(String[] args) {
        TestRunner.run(GrouperNameFromLdapDnTest.class);
        // TestRunner.run(new GrouperNameFromLdapDnTest("testGroupA"));
    }

    public void setUp() {

        super.setUp();

        try {
            setUpPSP();
            setUpLdap();
        } catch (ResourceException e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        }
    }

    public void testGroupA() throws Exception {

        loadLdif(DATA_PATH + "GrouperNameFromLdapDnTest.before.ldif");

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId("cn=groupA,ou=edu,ou=groups," + getLdapBaseDn());
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperNameFromLdapDnTest.testGroupA.response.xml");
    }

    public void testCourseA() throws Exception {

        loadLdif(DATA_PATH + "GrouperNameFromLdapDnTest.before.ldif");

        CalcRequest request = new CalcRequest();
        request.setRequestID("REQUESTID_TEST");
        request.setId("cn=courseA,ou=spring,ou=courses,ou=edu,ou=groups," + getLdapBaseDn());
        CalcResponse response = psp.execute(request);

        verifySpml(response, DATA_PATH + "GrouperNameFromLdapDnTest.testCourseA.response.xml");
    }

}

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

import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spml.SchemaEntityRef;

import edu.internet2.middleware.psp.spml.request.CalcRequest;
import edu.internet2.middleware.psp.spml.request.ProvisioningRequest;

/**
 * Basic tests for cli argument processing.
 */
public class PSPOptionsTest extends TestCase {

  public static void main(String[] args) {
    TestRunner.run(PSPOptionsTest.class);
    // TestRunner.run(new PSPOptionsTest("testCalc"));
  }

  public PSPOptionsTest(String name) {
    super(name);
  }

  public void testCalc() throws Exception {

    String[] args = new String[] { "-" + PspOptions.Mode.calc.getOpt(), "test",
        "-" + PspOptions.Opts.returnData.getOpt(), "-" + PspOptions.Opts.targetID.getOpt(), "target1",
        "-" + PspOptions.Opts.requestID.getOpt(), "REQUEST1" };

    PspOptions options = new PspOptions(args);
    options.parseCommandLineOptions();

    List<ProvisioningRequest> requests = options.getRequests();
    assertEquals(1, requests.size());
    CalcRequest request = new CalcRequest();
    request.setId("test");
    request.setReturnData(ReturnData.DATA);
    request.addSchemaEntity(new SchemaEntityRef("target1", null));
    request.setRequestID("REQUEST1");
    assertEquals(request, requests.get(0));
  }

  public void testMissingRequiredID() {

    try {
      PspOptions options = new PspOptions(new String[] { "-" + PspOptions.Mode.calc.getOpt() });
      options.parseCommandLineOptions();
      fail("should throw exception");
    } catch (ParseException e) {
      // OK
      assertTrue(e instanceof MissingArgumentException);
    }

    try {
      PspOptions options = new PspOptions(new String[] { "-" + PspOptions.Mode.diff.getOpt() });
      options.parseCommandLineOptions();
      fail("should throw exception");
    } catch (ParseException e) {
      // OK
      assertTrue(e instanceof MissingArgumentException);
    }

    try {
      PspOptions options = new PspOptions(new String[] { "-" + PspOptions.Mode.sync.getOpt() });
      options.parseCommandLineOptions();
      fail("should throw exception");
    } catch (ParseException e) {
      // OK
      assertTrue(e instanceof MissingArgumentException);
    }

  }

  public void testMissingRequiredOptions() {

    try {
      PspOptions options = new PspOptions(null);
      options.parseCommandLineOptions();
      fail("should throw exception");
    } catch (ParseException e) {
      // OK
      assertTrue(e instanceof MissingOptionException);
    }
  }

}

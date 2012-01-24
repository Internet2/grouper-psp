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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.log4j.spi.LoggingEvent;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.DifferenceListener;
import org.opensaml.util.resource.PropertyReplacementResourceFilter;
import org.opensaml.xml.util.DatatypeHelper;
import org.openspml.v2.msg.Marshallable;
import org.openspml.v2.msg.XMLMarshaller;
import org.openspml.v2.msg.XMLUnmarshaller;
import org.openspml.v2.util.Spml2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.psp.util.PSPUtil;

/**
 *
 */
public class PSPTestHelper {

  private static final Logger LOG = LoggerFactory.getLogger(PSPTestHelper.class);

  public static Marshallable verifySpml(XMLMarshaller m, XMLUnmarshaller u, Marshallable testObject, File correctXMLFile) {
    return verifySpml(m, u, testObject, correctXMLFile, false, null);
  }

  public static Marshallable verifySpml(XMLMarshaller m, XMLUnmarshaller u, Marshallable testObject,
      File correctXMLFile, boolean testEquality, File propertiesFile) {
    try {
      InputStream correct = new FileInputStream(correctXMLFile);
      return verifySpml(m, u, testObject, correct, testEquality, propertiesFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException("File not found", e);
    }
  }

  public static Marshallable verifySpml(XMLMarshaller m, XMLUnmarshaller u, Marshallable testObject,
      InputStream correctXMLStream, boolean testEquality, File propertiesFile) {

    try {
      String testXML = testObject.toXML(m);

      Marshallable unmarshalledObject = u.unmarshall(testXML);

      String unmarshalledTestXML = unmarshalledObject.toXML(m);

      String correctXML = null;
      if (propertiesFile != null) {
        // replace macros
        PropertyReplacementResourceFilter filter = new PropertyReplacementResourceFilter(propertiesFile);
        correctXML = DatatypeHelper.inputstreamToString(filter.applyFilter(correctXMLStream), null);
      } else {
        correctXML = DatatypeHelper.inputstreamToString(correctXMLStream, null);
      }

      Marshallable unmarshalledFromCorrectXMLFile = u.unmarshall(correctXML);

      if (LOG.isDebugEnabled()) {
        LOG.debug("current:\n{}", testXML);
        LOG.debug("correct:\n{}", correctXML);
        LOG.debug("unmarshalledTestXML:\n{}", unmarshalledTestXML);
      }

      // test objects
      if (testEquality) {
        Assert.assertEquals(testObject, unmarshalledObject);
        Assert.assertEquals(unmarshalledFromCorrectXMLFile, testObject);
      }

      // TODO test marshalling and unmarshalling objects
      // OCEtoMarshallableAdapter does not have an equals() method

      // test marshalling and unmarshalling xml
      DetailedDiff marshallingDiff = new DetailedDiff(new Diff(testXML, unmarshalledTestXML));
      Assert.assertTrue(marshallingDiff.identical());

      // ignore requestID, must test similar not identical
      DifferenceListener ignoreRequestID = new IgnoreRequestIDDifferenceListener();

      // test testXML against correctXML
      Diff correctDiff = new Diff(new StringReader(correctXML), new StringReader(testXML));

      correctDiff.overrideDifferenceListener(ignoreRequestID);
      // // TODO ignore order ?
      // correctDiff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());

      DetailedDiff correctDetailedDiff = new DetailedDiff(correctDiff);
      // TODO ignore order ?
      // correctDetailedDiff.overrideElementQualifier(new
      // RecursiveElementNameAndTextQualifier());
      correctDetailedDiff.overrideDifferenceListener(ignoreRequestID);

      if (!correctDetailedDiff.getAllDifferences().isEmpty()) {
        LOG.debug("differences   \n'{}'", correctDetailedDiff.getAllDifferences());
        LOG.debug("diff to string\n'{}'", correctDetailedDiff.toString());
      }
      Assert.assertTrue("SPML diff should be empty", correctDetailedDiff.getAllDifferences().isEmpty());
      Assert.assertTrue("SPML diff should be similar", correctDetailedDiff.similar());

      // test unmarshalledXML against correctXML
      Diff unmarshalledDiff = new Diff(new StringReader(correctXML), new StringReader(unmarshalledTestXML));

      unmarshalledDiff.overrideDifferenceListener(ignoreRequestID);

      DetailedDiff unmarshalledDetailedDiff = new DetailedDiff(unmarshalledDiff);
      unmarshalledDetailedDiff.overrideDifferenceListener(ignoreRequestID);
      if (!unmarshalledDetailedDiff.getAllDifferences().isEmpty()) {
        LOG.debug("differences '{}'", unmarshalledDetailedDiff.getAllDifferences());
        LOG.debug("diff '{}'", unmarshalledDetailedDiff.toString());
      }
      Assert.assertTrue(unmarshalledDetailedDiff.getAllDifferences().isEmpty());
      Assert.assertTrue(unmarshalledDetailedDiff.similar());

      return unmarshalledObject;

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("An error occurred : " + e.getMessage());
      return null;
    }
  }

  public static void verifySpml(XMLMarshaller m, XMLUnmarshaller u, InputStream correctXMLStream,
      InputStream currentXMLStream, boolean testEquality, File propertiesFile) {

    try {
      String correctXML = null;
      String currentXML = null;
      if (propertiesFile != null) {
        // replace macros
        PropertyReplacementResourceFilter filter = new PropertyReplacementResourceFilter(propertiesFile);
        correctXML = DatatypeHelper.inputstreamToString(filter.applyFilter(correctXMLStream), null);
        currentXML = DatatypeHelper.inputstreamToString(filter.applyFilter(currentXMLStream), null);
      } else {
        correctXML = DatatypeHelper.inputstreamToString(correctXMLStream, null);
        currentXML = DatatypeHelper.inputstreamToString(currentXMLStream, null);
      }

      Marshallable unmarshalledFromCorrectXMLFile = u.unmarshall(correctXML);
      Marshallable unmarshalledFromCurrentXMLFile = u.unmarshall(currentXML);

      if (LOG.isDebugEnabled()) {
        LOG.debug("current:\n{}", currentXML);
        LOG.debug("correct:\n{}", correctXML);
      }

      // test objects
      if (testEquality) {
        // Assert.assertEquals(testObject, unmarshalledObject);
        Assert.assertEquals(unmarshalledFromCorrectXMLFile, unmarshalledFromCurrentXMLFile);
      }

      // ignore requestID, must test similar not identical
      DifferenceListener ignoreRequestID = new IgnoreRequestIDDifferenceListener();

      // test testXML against correctXML
      Diff correctDiff = new Diff(new StringReader(correctXML), new StringReader(currentXML));

      correctDiff.overrideDifferenceListener(ignoreRequestID);
      // // TODO ignore order ?
      // correctDiff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());

      DetailedDiff correctDetailedDiff = new DetailedDiff(correctDiff);
      // TODO ignore order ?
      // correctDetailedDiff.overrideElementQualifier(new
      // RecursiveElementNameAndTextQualifier());
      correctDetailedDiff.overrideDifferenceListener(ignoreRequestID);

      if (!correctDetailedDiff.getAllDifferences().isEmpty()) {
        LOG.debug("differences   \n'{}'", correctDetailedDiff.getAllDifferences());
        LOG.debug("diff to string\n'{}'", correctDetailedDiff.toString());
      }
      Assert.assertTrue("SPML diff should be empty", correctDetailedDiff.getAllDifferences().isEmpty());
      Assert.assertTrue("SPML diff should be similar", correctDetailedDiff.similar());

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("An error occurred : " + e.getMessage());
    }
  }

  public static void verifySpmlLoggingEvents(File propertiesFile, Psp psp, List<LoggingEvent> loggingEvents,
      String correctXMLFileName) {

    PSPTestHelper.verifySpmlLoggingEvents(psp.getXMLMarshaller(), psp.getXmlUnmarshaller(), loggingEvents,
        correctXMLFileName, propertiesFile);
  }

  public static void verifySpmlLoggingEvents(XMLMarshaller m, XMLUnmarshaller u, List<LoggingEvent> loggingEvents,
      String correctXMLFileName, File propertiesFile) {

    try {
      // build list of string messages
      List<String> stringLoggingEvents = new ArrayList<String>();
      for (LoggingEvent loggingEvent : loggingEvents) {
        stringLoggingEvents.add(loggingEvent.getMessage().toString());
      }

      // read correct newline separated spml messages from file
      InputStream correctXMLInputStream = new FileInputStream(PSPUtil.getFile(correctXMLFileName));
      String correctXML = DatatypeHelper.inputstreamToString(correctXMLInputStream, null);
      List<String> correctXMLMessages = new ArrayList<String>();
      String[] toks = correctXML.split("\\n\\n");
      for (int i = 0; i < toks.length; i++) {
        if (toks[i].startsWith("\n")) {
          correctXMLMessages.add(toks[i]);
        } else {
          correctXMLMessages.add("\n" + toks[i]);
        }
      }

      Assert.assertEquals("Number of messages mismatch", correctXMLMessages.size(), stringLoggingEvents.size());

      // verify each message in order
      for (int i = 0; i < toks.length; i++) {
        InputStream correctInputStream = new ByteArrayInputStream(correctXMLMessages.get(i).getBytes("UTF-8"));
        InputStream currentInputStream = new ByteArrayInputStream(stringLoggingEvents.get(i).getBytes("UTF-8"));

        PSPTestHelper.verifySpml(m, u, correctInputStream, currentInputStream, false, propertiesFile);
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
      Assert.fail("An error occurred : " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail("An error occurred : " + e.getMessage());
    }
  }

  public static void verifySpmlLoggingEventsWrite(List<LoggingEvent> loggingEvents, String correctXMLFileName,
      File propertiesFile) {

    List<String> strings = new ArrayList<String>();
    for (LoggingEvent loggingEvent : loggingEvents) {
      strings.add(loggingEvent.getMessage().toString());
    }

    PSPTestHelper.writeCorrectTestFile(propertiesFile, correctXMLFileName, strings.toArray(new String[] {}));
  }

  public static void verifySpmlWrite(File propertiesFile, Psp psp, Marshallable testObject, String correctXMLFileName) {

    try {
      String xml = testObject.toXML(psp.getXMLMarshaller());
      PSPTestHelper.writeCorrectTestFile(propertiesFile, correctXMLFileName, xml);
    } catch (Spml2Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static void writeCorrectTestFile(File propertiesFile, String fileName, String... strings) {
    try {

      File file = PSPUtil.getFile(fileName);
      if (file != null) {
        throw new RuntimeException("File already exists " + fileName);
      }

      String path = System.getProperty("user.dir") + File.separator + "src" + File.separator + "test" + File.separator
          + "resources" + File.separator + fileName;

      Properties props = new Properties();
      props.load(new FileInputStream(propertiesFile));

      BufferedWriter out = new BufferedWriter(new FileWriter(path));

      for (String string : strings) {

        if (!DatatypeHelper.isEmpty(string)) {
          // string = string.replace(props.getProperty("edu.vt.middleware.ldap.base"), "${edu.vt.middleware.ldap.base}");
          
          string = string.replace(props.getProperty("edu.internet2.middleware.psp.groupsBaseDn"), "${edu.internet2.middleware.psp.groupsBaseDn}");
          string = string.replace(props.getProperty("edu.internet2.middleware.psp.peopleBaseDn"), "${edu.internet2.middleware.psp.peopleBaseDn}");

          string = string.replace("<dsml:value>" + props.getProperty("edu.internet2.middleware.psp.groupObjectClass") + "</dsml:value>",
              "<dsml:value>${edu.internet2.middleware.psp.groupObjectClass}</dsml:value>");

          // xml = xml.replaceAll("requestID='2.*?'", "requestID='REQUEST_ID'");

          if (!string.endsWith(System.getProperty("line.separator"))) {
            string = string + System.getProperty("line.separator");
          }
        }
        out.write(string);
      }

      out.close();

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}

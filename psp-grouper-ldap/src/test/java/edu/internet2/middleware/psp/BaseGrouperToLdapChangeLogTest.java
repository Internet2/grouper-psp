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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.opensaml.util.resource.ResourceException;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.grouper.app.loader.GrouperLoaderConfig;
import edu.internet2.middleware.grouper.app.loader.GrouperLoaderStatus;
import edu.internet2.middleware.grouper.app.loader.db.Hib3GrouperLoaderLog;
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;
import edu.internet2.middleware.grouper.changeLog.ChangeLogHelper;
import edu.internet2.middleware.grouper.changeLog.ChangeLogTempToEntity;
import edu.internet2.middleware.grouper.hibernate.HibernateSession;
import edu.internet2.middleware.grouper.misc.GrouperDAOFactory;
import edu.internet2.middleware.grouper.util.GrouperUtil;
import edu.internet2.middleware.psp.grouper.PspChangeLogConsumer;
import edu.internet2.middleware.psp.shibboleth.ChangeLogDataConnector;
import edu.internet2.middleware.psp.util.PSPUtil;

/** Abstract class for testing the {@link PspChangeLogConsumer} */
public abstract class BaseGrouperToLdapChangeLogTest extends BaseGrouperLdapTest {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(BaseGrouperToLdapChangeLogTest.class);

    /** The temporary file to which spml requests and responses are written. */
    private File tmpFile;

    /** The PSP change log consumer. */
    protected PspChangeLogConsumer pspConsumer;

    /**
     * Constructor
     * 
     * @param name
     */
    public BaseGrouperToLdapChangeLogTest(String name) {
        super(name);
    }

    /**
     * Create temporary file. Initialize the {@link PspChangeLogConsumer} and underlying {@link Psp}.
     * 
     * {@inheritDoc}
     */
    public void setUp() {

        super.setUp();

        GrouperLoaderConfig.retrieveConfig().properties().put("changeLog.consumer.psp.class", PspChangeLogConsumer.class.getName());
        // GrouperLoaderConfig.testConfig.put("changeLog.consumer.ldappcng.quartzCron", "0 0 8 * * ?");
        // GrouperLoaderConfig.testConfig.put("changeLog.consumer.ldappcng.confDir",

        setUpMailLocalAddressAttributeDef();

        setUpSeeAlsoAddressAttributeDef();
        
        try {

            tmpFile = File.createTempFile(getName(), ".tmp");
            LOG.debug("creating tmp file '{}'", tmpFile);
            tmpFile.deleteOnExit();

            setUpPSPConsumer(tmpFile);

        } catch (IOException e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        } catch (ResourceException e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        }

        runChangeLog();
    }

    /**
     * Initialize the {@Link LdappcngConsumer} and configure the underlying {@link Psp} to write spml requests
     * and responses to the given file.
     * 
     * @param outputFile
     * @throws ResourceException
     */
    public void setUpPSPConsumer(File outputFile) throws ResourceException {
        pspConsumer = new PspChangeLogConsumer();
        pspConsumer.initialize();
        pspConsumer.getPsp().setWriteRequests(true);
        pspConsumer.getPsp().setWriteResponses(true);
        pspConsumer.getPsp().setPathToOutputFile(outputFile.getAbsolutePath());

        // set the writer to null so we write a new file for every test, necessary when PSP is static
        pspConsumer.getPsp().setWriter(null);

        // just for tests
        pspConsumer.getPsp().setLogSpml(true);

        // just for tests
        psp = pspConsumer.getPsp();
    }

    /**
     * Delete the temporary file.
     * 
     * {@inheritDoc}
     */
    public void tearDown() {

        // delete tmp file if it exists
        if (tmpFile != null) {
            if (tmpFile.exists()) {
                LOG.debug("deleting tmp file '{}'", tmpFile);
                tmpFile.delete();
            }
        }

        super.tearDown();
    }

    public static void clearChangeLog() {
        ChangeLogTempToEntity.convertRecords();
        HibernateSession.byHqlStatic().createQuery("delete from ChangeLogEntryEntity").executeUpdate();
    }

    /**
     * Process change log records.
     */
    public void runChangeLog() {

        Hib3GrouperLoaderLog hib3GrouploaderLog = new Hib3GrouperLoaderLog();
        hib3GrouploaderLog.setHost(GrouperUtil.hostname());
        hib3GrouploaderLog.setJobName("psp");
        hib3GrouploaderLog.setStatus(GrouperLoaderStatus.RUNNING.name());
        hib3GrouploaderLog.store();

        try {
            ChangeLogHelper.processRecords("psp", hib3GrouploaderLog, pspConsumer);
            hib3GrouploaderLog.setStatus(GrouperLoaderStatus.SUCCESS.name());
        } catch (Exception e) {
            LOG.error("Error processing records", e);
            e.printStackTrace();
            hib3GrouploaderLog.setStatus(GrouperLoaderStatus.ERROR.name());
        }
        hib3GrouploaderLog.store();
    }

    /**
     * Given spml messages separated by an empty line, return a list of these messages, split by the empty line.
     * 
     * @param xml the xml containing empty line separated xml messages
     * @return the list of messages
     */
    public List<String> parseStrings(String xml) {
        List<String> strings = new ArrayList<String>();
        String[] toks = xml.split("\\n\\n");
        for (int i = 0; i < toks.length; i++) {
            if (toks[i].startsWith("\n")) {
                strings.add(toks[i]);
            } else {
                strings.add("\n" + toks[i]);
            }
        }
        return strings;
    }

    public void printChangeLogEntries() {
        List<ChangeLogEntry> changeLogEntryList =
                GrouperDAOFactory.getFactory().getChangeLogEntry().retrieveBatch(-1, 100);
        LOG.debug("change log entry list size {}", changeLogEntryList.size());
        for (ChangeLogEntry changeLogEntry : changeLogEntryList) {
            LOG.debug(ChangeLogDataConnector.toStringDeep(changeLogEntry));
            LOG.debug(changeLogEntry.toStringDeep());
        }
    }

    /**
     * Verify that the spml messages in the given file match those in the temporary file.
     * 
     * @param correctXMLFileName the file containing correct spml messages
     */
    public void verifySpml(String correctXMLFileName) {
        try {
            String currentXML = DatatypeHelper.inputstreamToString(new FileInputStream(tmpFile), null);
            List<String> currentXMLMsgs = parseStrings(currentXML);

            String correctXML =
                    DatatypeHelper.inputstreamToString(new FileInputStream(PSPUtil.getFile(correctXMLFileName)), null);
            List<String> correctXMLMsgs = parseStrings(correctXML);

            LOG.debug("current\n{}", currentXML);
            LOG.debug("correct\n{}", correctXML);

            Assert.assertEquals("Number of messages mismatch", correctXMLMsgs.size(), currentXMLMsgs.size());

            // verify each message in order
            for (int i = 0; i < currentXMLMsgs.size(); i++) {
                InputStream correctInputStream = new ByteArrayInputStream(correctXMLMsgs.get(i).getBytes("UTF-8"));
                InputStream currentInputStream = new ByteArrayInputStream(currentXMLMsgs.get(i).getBytes("UTF-8"));

                PSPTestHelper.verifySpml(pspConsumer.getPsp().getXMLMarshaller(), pspConsumer.getPsp()
                        .getXmlUnmarshaller(), correctInputStream, currentInputStream, false, getPropertiesFile());
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        } catch (IOException e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        }
    }

    /**
     * Write the correct spml file from output written to the temporary file. Usually used once during test setup.
     * 
     * @param correctXMLFileName the file which will contain the correct spml messages
     */
    protected void verifySpmlWrite(String correctXMLFileName) {
        try {
            String currentXML = DatatypeHelper.inputstreamToString(new FileInputStream(tmpFile), null);
            List<String> strings = parseStrings(currentXML);
            PSPTestHelper.writeCorrectTestFile(getPropertiesFile(), correctXMLFileName,
                    strings.toArray(new String[] {}));
        } catch (IOException e) {
            e.printStackTrace();
            fail("An error occurred : " + e);
        }
    }

}

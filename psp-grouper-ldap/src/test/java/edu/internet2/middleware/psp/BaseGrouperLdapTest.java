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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.opensaml.util.resource.ResourceException;
import org.openspml.v2.msg.Marshallable;
import org.openspml.v2.msg.spml.SchemaEntityRef;

import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.RegistrySubject;
import edu.internet2.middleware.grouper.Stem;
import edu.internet2.middleware.grouper.StemFinder;
import edu.internet2.middleware.grouper.SubjectFinder;
import edu.internet2.middleware.grouper.attr.AttributeDef;
import edu.internet2.middleware.grouper.attr.AttributeDefName;
import edu.internet2.middleware.grouper.attr.AttributeDefType;
import edu.internet2.middleware.grouper.attr.AttributeDefValueType;
import edu.internet2.middleware.grouper.helper.GrouperTest;
import edu.internet2.middleware.grouper.helper.StemHelper;
import edu.internet2.middleware.grouper.misc.GrouperDAOFactory;
import edu.internet2.middleware.grouper.util.GrouperUtil;
import edu.internet2.middleware.psp.helper.LdapTestHelper;
import edu.internet2.middleware.psp.shibboleth.GroupDnStructure;
import edu.internet2.middleware.psp.shibboleth.LdapDnFromGrouperNamePSOIdentifierAttributeDefinition;
import edu.internet2.middleware.psp.spml.config.Pso;
import edu.internet2.middleware.psp.util.PSPUtil;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethAttributeResolver;
import edu.internet2.middleware.subject.Subject;
import edu.vt.middleware.ldap.Ldap;

/** Test provisioning Grouper to an LDAP directory target. */
public abstract class BaseGrouperLdapTest extends GrouperTest {

    /** The name of the ldap properties file. */
    public static String PROPERTIES_FILE = "ldap.properties";

    /** The Spring id of the Shibboleth Attribute Resolver. */
    public static final String SPRING_ID_ATTRIBUTE_RESOLVER = "psp.AttributeResolver";

    /** Path to xml and ldif files. */
    public String DATA_PATH = "data/";

    /** The 'edu' stem. */
    protected Stem edu;

    /** Test group A. */
    protected Group groupA;

    /** Test group B. */
    protected Group groupB;

    /** The ldap connection. */
    protected Ldap ldap;

    /** The ldap properties file. */
    private File propertiesFile;

    /** The provisioning service provider. */
    protected Psp psp;

    /**
     * Constructor.
     * 
     * @param name
     */
    public BaseGrouperLdapTest(String name) {
        super(name);
    }

    /**
     * Create a <code>Subject</code> with the given id and name. Returns the newly created subject.
     * 
     * @param id the subject id
     * @param name the subject name
     * @return the <code>Subject<code> or throws <code>SubjectNotFoundException<code> if the subject is not found.
     */
    public Subject createSubject(String id, String name) {

        RegistrySubject registrySubject = new RegistrySubject();
        registrySubject.setId(id);
        registrySubject.setName(name);
        registrySubject.setTypeString("person");

        registrySubject.getAttributes().put("name", GrouperUtil.toSet("name." + id));
        registrySubject.getAttributes().put("loginid", GrouperUtil.toSet("id." + id));
        registrySubject.getAttributes().put("description", GrouperUtil.toSet("description." + id));

        GrouperDAOFactory.getFactory().getRegistrySubject().create(registrySubject);

        return SubjectFinder.findById(id, true);
    }

    /**
     * Delete all ldap entries under the vt-ldap base dn.
     * 
     * @throws NamingException
     */
    public void deleteAllLdapEntries() throws NamingException {
        LdapTestHelper.deleteChildren(ldap.getLdapConfig().getBaseDn(), ldap);
    }

    public List<String> getAllReferenceNames() {
        ArrayList<String> referenceNames = new ArrayList<String>();
        try {
            Map<String, List<Pso>> map = psp.getTargetAndObjectDefinitions(new SchemaEntityRef());
            for (String targetId : map.keySet()) {
                for (Pso psoDefinition : map.get(targetId)) {
                    referenceNames.addAll(psoDefinition.getReferenceNames());
                }
            }
        } catch (PspException e) {
            // ignore, unreachable
        }
        return referenceNames;
    }

    /**
     * Get the base dn from the vt-ldap configuration.
     * 
     * @return the base dn from the vt-ldap configuration
     */
    public String getLdapBaseDn() {
        return ldap.getLdapConfig().getBaseDn();
    }

    /**
     * Get the ldap properties file.
     * 
     * @return the ldap properties file
     */
    public File getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * Load the supplied LDIF file into the target ldap directory.
     * 
     * Macros are replaced using the {@link PROPERTIES_FILE}.
     * 
     * @param file the name of the ldif file
     * @throws Exception if an error occurs
     */
    public void loadLdif(String fileName) throws Exception {
        File file = PSPUtil.getFile(fileName);
        if (file == null) {
            throw new IllegalArgumentException("Unable to find '" + fileName + "'");
        }
        LdapTestHelper.loadLdif(file, propertiesFile, ldap);
    }

    /**
     * Convert a psp-resolver.xml configuration from bushy to flat dn structure.
     */
    public void makeGroupDNStructureFlat() {

        ShibbolethAttributeResolver AR =
                (ShibbolethAttributeResolver) psp.getApplicationContext().getBean(SPRING_ID_ATTRIBUTE_RESOLVER);

        LdapDnFromGrouperNamePSOIdentifierAttributeDefinition lad =
                (LdapDnFromGrouperNamePSOIdentifierAttributeDefinition) AR.getAttributeDefinitions().get("groupDn");

        lad.setStructure(GroupDnStructure.flat);

        // TODO really ?
        lad.setSourceAttributeID("extension");
    }

    /**
     * {@inheritDoc}
     * 
     * Find the properties file {@link PROPERTIES_FILE}.
     */
    public void setUp() {

        super.setUp();

        propertiesFile = PSPUtil.getFile(PROPERTIES_FILE);
        if (propertiesFile == null) {
            throw new IllegalArgumentException("Unable to find '" + PROPERTIES_FILE + "'");
        }
    }

    /**
     * Set up stems and groups representing courses.
     */
    public void setUpCourseTest() {
        Stem courses = edu.addChildStem("courses", "Courses");

        Stem spring = courses.addChildStem("spring", "Spring");
        Stem fall = courses.addChildStem("fall", "Fall");

        Group springCourseA = spring.addChildGroup("courseA", "Course A");
        springCourseA.addMember(LdapSubjectTestHelper.SUBJ0);
        springCourseA.addMember(LdapSubjectTestHelper.SUBJ1);

        Group springCourseB = spring.addChildGroup("courseB", "Course B");
        springCourseB.addMember(LdapSubjectTestHelper.SUBJ1);

        Group fallCourseA = fall.addChildGroup("courseA", "Course A");
        fallCourseA.addMember(LdapSubjectTestHelper.SUBJ0);
        fallCourseA.addMember(LdapSubjectTestHelper.SUBJ1);

        Group fallCourseB = fall.addChildGroup("courseB", "Course B");
        fallCourseB.addMember(LdapSubjectTestHelper.SUBJ1);
    }

    /**
     * Set up the edu stem.
     * 
     * @return the edu stem
     */
    public Stem setUpEdu() {
        GrouperSession.startRootSession();
        Stem root = StemHelper.findRootStem(GrouperSession.staticGrouperSession());
        edu = StemHelper.addChildStem(root, "edu", "education");
        return edu;
    }

    /**
     * Set up groupA.
     * 
     * @return groupA
     */
    public Group setUpGroupA() {
        groupA = StemHelper.addChildGroup(edu, "groupA", "Group A");
        groupA.addMember(LdapSubjectTestHelper.SUBJ0);
        return groupA;
    }

    /**
     * Set up groupB.
     * 
     * @return groupB
     */
    public Group setUpGroupB() {
        groupB = StemHelper.addChildGroup(edu, "groupB", "Group B");
        groupB.addMember(LdapSubjectTestHelper.SUBJ1);
        groupB.setDescription("descriptionB");
        groupB.store();
        return groupB;
    }

    /**
     * Set up the mailLocalAddress attribute definition.
     * 
     * @return the attribute def name, whatever that is
     */
    public AttributeDefName setUpMailLocalAddressAttributeDef() {

        Stem etcAttribute = StemFinder.findByName(GrouperSession.staticGrouperSession(), "etc:attribute", true);

        AttributeDef attributeDef =
                etcAttribute.addChildAttributeDef("mailLocalAddressAttributeDef", AttributeDefType.attr);
        attributeDef.setAssignToGroup(true);
        attributeDef.setMultiValued(true);
        attributeDef.setValueType(AttributeDefValueType.string);
        attributeDef.store();

        return etcAttribute.addChildAttributeDefName(attributeDef, "mailLocalAddress", "mailLocalAddress");
    }

    /**
     * Delete all existing entries under the base DN and load the skeleton LDIF.
     * 
     * @throws Exception
     */
    public Ldap setUpLdap() throws Exception {
        ldap = new Ldap();
        ldap.loadFromProperties();
        deleteAllLdapEntries();
        loadLdif(DATA_PATH + "GrouperLdapTest.before.ldif");
        return ldap;
    }

    /**
     * Initialize the {@link Psp}.
     */
    public Psp setUpPSP() throws ResourceException {
        psp = Psp.getPSP();
        return psp;
    }

    /**
     * Verify that the entries in the ldif file are correctly provisioned.
     * 
     * @param pathToCorrectFile the ldif file
     * @throws Exception if an error occurs
     */
    public void verifyLdif(String pathToCorrectFile) throws Exception {
        String correctLdif = LdapTestHelper.readFile(PSPUtil.getFile(pathToCorrectFile));
        LdapTestHelper.verifyLdif(correctLdif, propertiesFile, getAllReferenceNames(),
                ldap.getLdapConfig().getBaseDn(), ldap, false);
    }

    /**
     * Verify that the spml object is the same as the xml representation.
     * 
     * @param testObject the spml object
     * @param correctXMLFileName the xml representation
     */
    public void verifySpml(Marshallable testObject, String correctXMLFileName) {
        PSPTestHelper.verifySpml(psp.getXMLMarshaller(), psp.getXmlUnmarshaller(), testObject,
                PSPUtil.getFile(correctXMLFileName), false, propertiesFile);
    }

    /**
     * Write the xml representation of the spml object.
     * 
     * @param testObject the spml object
     * @param correctXMLFileName the xml representation
     */
    public void verifySpmlWrite(Marshallable testObject, String correctXMLFileName) {
        PSPTestHelper.verifySpmlWrite(propertiesFile, psp, testObject, correctXMLFileName);
    }

}

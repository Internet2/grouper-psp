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

import edu.internet2.middleware.grouper.SubjectFinder;
import edu.internet2.middleware.subject.Subject;

/** Subjects for ldap tests. */
public class LdapSubjectTestHelper {

    public static final Subject SUBJ0;

    public static final Subject SUBJ1;

    public static final Subject SUBJ2;

    public static final Subject SUBJ3;

    public static final Subject SUBJ4;

    public static final Subject SUBJ5;

    public static final Subject SUBJ6;

    public static final Subject SUBJ7;

    public static final Subject SUBJ8;

    public static final Subject SUBJ9;

    public static final String SUBJ0_ID = "test.subject.0";

    public static final String SUBJ0_IDENTIFIER = "id.test.subject.0";

    public static final String SUBJ0_NAME = "my name is test.subject.0";

    public static final String SUBJ0_TYPE = "person";

    public static final String SUBJ1_ID = "test.subject.1";

    public static final String SUBJ1_IDENTIFIER = "id.test.subject.1";

    public static final String SUBJ1_NAME = "my name is test.subject.1";

    public static final String SUBJ1_TYPE = "person";

    public static final String SUBJ2_ID = "test.subject.2";

    public static final String SUBJ2_IDENTIFIER = "id.test.subject.2";

    public static final String SUBJ2_NAME = "my name is test.subject.2";

    public static final String SUBJ2_TYPE = "person";

    public static final String SUBJ3_ID = "test.subject.3";

    public static final String SUBJ3_IDENTIFIER = "id.test.subject.3";

    public static final String SUBJ3_NAME = "my name is test.subject.3";

    public static final String SUBJ3_TYPE = "person";

    public static final String SUBJ4_ID = "test.subject.4";

    public static final String SUBJ4_IDENTIFIER = "id.test.subject.4";

    public static final String SUBJ4_NAME = "my name is test.subject.4";

    public static final String SUBJ4_TYPE = "person";

    public static final String SUBJ5_ID = "test.subject.5";

    public static final String SUBJ5_IDENTIFIER = "id.test.subject.5";

    public static final String SUBJ5_NAME = "my name is test.subject.5";

    public static final String SUBJ5_TYPE = "person";

    public static final String SUBJ6_ID = "test.subject.6";

    public static final String SUBJ6_IDENTIFIER = "id.test.subject.6";

    public static final String SUBJ6_NAME = "my name is test.subject.6";

    public static final String SUBJ6_TYPE = "person";

    public static final String SUBJ7_ID = "test.subject.7";

    public static final String SUBJ7_IDENTIFIER = "id.test.subject.7";

    public static final String SUBJ7_NAME = "my name is test.subject.7";

    public static final String SUBJ7_TYPE = "person";

    public static final String SUBJ8_ID = "test.subject.8";

    public static final String SUBJ8_IDENTIFIER = "id.test.subject.8";

    public static final String SUBJ8_NAME = "my name is test.subject.8";

    public static final String SUBJ8_TYPE = "person";

    public static final String SUBJ9_ID = "test.subject.9";

    public static final String SUBJ9_IDENTIFIER = "id.test.subject.9";

    public static final String SUBJ9_NAME = "my name is test.subject.9";

    public static final String SUBJ9_TYPE = "person";

    static {
        try {
            SUBJ0 = SubjectFinder.findByIdAndSource(SUBJ0_ID, "ldap", true);
            SUBJ1 = SubjectFinder.findByIdAndSource(SUBJ1_ID, "ldap", true);
            SUBJ2 = SubjectFinder.findByIdAndSource(SUBJ2_ID, "ldap", true);
            SUBJ3 = SubjectFinder.findByIdAndSource(SUBJ3_ID, "ldap", true);
            SUBJ4 = SubjectFinder.findByIdAndSource(SUBJ4_ID, "ldap", true);
            SUBJ5 = SubjectFinder.findByIdAndSource(SUBJ5_ID, "ldap", true);
            SUBJ6 = SubjectFinder.findByIdAndSource(SUBJ6_ID, "ldap", true);
            SUBJ7 = SubjectFinder.findByIdAndSource(SUBJ7_ID, "ldap", true);
            SUBJ8 = SubjectFinder.findByIdAndSource(SUBJ8_ID, "ldap", true);
            SUBJ9 = SubjectFinder.findByIdAndSource(SUBJ9_ID, "ldap", true);
        } catch (Exception e) {
            throw new RuntimeException("Unable to run tests without subjects: " + e.getMessage(), e);
        }
    }
}

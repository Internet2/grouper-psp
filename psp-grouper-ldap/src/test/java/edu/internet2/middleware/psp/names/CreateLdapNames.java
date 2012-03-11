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

package edu.internet2.middleware.psp.names;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.WordUtils;
import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.psp.util.PSPUtil;

/**
 *
 */
public class CreateLdapNames {

    /** Text file containing female and male first names one per line. */
    public static final String GIVENNAME_FILE = "names/givenName.txt";

    /** Csv file containing surnames occurring 100 times or more. */
    public static final String SURNAME_FILE = "names/app_c.csv";

    public static Set<String> uids = new HashSet<String>();

    /**
     * @param args
     */

    public static void main(String[] args) {

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("/tmp/people.ldif"));

            RandomCollection<String> givenNames = readGivenNames();

            RandomCollection<String> surnames = readSurnames();

            for (int i = 0; i < 1000; i++) {
                String surname = surnames.next();
                String givenName = givenNames.next();
                String ldif = getLdif(i, givenName, surname);
                // System.out.println(ldif);
                writer.write(ldif);
                writer.write("\n");
            }

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getUid(String givenName, String surname) {
        String name = givenName + surname;
        name = name.replaceAll("[\\s-']", "");

        String uid = RandomStringUtils.random(3, name).toLowerCase() + RandomStringUtils.randomNumeric(3);

        while (uids.contains(uid)) {
            uid = RandomStringUtils.random(3, name).toLowerCase() + RandomStringUtils.randomNumeric(3);
        }

        uids.add(uid);

        return uid;
    }

    public static String getLdif(int i, String givenName, String surname) {

        String uid = getUid(givenName, surname);

        StringBuilder ldif = new StringBuilder();

        ldif.append("dn: uid=" + uid + ",ou=people,dc=example,dc=edu\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: person\n");
        ldif.append("objectClass: organizationalPerson\n");
        ldif.append("objectClass: inetOrgPerson\n");
        ldif.append("cn: " + givenName + " " + surname + "\n");
        ldif.append("givenName: " + givenName + "\n");
        ldif.append("surname: " + surname + "\n");
        ldif.append("uid: " + uid + "\n");

        return ldif.toString();
    }

    public static RandomCollection<String> readGivenNames() throws IOException {

        File file = PSPUtil.getFile(GIVENNAME_FILE);
        if (file == null) {
            throw new IllegalArgumentException("Unable to find '" + GIVENNAME_FILE + "'");
        }

        CreateLdapNames createLdapNames = new CreateLdapNames();
        RandomCollection<String> givenNames = createLdapNames.new RandomCollection<String>();

        BufferedReader in = new BufferedReader(new FileReader(file));
        String str = null;
        while ((str = in.readLine()) != null) {
            if (!str.startsWith("#")) {
                String name = DatatypeHelper.safeTrim(str);
                if (name != null) {
                    givenNames.add(1, name);
                }
            }
        }
        in.close();
        return givenNames;
    }

    public static RandomCollection<String> readSurnames() throws IOException {

        File file = PSPUtil.getFile(SURNAME_FILE);
        if (file == null) {
            throw new IllegalArgumentException("Unable to find '" + SURNAME_FILE + "'");
        }

        CreateLdapNames createLdapNames = new CreateLdapNames();
        RandomCollection<String> surnames = createLdapNames.new RandomCollection<String>();

        BufferedReader in = new BufferedReader(new FileReader(file));
        String str = null;
        while ((str = in.readLine()) != null) {
            if (!str.startsWith("#")) {
                String line = DatatypeHelper.safeTrim(str);
                if (line != null) {
                    String[] toks = line.split(",");
                    String surname = WordUtils.capitalizeFully(toks[0]);
                    String prop100k = toks[3];
                    surnames.add(Double.parseDouble(prop100k) / 100000.0, surname);
                }
            }
        }
        in.close();
        return surnames;
    }

    /**
     * 
     * From http://stackoverflow.com/questions/6409652/random-weighted-selection-java-framework.
     * 
     * @param <E>
     */
    public class RandomCollection<E> {
        private final NavigableMap<Double, E> map = new TreeMap<Double, E>();

        private final Random random;

        private double total = 0;

        public RandomCollection() {
            this(new Random());
        }

        public RandomCollection(Random random) {
            this.random = random;
        }

        public void add(double weight, E result) {
            if (weight <= 0)
                return;
            total += weight;
            map.put(total, result);
        }

        public E next() {
            double value = random.nextDouble() * total;
            return map.ceilingEntry(value).getValue();
        }
    }

}

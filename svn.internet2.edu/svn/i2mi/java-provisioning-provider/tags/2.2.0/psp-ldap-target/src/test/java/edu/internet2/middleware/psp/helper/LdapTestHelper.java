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

package edu.internet2.middleware.psp.helper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import junit.framework.Assert;

import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.opensaml.util.resource.PropertyReplacementResourceFilter;
import org.opensaml.util.resource.ResourceException;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.psp.ldap.LdapSpmlTarget;
import edu.vt.middleware.ldap.Ldap;
import edu.vt.middleware.ldap.SearchFilter;
import edu.vt.middleware.ldap.bean.LdapResult;
import edu.vt.middleware.ldap.bean.SortedLdapBeanFactory;
import edu.vt.middleware.ldap.ldif.Ldif;

public class LdapTestHelper {

  private static final Logger LOG = LoggerFactory.getLogger(LdapTestHelper.class);

  /**
   * Rewrite the given string containing macros of the form ${key} with the properties
   * from the given property file.
   * 
   * @param ldif
   * @param propertiesFile
   * @return
   * @throws IOException
   * @throws ResourceException
   */
  public static String applyFilter(String ldif, File propertiesFile) throws IOException,
      ResourceException {
    if (propertiesFile == null) {
      return ldif;
    }

    PropertyReplacementResourceFilter filter = new PropertyReplacementResourceFilter(
        propertiesFile);
    return DatatypeHelper.inputstreamToString(filter
        .applyFilter(new ByteArrayInputStream(ldif.getBytes())), null);
  }

  /**
   * Get a map with keys objectclass names and values the names of the attributes that are
   * present for each object of the given objectclass.
   * 
   * @param ldif
   * @return
   * @throws NamingException
   */
  public static Map<String, Collection<String>> buildObjectlassAttributeMap(
      Collection<LdifEntry> ldifEntries) throws NamingException {
    Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();

    for (LdifEntry ldifEntry : ldifEntries) {
      Set<String> objectclasses = new HashSet<String>();
      Set<String> attributeIds = new HashSet<String>();

      if (!ldifEntry.isEntry()) {
        LOG.trace("Unable to parse LdifEntry as an Entry {}", ldifEntry);
        return null;
      }
      Entry entry = ldifEntry.getEntry();

      Iterator<EntryAttribute> iterator = entry.iterator();
      while (iterator.hasNext()) {
        EntryAttribute entryAttribute = iterator.next();
        String entryAttributeId = entryAttribute.getId();
        if (entryAttributeId.equalsIgnoreCase("objectclass")) {
          Iterator<Value<?>> values = entryAttribute.getAll();
          while (values.hasNext()) {
            Value<?> value = values.next();
            if (value.getString().equals("top")) {
              continue;
            }
            objectclasses.add(value.getString());
          }
        }
        attributeIds.add(entryAttributeId);
      }
      for (String objectclass : objectclasses) {
        if (!map.containsKey(objectclass)) {
          map.put(objectclass, new HashSet<String>());
        }
        map.get(objectclass).addAll(attributeIds);
      }
    }
    return map;
  }

  /**
   * see {@link #buildObjectlassAttributeMap(Collection)}
   * 
   * @param ldif
   * @return
   * @throws NamingException
   */
  public static Map<String, Collection<String>> buildObjectlassAttributeMap(String ldif)
      throws NamingException {
    LdifReader reader = new LdifReader();
    return buildObjectlassAttributeMap(reader.parseLdif(ldif));
  }

  /**
   * Destroy everything under the given base.
   * 
   * @param baseDn
   * @param ldap
   * @throws NamingException
   */
  public static void deleteChildren(String baseDn, Ldap ldap) throws NamingException {
    LOG.debug("base " + baseDn);
    List<String> toDelete = LdapTestHelper.getChildDNs(baseDn, ldap, false);
    LOG.debug("childs " + toDelete);
    for (String dn : toDelete) {
      LOG.info("delete '{}'", dn);
      ldap.delete(LdapSpmlTarget.escapeForwardSlash(dn));
    }
  }

  /**
   * Delete the ldap entry with the given cn.
   * 
   * @param cn the cn
   * @param ldap the ldap connection
   * @throws NamingException
   */
  public static void deleteCn(String cn, Ldap ldap) throws NamingException {

    Iterator<SearchResult> results = ldap.search(new SearchFilter("cn=" + cn));
    while (results.hasNext()) {
      SearchResult result = results.next();
      ldap.delete(result.getName());
    }
  }
  
  /**
   * Return a list of child DNs under the given DN, in (reverse) order suitable for
   * deletion. This method requires the use of the FqdnSearchResultHandler.
   * 
   * @param dn
   *          the dn to delete, as well as all children
   * @param ldap
   *          the ldap connection
   * @return
   * @throws NamingException
   */
  public static List<String> getChildDNs(String dn, Ldap ldap) throws NamingException {
    return getChildDNs(dn, ldap, true);
  }
  
  /**
   * Return a list of child DNs under the given DN either in ascending or descending order (suitable for deletion). This
   * method requires the use of the FqdnSearchResultHandler.
   * 
   * @param baseDn the base DN to include as well as child DNs
   * @param ldap the ldap connection
   * @param decendingOrder true to indicate descending order, false to indicate ascending order
   * @return
   * @throws NamingException
   */
  public static List<String> getChildDNs(String baseDn, Ldap ldap, boolean decendingOrder) throws NamingException {

    ArrayList<LdapName> ldapNames = new ArrayList<LdapName>();

    SearchControls searchControls = new SearchControls();
    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    searchControls.setReturningAttributes(new String[] {});

    Iterator<SearchResult> results = ldap.search(LdapSpmlTarget.escapeForwardSlash(baseDn),
        new SearchFilter("objectclass=*"), searchControls);

    while (results.hasNext()) {
      ldapNames.add(new LdapName(results.next().getName()));
    }

    ldapNames.remove(new LdapName(baseDn));

    Collections.sort(ldapNames);

    if (!decendingOrder) {
      Collections.reverse(ldapNames);
    }

    ArrayList<String> dns = new ArrayList<String>();
    for (LdapName ldapName : ldapNames) {
      dns.add(ldapName.toString());
    }

    return dns;
  }

  /**
   * Return an LDIF representation of the LDAP DIT starting at the given base DN.
   * 
   * @param baseDn
   *          the base of the LDAP DIT
   * @param ldap
   *          <code>Ldap</code>
   * @return LDIF
   * @throws NamingException
   */
  public static String getCurrentLdif(String baseDn, Ldap ldap) throws NamingException {
    return getCurrentLdif(baseDn, null, ldap);
  }

  /**
   * Return an LDIF representation of the LDAP DIT starting at the given base DN.
   * 
   * @param baseDn
   *          the base of the LDAP DIT
   * @param ldap
   *          <code>Ldap</code>
   * @param descendingOrder
   *          true to indicate descending order, false to indicate ascending order
   * @return LDIF
   * @throws NamingException
   */
  public static String getCurrentLdif(String baseDn, Ldap ldap, boolean descendingOrder) throws NamingException {
    return getCurrentLdif(baseDn, null, ldap, descendingOrder);
  }

  /**
   * Return an LDIF representation of the LDAP DIT starting at the given base DN and
   * consisting of the specified attributes.
   * 
   * @param baseDn
   *          the base of the LDAP DIT
   * @param attrIds
   *          the names of the attributes to be included
   * @param ldap
   *          <code>Ldap</code>
   * @return LDIF
   * @throws NamingException
   */
  public static String getCurrentLdif(String baseDn, String[] attrIds, Ldap ldap)
      throws NamingException {

    List<String> currentDns = LdapTestHelper.getChildDNs(baseDn, ldap);

    SortedLdapBeanFactory factory = new SortedLdapBeanFactory();
    LdapResult result = factory.newLdapResult();

    for (String currentDn : currentDns) {
      // LOG.debug("currentDn '{}'", currentDn);

      Iterator<SearchResult> rs = (LdapTestHelper.searchEntryDn(ldap, LdapSpmlTarget.escapeForwardSlash(currentDn), attrIds));
      while (rs.hasNext()) {
        SearchResult r = rs.next();
        // TODO must be a better way to handle cn=group\/F,
        r.setName(LdapSpmlTarget.unescapeForwardSlash(r.getName()));
        result.addEntry(r);
      }
    }

    Ldif l = new Ldif();
    l.setLdapBeanFactory(factory);
    return l.createLdif(result);
  }
  
  /**
   * Return an LDIF representation of the LDAP DIT starting at the given base DN and consisting of the specified
   * attributes.
   * 
   * @param baseDn the base of the LDAP DIT
   * @param attrIds the names of the attributes to be included
   * @param ldap the ldap connection
   * @param descendingOrder true to indicate descending order, false to indicate ascending order
   * @return the LDIF
   * @throws NamingException
   */
  public static String getCurrentLdif(String baseDn, String[] attrIds, Ldap ldap, boolean descendingOrder)
      throws NamingException {

    List<String> currentDns = LdapTestHelper.getChildDNs(baseDn, ldap, descendingOrder);

    SortedOrderedResultLdapBeanFactory factory = new SortedOrderedResultLdapBeanFactory();
    LdapResult result = factory.newLdapResult();

    for (String currentDn : currentDns) {
      LOG.debug("currenDn '{}'", currentDn);

      Iterator<SearchResult> rs = (LdapTestHelper.searchEntryDn(ldap, LdapSpmlTarget.escapeForwardSlash(currentDn), attrIds));
      while (rs.hasNext()) {
        SearchResult r = rs.next();
        // TODO must be a better way to handle cn=group\/F,
        r.setName(LdapSpmlTarget.unescapeForwardSlash(r.getName()));
        result.addEntry(r);
      }
    }

    Ldif l = new Ldif();
    l.setLdapBeanFactory(factory);
    return l.createLdif(result);
  }

  /**
   * Create entries read from the given ldif file.
   * 
   * @param file
   * @param ldap
   * @throws NamingException
   */
  public static void loadLdif(File file, Ldap ldap) throws NamingException {

    LdifReader ldifReader = new LdifReader(file);
    for (LdifEntry entry : ldifReader) {
      Attributes attributes = new BasicAttributes(true);
      for (EntryAttribute entryAttribute : entry.getEntry()) {
        BasicAttribute attribute = new BasicAttribute(entryAttribute.getId());
        Iterator<Value<?>> values = entryAttribute.getAll();
        while (values.hasNext()) {
          attribute.add(values.next().get());
        }
        attributes.put(attribute);
      }
      LOG.debug("creating '" + entry.getDn().toString() + " " + attributes);
      ldap.create(entry.getDn().toString(), attributes);
    }
  }

  public static void loadLdif(File ldifFile, File replacementPropertiesFile, Ldap ldap)
      throws Exception {
    loadLdif(new FileInputStream(ldifFile), replacementPropertiesFile, ldap);
  }

  public static void loadLdif(String ldif, File replacementPropertiesFile, Ldap ldap)
      throws Exception {
    loadLdif(new ByteArrayInputStream(ldif.getBytes()), replacementPropertiesFile, ldap);
  }

  public static void loadLdif(InputStream ldif, File replacementPropertiesFile, Ldap ldap)
      throws Exception {

    LdifReader ldifReader = null;
    if (replacementPropertiesFile != null) {
      PropertyReplacementResourceFilter prf = new PropertyReplacementResourceFilter(
          replacementPropertiesFile);
      ldifReader = new LdifReader(prf.applyFilter(ldif));
    } else {
      ldifReader = new LdifReader(ldif);
    }

    for (LdifEntry entry : ldifReader) {
      Attributes attributes = new BasicAttributes(true);
      if (entry.isChangeAdd()) {
        for (EntryAttribute entryAttribute : entry.getEntry()) {
          BasicAttribute attribute = new BasicAttribute(entryAttribute.getId());
          Iterator<Value<?>> values = entryAttribute.getAll();
          while (values.hasNext()) {
            attribute.add(values.next().get());
          }
          attributes.put(attribute);
        }
        LOG.debug("creating '" + entry.getDn().toString() + " " + attributes);
        ldap.create(LdapSpmlTarget.escapeForwardSlash(entry.getDn().toString()), attributes);
      } else if (entry.isChangeModify()) {
        // nice, ApacheDS really makes this easy, maybe 0 == 0 next time.
        List<ModificationItem> mods = new ArrayList<ModificationItem>();
        for (Modification modification : entry.getModificationItems()) {
          if (modification.getOperation().equals(ModificationOperation.ADD_ATTRIBUTE)) {
            mods.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, AttributeUtils
                .toAttribute(modification.getAttribute())));
          } else if (modification.getOperation().equals(
              ModificationOperation.REMOVE_ATTRIBUTE)) {
            mods.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, AttributeUtils
                .toAttribute(modification.getAttribute())));
          } else if (modification.getOperation().equals(
              ModificationOperation.REPLACE_ATTRIBUTE)) {
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, AttributeUtils
                .toAttribute(modification.getAttribute())));
          }
        }
        LOG.debug("modifying '" + entry.getDn().toString() + " " + mods);
        ldap.modifyAttributes(LdapSpmlTarget.escapeForwardSlash(entry.getDn().toString()), mods
            .toArray(new ModificationItem[] {}));
      } else {
        throw new RuntimeException("Unhandled entry type : " + entry.getChangeType());
      }
    }
  }

  /**
   * Normalize values as DNs for every attribute of the given Entry which matches a given
   * attribute name. Probably this method should use the ApacheDS Normalization.
   * 
   * @param entry
   * @param attributeNames
   * @throws NamingException
   */
  public static void normalizeDNValues(Entry entry, Collection<String> dnAttributeNames)
      throws NamingException {
    Iterator<EntryAttribute> iterator = entry.iterator();
    while (iterator.hasNext()) {
      EntryAttribute entryAttribute = iterator.next();
      normalizeDNValues(entryAttribute, dnAttributeNames);
    }
  }
  
  /**
   * Normalize values as DNs for the given attribute if the attribute's name matches the collection of names. Probably
   * this method should use the ApacheDS Normalization.
   * 
   * @param entryAttribute
   * @param dnAttributeNames
   * @throws InvalidNameException
   */
  public static void normalizeDNValues(EntryAttribute entryAttribute, Collection<String> dnAttributeNames)
      throws InvalidNameException {
    Set<String> toAdd = new HashSet<String>();
    Set<String> toRemove = new HashSet<String>();
    if (dnAttributeNames.contains(entryAttribute.getId())) {
      Iterator<Value<?>> valueIterator = entryAttribute.getAll();
      while (valueIterator.hasNext()) {
        Value<?> value = valueIterator.next();
        String oldValue = value.getString();
        String newValue = new LdapDN(value.get().toString()).toNormName();
        if (!oldValue.equals(newValue)) {
          toRemove.add(value.getString());
          toAdd.add(new LdapDN(value.get().toString()).toNormName());
        }
      }
    }
    if (!toAdd.isEmpty()) {
      entryAttribute.add(toAdd.toArray(new String[] {}));
    }
    if (!toRemove.isEmpty()) {
      entryAttribute.remove(toRemove.toArray(new String[] {}));
    }
  }

  /**
   * see {@link #normalizeDNValues(Entry, Collection)}
   * 
   * @param ldifEntries
   * @param dnAttributeNames
   * @throws NamingException
   */
  public static void normalizeDNValues(Collection<LdifEntry> ldifEntries, Collection<String> dnAttributeNames)
      throws NamingException {
    for (LdifEntry ldifEntry : ldifEntries) {
      if (ldifEntry.isEntry()) {
        normalizeDNValues(ldifEntry.getEntry(), dnAttributeNames);
      } else if (ldifEntry.isChangeModify()) {
        for (Modification modification : ldifEntry.getModificationItems()) {
          normalizeDNValues(modification.getAttribute(), dnAttributeNames);
        }
      }
    }
  }

  /**
   * Remove from the entry any attribute which is not contained in the given collection of
   * attribute names.
   * 
   * @param entry
   * @param attributeNamesToKeep
   * @throws NamingException
   */
  public static void purgeAttributes(Entry entry, Collection<String> attributeNamesToKeep)
      throws NamingException {
    if (attributeNamesToKeep == null) {
      return;
    }

    Set<String> attrNames = new HashSet<String>();
    for (String attributeName : attributeNamesToKeep) {
      attrNames.add(attributeName.toLowerCase());
    }

    List<EntryAttribute> entryAttributesToRemove = new ArrayList<EntryAttribute>();
    Iterator<EntryAttribute> iterator = entry.iterator();
    while (iterator.hasNext()) {
      EntryAttribute entryAttribute = iterator.next();
      if (!attrNames.contains(entryAttribute.getId().toLowerCase())) {
        entryAttributesToRemove.add(entryAttribute);
      }
    }
    for (EntryAttribute entryAttributeToRemove : entryAttributesToRemove) {
      entry.remove(entryAttributeToRemove);
    }
  }

  /**
   * Remove attributes from the entry which are not in the supplied map.
   * 
   * see {@link #purgeAttributes(Entry, Collection)}.
   * 
   * see {@link #buildObjectlassAttributeMap(BufferedReader)}
   * 
   * @param ldifEntries
   * @param objectclassAttributeMap
   * @throws NamingException
   */
  public static void purgeAttributes(Collection<LdifEntry> ldifEntries,
      Map<String, Collection<String>> objectclassAttributeMap) throws NamingException {
    if (objectclassAttributeMap == null) {
      return;
    }
    for (LdifEntry ldifEntry : ldifEntries) {
      if (ldifEntry.isEntry()) {
        Set<String> attributeNamesToKeep = new HashSet<String>();
        for (String objectclass : objectclassAttributeMap.keySet()) {
          if (ldifEntry.getEntry().hasObjectClass(objectclass)) {
            attributeNamesToKeep.addAll(objectclassAttributeMap.get(objectclass));
          }
        }
        purgeAttributes(ldifEntry.getEntry(), attributeNamesToKeep);
      }
    }
  }

  /**
   * Remove any attribute whose name is "objectclass" and value is "top".
   * 
   * @param ldifEntries
   * @throws NamingException
   */
  public static void purgeObjectclassTop(Collection<LdifEntry> ldifEntries)
      throws NamingException {
    for (LdifEntry ldifEntry : ldifEntries) {
      if (ldifEntry.isEntry()) {
        Entry entry = ldifEntry.getEntry();
        if (entry.contains("objectclass", "top")) {
          entry.remove("objectclass", "top");
        }
      }
    }
  }
  
  /**
   * Remove any attribute whose name is "entryDN".
   * 
   * @param ldifEntries
   * @throws NamingException
   */
  public static void purgeEntryDN(Collection<LdifEntry> ldifEntries)
      throws NamingException {
    for (LdifEntry ldifEntry : ldifEntries) {
      if (ldifEntry.isEntry()) {
        Entry entry = ldifEntry.getEntry();        
        if (entry.contains("entryDN", entry.getDn().toString())) {
          entry.remove("entryDN", entry.getDn().toString());
        }
      }
    }
  }

  /**
   * Return the contents of the given file as a string.
   * 
   * @param file
   * @return
   */
  public static String readFile(File file) {

    StringBuffer buffer = new StringBuffer();

    try {
      BufferedReader in = new BufferedReader(new FileReader(file));
      String str;
      while ((str = in.readLine()) != null) {
        buffer.append(str + System.getProperty("line.separator"));
      }
      in.close();
    } catch (IOException e) {
      Assert.fail("An error occurred : " + e.getMessage());
    }
    return buffer.toString();
  }
  
  /**
   * Perform a search for a given LDAP entry. The underlying LdapContext method used is
   * search(), rather than getAttributes(), so that SearchResultHandlers are used. The
   * filter is (objectclass=*), the base is the given dn, and the scope is OBJECT.
   * 
   * @param ldap
   * @param dn
   * @param retAttrs
   * @return
   * @throws NamingException
   */
  public static Iterator<SearchResult> searchEntryDn(Ldap ldap, String dn, String[] retAttrs) throws NamingException {

    SearchControls searchControls = new SearchControls();
    searchControls.setSearchScope(SearchControls.OBJECT_SCOPE);
    searchControls.setReturningAttributes(retAttrs);
    searchControls.setCountLimit(1);

    return ldap.search(dn, new SearchFilter("objectclass=*"), searchControls);
  }

  /**
   * Sort the given entries by DN or string LDIF representation.
   * 
   * @param ldifEntries
   * @return
   */
  public static List<LdifEntry> sortLdif(Collection<LdifEntry> ldifEntries) {

    ArrayList<LdifEntry> list = new ArrayList<LdifEntry>(ldifEntries);
    list.trimToSize();

    Collections.sort(list, new Comparator() {

      public int compare(Object o1, Object o2) {
        // first compare by DN
        int c = (((LdifEntry) o1).getDn()).compareTo(((LdifEntry) o2).getDn());
        if (c != 0) {
          return c;
        }
        // then compare by "ldif"
        return ((LdifEntry) o1).toString().compareTo(((LdifEntry) o2).toString());
      }
    });

    return list;
  }

  /**
   * Assert that the LDIF represented by the given strings are the same.
   * 
   * @param correct
   *          the correct LDIF
   * @param current
   *          the current LDIF
   * @param purgeAttributes
   *          ignore attributes not in the correct ldif
   * @throws NamingException
   * @throws FileNotFoundException
   * @throws IOException
   * @throws ResourceException
   */
  public static void verifyLdif(String correctLdif, String currentLdif,
      boolean purgeAttributes) throws NamingException, FileNotFoundException,
      IOException, ResourceException {
    verifyLdif(correctLdif, currentLdif, null, null, purgeAttributes);
  }

  /**
   * Assert that the LDIF represented by the given string is the same as the LDIF returned
   * from the LDAP server at the given base DN.
   * 
   * @param correctLdif
   *          the correct LDIF
   * @param propertiesFile
   *          properties file to be used for macro replacement
   * @param normalizeDnAttributes
   *          attribute names whose values are DNs
   * @param base
   *          DN at which comparison should begin
   * @param ldap
   *          connection to LDAP server
   * @param purgeAttributes
   * @throws IOException
   * @throws ResourceException
   * @throws NamingException
   */
  public static void verifyLdif(String correctLdif, File propertiesFile,
      Collection<String> normalizeDnAttributes, String base, Ldap ldap,
      boolean purgeAttributes) throws IOException, ResourceException, NamingException {

    // replace macros
    String filteredCorrectLdif = LdapTestHelper.applyFilter(correctLdif, propertiesFile);

    // get attribute ids to request
    String[] requestedAttributes = null;
    Map<String, Collection<String>> map = LdapTestHelper.buildObjectlassAttributeMap(filteredCorrectLdif);

    if (map != null) {
      Set<String> attrIds = new HashSet<String>();
      for (Collection<String> values : map.values()) {
        attrIds.addAll(values);
      }
      requestedAttributes = attrIds.toArray(new String[] {});
    }

    // get current ldif using requested attribute ids
    String currentLdif = LdapTestHelper.getCurrentLdif(base, requestedAttributes, ldap);

    LOG.debug("currentLdif\n{}", currentLdif);
    LOG.debug("correctLdif\n{}", correctLdif);
    
    // verify ldif
    LdapTestHelper.verifyLdif(correctLdif, currentLdif, propertiesFile, normalizeDnAttributes, purgeAttributes);
  }

  /**
   * Assert that the LDIF represented by the given strings are the same.
   * 
   * @param correct
   *          the correct LDIF
   * @param current
   *          the current LDIF
   * @param propertiesFile
   *          properties file to be used for macro replacement
   * @param normalizeDnAttributes
   *          attribute names whose values are DNs
   * @param purgeAttributes
   *          ignore attributes not in the correct ldif
   * @throws NamingException
   * @throws FileNotFoundException
   * @throws IOException
   * @throws ResourceException
   */
  public static void verifyLdif(String correctLdif, String currentLdif,
      File propertiesFile, Collection<String> normalizeDnAttributes,
      boolean purgeAttributes) throws NamingException, FileNotFoundException,
      IOException, ResourceException {
    InputStream correct = new ByteArrayInputStream(correctLdif.getBytes());
    InputStream current = new ByteArrayInputStream(currentLdif.getBytes());
    verifyLdif(correct, current, propertiesFile, normalizeDnAttributes, purgeAttributes);
  }

  /**
   * Assert that the LDIF represented by the given files are the same.
   * 
   * @param correct
   *          the correct LDIF
   * @param current
   *          the current LDIF
   * @param propertiesFile
   *          properties file to be used for macro replacement
   * @param normalizeDnAttributes
   *          attribute names whose values are DNs
   * @param purgeAttributes
   *          ignore attributes not in the correct ldif
   * @throws FileNotFoundException
   * @throws IOException
   * @throws ResourceException
   * @throws NamingException
   */
  public static void verifyLdif(File correctFile, File currentFile, File propertiesFile,
      Collection<String> normalizeDnAttributes, boolean purgeAttributes)
      throws FileNotFoundException, IOException, ResourceException, NamingException {
    InputStream correct = new FileInputStream(correctFile);
    InputStream current = new FileInputStream(currentFile);
    verifyLdif(correct, current, propertiesFile, normalizeDnAttributes, purgeAttributes);
  }

  /**
   * Assert that the LDIF represented by the given input streams are the same.
   * 
   * @param correct
   *          the correct LDIF
   * @param current
   *          the current LDIF
   * @param propertiesFile
   *          properties file to be used for macro replacement
   * @param normalizeDnAttributes
   *          attribute names whose values are DNs
   * @param purgeAttributes
   *          ignore attributes not in the correct ldif
   * @throws FileNotFoundException
   * @throws IOException
   * @throws ResourceException
   * @throws NamingException
   */
  public static void verifyLdif(InputStream correct, InputStream current,
      File propertiesFile, Collection<String> normalizeDnAttributes,
      boolean purgeAttributes) throws FileNotFoundException, IOException,
      ResourceException, NamingException {
    String correctLdif;
    String currentLdif;
    if (propertiesFile != null) {
      // replace macros
      PropertyReplacementResourceFilter filter = new PropertyReplacementResourceFilter(
          propertiesFile);
      correctLdif = DatatypeHelper.inputstreamToString(filter.applyFilter(correct), null);
      currentLdif = DatatypeHelper.inputstreamToString(filter.applyFilter(current), null);
    } else {
      correctLdif = DatatypeHelper.inputstreamToString(correct, null);
      currentLdif = DatatypeHelper.inputstreamToString(current, null);
    }

    // the ApacheDS reader
    LdifReader reader = new LdifReader();

    Collection<LdifEntry> correctEntries = reader.parseLdif(correctLdif);
    // LOG.debug("cur ldif\n{}", currentLdif);
    Collection<LdifEntry> currentEntries = reader.parseLdif(currentLdif);

    // remove objectclass: top
    purgeObjectclassTop(correctEntries);
    purgeObjectclassTop(currentEntries);

    // remove entryDN if present
    purgeEntryDN(correctEntries);
    purgeEntryDN(currentEntries);
    
    if (purgeAttributes) {
      Map<String, Collection<String>> objectClassAttributeMap = buildObjectlassAttributeMap(correctEntries);
      if (objectClassAttributeMap != null) {
        purgeAttributes(correctEntries, objectClassAttributeMap);
        purgeAttributes(currentEntries, objectClassAttributeMap);
      }
    }

    // normalize dn values
    if (normalizeDnAttributes != null) {
      normalizeDNValues(correctEntries, normalizeDnAttributes);
      normalizeDNValues(currentEntries, normalizeDnAttributes);
    }

    verifyLdif(correctEntries, currentEntries);
  }

  /**
   * Verify that the given ldif entries are equal.
   * 
   * @param correctEntries
   * @param currentEntries
   */
  public static void verifyLdif(Collection<LdifEntry> correctEntries,
      Collection<LdifEntry> currentEntries) {

    List<LdifEntry> correctList = LdapTestHelper.sortLdif(correctEntries);
    List<LdifEntry> currentList = LdapTestHelper.sortLdif(currentEntries);

    Assert.assertEquals(correctList.size(), currentList.size());

    for (int i = 0; i < correctList.size(); i++) {
      Assert.assertEquals(correctList.get(i), currentList.get(i));
    }

    for (int i = 0; i < currentList.size(); i++) {
      Assert.assertEquals(currentList.get(i), correctList.get(i));
    }
  }

 
  
  
}

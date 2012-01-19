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

package edu.internet2.middleware.psp.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import org.opensaml.xml.util.DatatypeHelper;
import org.openspml.v2.msg.spml.AddRequest;
import org.openspml.v2.msg.spml.AddResponse;
import org.openspml.v2.msg.spml.DeleteRequest;
import org.openspml.v2.msg.spml.DeleteResponse;
import org.openspml.v2.msg.spml.ErrorCode;
import org.openspml.v2.msg.spml.Extensible;
import org.openspml.v2.msg.spml.LookupRequest;
import org.openspml.v2.msg.spml.LookupResponse;
import org.openspml.v2.msg.spml.Modification;
import org.openspml.v2.msg.spml.ModificationMode;
import org.openspml.v2.msg.spml.ModifyRequest;
import org.openspml.v2.msg.spml.ModifyResponse;
import org.openspml.v2.msg.spml.PSO;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.openspml.v2.msg.spml.QueryClause;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spml.StatusCode;
import org.openspml.v2.msg.spmlref.HasReference;
import org.openspml.v2.msg.spmlref.Reference;
import org.openspml.v2.msg.spmlsearch.Query;
import org.openspml.v2.msg.spmlsearch.Scope;
import org.openspml.v2.msg.spmlsearch.SearchRequest;
import org.openspml.v2.msg.spmlsearch.SearchResponse;
import org.openspml.v2.profiles.dsml.DSMLAttr;
import org.openspml.v2.profiles.dsml.DSMLModification;
import org.openspml.v2.profiles.dsml.DSMLProfileException;
import org.openspml.v2.profiles.dsml.DSMLValue;
import org.openspml.v2.profiles.dsml.EqualityMatch;
import org.openspml.v2.profiles.dsml.Filter;
import org.openspml.v2.profiles.dsml.FilterItem;
import org.openspml.v2.util.Spml2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.psp.PspException;
import edu.internet2.middleware.psp.spml.config.Pso;
import edu.internet2.middleware.psp.spml.config.PsoIdentifyingAttribute;
import edu.internet2.middleware.psp.spml.config.PsoReferences;
import edu.internet2.middleware.psp.spml.provider.BaseSpmlTarget;
import edu.internet2.middleware.psp.spml.request.AlternateIdentifier;
import edu.internet2.middleware.psp.util.PSPUtil;
import edu.internet2.middleware.shibboleth.common.service.ServiceException;
import edu.internet2.middleware.subject.Source;
import edu.internet2.middleware.subject.provider.LdapSourceAdapter;
import edu.internet2.middleware.subject.provider.SourceManager;
import edu.vt.middleware.ldap.Ldap;
import edu.vt.middleware.ldap.SearchFilter;
import edu.vt.middleware.ldap.bean.LdapAttribute;
import edu.vt.middleware.ldap.bean.LdapAttributes;
import edu.vt.middleware.ldap.bean.LdapEntry;
import edu.vt.middleware.ldap.bean.LdapResult;
import edu.vt.middleware.ldap.bean.OrderedLdapBeanFactory;
import edu.vt.middleware.ldap.bean.SortedLdapBeanFactory;
import edu.vt.middleware.ldap.ldif.Ldif;
import edu.vt.middleware.ldap.ldif.LdifResultConverter;
import edu.vt.middleware.ldap.pool.LdapPool;
import edu.vt.middleware.ldap.pool.LdapPoolException;

/** An (incomplete) spmlv2 provisioning target which provisions an ldap directory. */
public class LdapSpmlTarget extends BaseSpmlTarget {

    /** Pattern matching an escaped JNDI special forward slash character. */
    private static Pattern escapedforwardSlashPattern = Pattern.compile("\\\\/");

    /** Pattern matching the JNDI special forward slash character. */
    private static Pattern forwardSlashPattern = Pattern.compile("([^\\\\])/");

    /** The OpenLDAP error code returned when removing the last value of the groupOfNames attribute. */
    public static final String GROUP_OF_NAMES_ERROR =
            "[LDAP: error code 65 - object class 'groupOfNames' requires attribute 'member']";

    /** The OpenLDAP error code returned when removing the last value of the groupOfUniqueNames attribute. */
    public static final String GROUP_OF_UNIQUE_NAMES_ERROR =
            "[LDAP: error code 65 - object class 'groupOfUniqueNames' requires attribute 'uniqueMember']";

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(LdapSpmlTarget.class);

    /**
     * Normalize LDAP DN using {@link org.apache.directory.shared.ldap.name.LdapDN}. This will convert RDN
     * attributeTypes to lowercase, which is of interest since Active Directory usually (?) returns attributeTypes
     * uppercased.
     * 
     * @param dn the ldap dn
     * @return the lowercased and normalized dn
     * @throws InvalidNameException if the dn is not a valid ldap name
     */
    public static String canonicalizeDn(String dn) throws InvalidNameException {
        return new LdapName(unescapeForwardSlash(dn)).toString();
    }

    /**
     * Escape all forward slashes "/" with "\/".
     * 
     * @param dn the ldap dn
     * @return the resultant string with / replaced with \/
     */
    public static String escapeForwardSlash(final String dn) {

        Matcher matcher = forwardSlashPattern.matcher(dn);

        if (matcher.find()) {
            return matcher.replaceAll("$1\\\\/");
        }

        return dn;
    }

    /**
     * Remove the escape character "\" from all escaped forward slashes "\/", returning "/".
     * 
     * @param dn the ldap dn
     * @return the resultant string
     */
    public static String unescapeForwardSlash(final String dn) {

        Matcher matcher = escapedforwardSlashPattern.matcher(dn);

        if (matcher.find()) {
            return matcher.replaceAll("/");
        }

        return dn;
    }

    /** the ldap pool. */
    private LdapPool<Ldap> ldapPool;

    /** The id of the ldap pool. */
    private String ldapPoolId;

    /** The source of the ldap pool id. */
    private String ldapPoolIdSource;

    /** Whether or not log log ldif. */
    private boolean logLdif;

    /** Constructor */
    public LdapSpmlTarget() {
    }

    /** {@inheritDoc} */
    public void execute(AddRequest addRequest, AddResponse addResponse) {

        try {
            handleEmptyReferences(addRequest);
        } catch (DSMLProfileException e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
            return;
        } catch (PspException e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
            return;
        }

        Ldap ldap = null;
        try {
            SortedLdapBeanFactory ldapBeanFactory = new SortedLdapBeanFactory();
            LdapAttributes ldapAttributes = ldapBeanFactory.newLdapAttributes();

            Extensible data = addRequest.getData();

            // data
            Map<String, DSMLAttr> dsmlAttrs = PSPUtil.getDSMLAttrMap(data);
            for (DSMLAttr dsmlAttr : dsmlAttrs.values()) {
                BasicAttribute basicAttribute = new BasicAttribute(dsmlAttr.getName());
                for (DSMLValue dsmlValue : dsmlAttr.getValues()) {
                    basicAttribute.add(dsmlValue.getValue());
                }
                LdapAttribute ldapAttribute = ldapBeanFactory.newLdapAttribute();
                ldapAttribute.setAttribute(basicAttribute);
                ldapAttributes.addAttribute(ldapAttribute);
            }

            // references
            Map<String, List<Reference>> references = PSPUtil.getReferences(addRequest.getCapabilityData());
            for (String typeOfReference : references.keySet()) {
                BasicAttribute basicAttribute = new BasicAttribute(typeOfReference);
                for (Reference reference : references.get(typeOfReference)) {
                    if (reference.getToPsoID().getTargetID().equals(getId())) {
                        String id = reference.getToPsoID().getID();
                        // fake empty string since the spml toolkit ignores an empty string psoID
                        if (id == null) {
                            id = "";
                        }
                        basicAttribute.add(id);
                    }
                }
                LdapAttribute ldapAttribute = ldapBeanFactory.newLdapAttribute();
                ldapAttribute.setAttribute(basicAttribute);
                ldapAttributes.addAttribute(ldapAttribute);
            }

            // create
            // assume the psoID is a DN
            String dn = addRequest.getPsoID().getID();
            String escapedDn = LdapSpmlTarget.escapeForwardSlash(dn);

            ldap = ldapPool.checkOut();

            LOG.debug("Target '{}' - Create '{}'", getId(), PSPUtil.toString(addRequest));
            LOG.debug("Target '{}' - Create DN '{}'", getId(), escapedDn);
            ldap.create(escapedDn, ldapAttributes.toAttributes());
            LOG.info("Target '{}' - Created '{}'", getId(), PSPUtil.toString(addRequest));

            if (this.isLogLdif()) {
                LdapEntry ldapEntry = ldapBeanFactory.newLdapEntry();
                ldapEntry.setDn(dn);
                ldapEntry.setLdapAttributes(ldapAttributes);
                LdapResult result = ldapBeanFactory.newLdapResult();
                result.addEntry(ldapEntry);
                Ldif ldif = new Ldif();
                LOG.info("Target '{}' - LDIF\n{}", getId(), ldif.createLdif(result));
            }

            // response PSO
            if (addRequest.getReturnData().equals(ReturnData.IDENTIFIER)) {
                PSO responsePSO = new PSO();
                responsePSO.setPsoID(addRequest.getPsoID());
                addResponse.setPso(responsePSO);
            } else {
                LookupRequest lookupRequest = new LookupRequest();
                lookupRequest.setPsoID(addRequest.getPsoID());
                lookupRequest.setReturnData(addRequest.getReturnData());

                LookupResponse lookupResponse = this.execute(lookupRequest);
                if (lookupResponse.getStatus() == StatusCode.SUCCESS) {
                    addResponse.setPso(lookupResponse.getPso());
                } else {
                    fail(addResponse, lookupResponse.getError(), "Unable to lookup object after create.");
                }
            }

        } catch (LdapPoolException e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (NameAlreadyBoundException e) {
            fail(addResponse, ErrorCode.ALREADY_EXISTS, e);
        } catch (NamingException e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (PspException e) {
            // from PSO.getReferences, an unhandled capability data
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
        } finally {
            ldapPool.checkIn(ldap);
        }
    }

    /** {@inheritDoc} */
    public void execute(DeleteRequest deleteRequest, DeleteResponse deleteResponse) {

        // TODO support recursive delete requests
        if (deleteRequest.isRecursive()) {
            fail(deleteResponse, ErrorCode.UNSUPPORTED_OPERATION, "Recursive delete requests are not yet supported.");
            return;
        }

        Ldap ldap = null;
        try {
            // delete
            String dn = deleteRequest.getPsoID().getID();
            String escapedDn = LdapSpmlTarget.escapeForwardSlash(dn);

            ldap = ldapPool.checkOut();

            LOG.debug("Target '{}' - Delete '{}'", getId(), PSPUtil.toString(deleteRequest));
            LOG.debug("Target '{}' - Delete DN '{}'", getId(), escapedDn);
            ldap.delete(escapedDn);
            LOG.info("Target '{}' - Deleted '{}'", getId(), PSPUtil.toString(deleteRequest));

        } catch (LdapPoolException e) {
            fail(deleteResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (NameNotFoundException e) {
            fail(deleteResponse, ErrorCode.NO_SUCH_IDENTIFIER, e);
        } catch (NamingException e) {
            fail(deleteResponse, ErrorCode.CUSTOM_ERROR, e);
        } finally {
            ldapPool.checkIn(ldap);
        }
    }

    /** {@inheritDoc} */
    public void execute(LookupRequest lookupRequest, LookupResponse lookupResponse) {

        Ldap ldap = null;
        try {
            // will not return AD Range option attrs
            // Attributes attributes = ldap.getAttributes(escapedDn, retAttrs);

            SearchFilter sf = new SearchFilter();
            sf.setFilter("objectclass=*");
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.OBJECT_SCOPE);

            // This lookup requests attributes defined for *all* objects.
            // Perhaps there should be two searches, one for the identifier
            // and a second for attributes.
            String[] retAttrs = getPSP().getNames(getId(), lookupRequest.getReturnData()).toArray(new String[] {});
            sc.setReturningAttributes(retAttrs);

            // TODO logging
            String dn = lookupRequest.getPsoID().getID();
            String escapedDn = LdapSpmlTarget.escapeForwardSlash(dn);

            ldap = ldapPool.checkOut();

            LOG.debug("Target '{}' - Searching '{}'", getId(), PSPUtil.toString(lookupRequest));
            Iterator<SearchResult> searchResults = ldap.search(escapedDn, sf, sc);
            LOG.debug("Target '{}' - Searched '{}'", getId(), PSPUtil.toString(lookupRequest));

            if (!searchResults.hasNext()) {
                fail(lookupResponse, ErrorCode.NO_SUCH_IDENTIFIER);
                return;
            }

            SearchResult result = searchResults.next();

            if (searchResults.hasNext()) {
                fail(lookupResponse, ErrorCode.CUSTOM_ERROR, "More than one result found.");
                return;
            }
            Attributes attributes = result.getAttributes();

            // return attributes in order defined by config
            OrderedLdapBeanFactory orderedLdapBeanFactory = new OrderedLdapBeanFactory();
            // sort values
            SortedLdapBeanFactory sortedLdapBeanFactory = new SortedLdapBeanFactory();

            LdapAttributes ldapAttributes = orderedLdapBeanFactory.newLdapAttributes();
            for (String retAttr : retAttrs) {
                Attribute attr = attributes.get(retAttr);
                if (attr != null) {
                    LdapAttribute ldapAttribute = sortedLdapBeanFactory.newLdapAttribute();
                    ldapAttribute.setAttribute(attr);
                    ldapAttributes.addAttribute(ldapAttribute);
                }
            }

            LdapEntry entry = sortedLdapBeanFactory.newLdapEntry();
            entry.setDn(dn);
            entry.setLdapAttributes(ldapAttributes);

            if (this.isLogLdif()) {
                LdapResult lr = sortedLdapBeanFactory.newLdapResult();
                lr.addEntry(entry);
                LdifResultConverter lrc = new LdifResultConverter();
                LOG.info("Target '{}' - LDIF\n{}", getId(), lrc.toLdif(lr));
            }

            // build pso
            lookupResponse.setPso(getPSO(entry, lookupRequest.getReturnData()));

        } catch (NameNotFoundException e) {
            fail(lookupResponse, ErrorCode.NO_SUCH_IDENTIFIER);
        } catch (LdapPoolException e) {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (InvalidNameException e) {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (NamingException e) {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (DSMLProfileException e) {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (Spml2Exception e) {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (PspException e) {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, e);
        } finally {
            if (ldap != null) {
                ldapPool.checkIn(ldap);
            }
        }
    }

    /** {@inheritDoc} */
    public void execute(ModifyRequest modifyRequest, ModifyResponse modifyResponse) {

        execute(modifyRequest, modifyResponse, true);
    }

    /**
     * Execute a modify request and optionally retry with the empty reference value if adding an empty reference is a
     * schema violation.
     * 
     * @param modifyRequest the modify request
     * @param modifyResponse the modify response
     * @param retry whether or not to retry with the empty reference value if adding an empty reference is a schema
     *            violation
     */
    public void execute(ModifyRequest modifyRequest, ModifyResponse modifyResponse, boolean retry) {

        Ldap ldap = null;
        try {
            String dn = modifyRequest.getPsoID().getID();

            List<AlternateIdentifier> alternateIdentifiers = new ArrayList<AlternateIdentifier>();
            List<ModificationItem> modificationItems = new ArrayList<ModificationItem>();
            for (Modification modification : modifyRequest.getModifications()) {
                modificationItems.addAll(getDsmlMods(modification));
                modificationItems.addAll(getReferenceMods(modification));
                alternateIdentifiers.addAll(PSPUtil.getAlternateIdentifiers(modification));
            }

            if (alternateIdentifiers.size() == 1) {
                AlternateIdentifier alternateIdentifier = alternateIdentifiers.get(0);
                if (!alternateIdentifier.getTargetID().equals(getId())) {
                    fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Unable to rename object with a different target ID.");
                    return;
                }
            }

            ldap = ldapPool.checkOut();

            PSOIdentifier responseLookupPsoID = modifyRequest.getPsoID();

            // rename
            if (alternateIdentifiers.size() == 1) {
                String oldDn = LdapSpmlTarget.escapeForwardSlash(dn);
                String newDn = LdapSpmlTarget.escapeForwardSlash(alternateIdentifiers.get(0).getID());
                LOG.info("Target '{}' - Renaming '{}' to '{}'", new Object[] {getId(), oldDn, newDn});
                ldap.rename(oldDn, newDn);
                dn = newDn;
                responseLookupPsoID = alternateIdentifiers.get(0).getPSOIdentifier();
            }

            // modify
            LOG.debug("Target '{}' - Modifying '{}'", getId(), PSPUtil.toString(modifyRequest));
            LOG.debug("Target '{}' - Modifications '{}'", getId(), modificationItems);
            String escapedDn = LdapSpmlTarget.escapeForwardSlash(dn);
            LOG.debug("Target '{}' - Modify DN '{}'", getId(), escapedDn);
            ldap.modifyAttributes(escapedDn, modificationItems.toArray(new ModificationItem[] {}));
            LOG.debug("Target '{}' - Modified '{}'", getId(), PSPUtil.toString(modifyRequest));

            // response PSO
            if (modifyRequest.getReturnData().equals(ReturnData.IDENTIFIER)) {
                PSO responsePSO = new PSO();
                responsePSO.setPsoID(responseLookupPsoID);
                // TODO entityName attribute ?
                modifyResponse.setPso(responsePSO);
            } else {
                LookupRequest lookupRequest = new LookupRequest();
                lookupRequest.setPsoID(responseLookupPsoID);
                lookupRequest.setReturnData(modifyRequest.getReturnData());

                LookupResponse lookupResponse = this.execute(lookupRequest);
                if (lookupResponse.getStatus() == StatusCode.SUCCESS) {
                    modifyResponse.setPso(lookupResponse.getPso());
                } else {
                    fail(modifyResponse, lookupResponse.getError());
                }
            }
        } catch (SchemaViolationException e) {

            // optionally retry after adding an empty reference if this is an openldap schema violation
            if (retry) {
                LOG.error("Target '{}' - A schema violation occurred {}", getId(), e);
                if (GROUP_OF_NAMES_ERROR.equals(e.getMessage()) || GROUP_OF_UNIQUE_NAMES_ERROR.equals(e.getMessage())) {
                    ModifyRequest emptyReference = null;
                    try {
                        emptyReference = handleEmptyReferences(modifyRequest);
                    } catch (PspException e1) {
                        fail(modifyResponse, ErrorCode.CUSTOM_ERROR, e1);
                        return;
                    }
                    if (emptyReference != null) {
                        LOG.info("Target '{}' - Retrying modify request", getId(), PSPUtil.toString(emptyReference));
                        execute(emptyReference, modifyResponse, false);
                    }
                }
            } else {
                // return the failure response
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR, e);
                return;
            }

        } catch (LdapPoolException e) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (NamingException e) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (PspException e) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, e);
        } finally {
            ldapPool.checkIn(ldap);
        }
    }

    /** {@inheritDoc} */
    public void execute(SearchRequest searchRequest, SearchResponse searchResponse) {

        // query
        Query query = searchRequest.getQuery();

        // query filter
        // TODO support QueryClause other than our own
        String filter = null;
        for (QueryClause queryClause : query.getQueryClauses()) {
            if (queryClause instanceof HasReference) {
                HasReference hasReference = (HasReference) queryClause;
                if (hasReference.getTypeOfReference() != null && hasReference.getToPsoID() != null
                        && hasReference.getToPsoID().getID() != null) {
                    filter = "(" + hasReference.getTypeOfReference() + "=" + hasReference.getToPsoID().getID() + ")";
                    // TODO what do we do with hasReference.getReferenceData(); ?
                }
            } else if (queryClause instanceof Filter) {
                FilterItem filterItem = ((Filter) queryClause).getItem();
                if (filterItem instanceof EqualityMatch) {
                    String name = ((EqualityMatch) filterItem).getName();
                    String value = ((EqualityMatch) filterItem).getValue().getValue();
                    filter = "(" + name + "=" + value + ")";
                }
            } else {
                fail(searchResponse, ErrorCode.MALFORMED_REQUEST, "Unsupported query.");
                return;
            }
        }
        if (DatatypeHelper.isEmpty(filter)) {
            fail(searchResponse, ErrorCode.MALFORMED_REQUEST, "A filter is required.");
            return;
        }

        // query base
        if (query.getBasePsoID() == null || query.getBasePsoID().getID() == null) {
            fail(searchResponse, ErrorCode.MALFORMED_REQUEST, "A basePsoID is required.");
            return;
        }
        String base = query.getBasePsoID().getID();

        SearchControls searchControls = new SearchControls();

        // query scope
        Scope scope = query.getScope();
        if (scope != null) {
            searchControls.setSearchScope(PSPUtil.getScope(scope));
        }

        ReturnData returnData = searchRequest.getReturnData();
        if (returnData == null) {
            returnData = ReturnData.EVERYTHING;
        }

        // attributes to return
        String[] retAttrs = getPSP().getNames(getId(), returnData).toArray(new String[] {});
        searchControls.setReturningAttributes(retAttrs);

        Ldap ldap = null;
        try {
            ldap = ldapPool.checkOut();

            LOG.debug("Target '{}' - Search will return attributes '{}'", getId(), Arrays.asList(retAttrs));
            LOG.debug("Target '{}' - Searching '{}'", getId(), PSPUtil.toString(searchRequest));
            Iterator<SearchResult> searchResults = ldap.search(base, new SearchFilter(filter), searchControls);
            LOG.debug("Target '{}' - Searched '{}'", getId(), PSPUtil.toString(searchRequest));

            SortedLdapBeanFactory ldapBeanFactory = new SortedLdapBeanFactory();
            LdapResult ldapResult = ldapBeanFactory.newLdapResult();
            ldapResult.addEntries(searchResults);

            Collection<LdapEntry> entries = ldapResult.getEntries();
            LOG.debug("Target '{}' - Search found {} entries.", getId(), entries.size());
            for (LdapEntry entry : entries) {
                searchResponse.addPSO(getPSO(entry, returnData));
            }

            if (logLdif) {
                Ldif ldif = new Ldif();
                LOG.info("Target '{}' - LDIF\n{}", getId(), ldif.createLdif(ldapResult));
            }

        } catch (NameNotFoundException e) {
            fail(searchResponse, ErrorCode.NO_SUCH_IDENTIFIER, e);
        } catch (NamingException e) {
            fail(searchResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (LdapPoolException e) {
            fail(searchResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (Spml2Exception e) {
            fail(searchResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (PspException e) {
            fail(searchResponse, ErrorCode.CUSTOM_ERROR, e);
        } finally {
            ldapPool.checkIn(ldap);
        }

    }

    /**
     * Converts spml modifications to jndi modifications.
     * 
     * @param modification the spml modification
     * @return the jndi modifications
     * @throws PspException if a psp error occurs
     */
    protected List<ModificationItem> getDsmlMods(Modification modification) throws PspException {
        List<ModificationItem> mods = new ArrayList<ModificationItem>();

        for (Object object : modification.getOpenContentElements(DSMLModification.class)) {
            DSMLModification dsmlModification = (DSMLModification) object;

            Attribute attribute = new BasicAttribute(dsmlModification.getName());

            DSMLValue[] dsmlValues = dsmlModification.getValues();
            for (DSMLValue dsmlValue : dsmlValues) {
                attribute.add(dsmlValue.getValue());
            }

            int op = -1;
            if (dsmlModification.getOperation().equals(ModificationMode.ADD)) {
                op = DirContext.ADD_ATTRIBUTE;
            } else if (dsmlModification.getOperation().equals(ModificationMode.DELETE)) {
                op = DirContext.REMOVE_ATTRIBUTE;
            } else if (dsmlModification.getOperation().equals(ModificationMode.REPLACE)) {
                op = DirContext.REPLACE_ATTRIBUTE;
            } else {
                throw new PspException("Unknown dsml modification operation : " + dsmlModification.getOperation());
            }

            mods.add(new ModificationItem(op, attribute));
        }

        return mods;
    }

    /**
     * Gets the ldap pool.
     * 
     * @return the ldap pool
     */
    public LdapPool<Ldap> getLdapPool() {
        return ldapPool;
    }

    /**
     * Gets the id of the ldap pool.
     * 
     * @return the id of the ldap pool
     */
    public String getLdapPoolId() {
        return ldapPoolId;
    }

    /**
     * Get the source of the ldap pool id.
     * 
     * @return Returns the source of the ldap pool id
     */
    public String getLdapPoolIdSource() {
        return ldapPoolIdSource;
    }

    /**
     * Gets the pso representation of the ldap entry.
     * 
     * @param entry the ldap entry
     * @param returnData whether or not to include the identifier, data, and references
     * @return the pso representation of the ldap entry
     * @throws Spml2Exception if an spml error occurs
     * @throws PspException if a psp error occurs
     */
    protected PSO getPSO(LdapEntry entry, ReturnData returnData) throws Spml2Exception, PspException {

        String msg = "get pso for '" + entry.getDn() + "' target '" + getId() + "'";

        PSO pso = new PSO();

        // determine schema entity
        // throws PSPException
        Pso psoDefinition = this.getPSODefinition(entry);
        LOG.debug("{} schema entity '{}'", msg, psoDefinition.getId());
        pso.addOpenContentAttr(Pso.ENTITY_NAME_ATTRIBUTE, psoDefinition.getId());

        PSOIdentifier psoID = new PSOIdentifier();
        psoID.setTargetID(getId());

        try {
            psoID.setID(LdapSpmlTarget.canonicalizeDn(entry.getDn()));
        } catch (InvalidNameException e) {
            LOG.error(msg + " Unable to canonicalize entry dn.", e);
            throw new Spml2Exception(e);
        }

        // TODO skipping container id for now
        // String baseId = psoDefinition.getPsoIdentifierDefinition().getBaseId();
        // if (baseId != null) {
        // PSOIdentifier containerID = new PSOIdentifier();
        // containerID.setID(baseId);
        // containerID.setTargetID(getId());
        // psoID.setContainerID(containerID);
        // }

        pso.setPsoID(psoID);

        LdapAttributes ldapAttributes = entry.getLdapAttributes();

        if (returnData.equals(ReturnData.DATA) || returnData.equals(ReturnData.EVERYTHING)) {
            // TODO this is ugly; ldap attribute names are case insensitive
            Map<String, String> attributeNameMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            for (String attributeName : psoDefinition.getAttributeNames()) {
                attributeNameMap.put(attributeName, attributeName);
            }
            Map<String, String> referenceNameMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            if (returnData.equals(ReturnData.EVERYTHING)) {
                for (String referenceName : psoDefinition.getReferenceNames()) {
                    referenceNameMap.put(referenceName, referenceName);
                }
            }

            Extensible data = new Extensible();
            List<Reference> references = new ArrayList<Reference>();

            for (LdapAttribute ldapAttribute : ldapAttributes.getAttributes()) {
                if (attributeNameMap.containsKey(ldapAttribute.getName())) {
                    data.addOpenContentElement(this.getDsmlAttr(attributeNameMap.get(ldapAttribute.getName()),
                            ldapAttribute.getStringValues()));
                } else if (returnData.equals(ReturnData.EVERYTHING)
                        && referenceNameMap.containsKey(ldapAttribute.getName())) {
                    references.addAll(this.getReferences(referenceNameMap.get(ldapAttribute.getName()),
                            ldapAttribute.getStringValues()));
                } else {
                    LOG.trace("{} ignoring attribute '{}'", msg, ldapAttribute.getName());
                }

                if (data.getOpenContentElements().length > 0) {
                    pso.setData(data);
                }
            }
            if (returnData.equals(ReturnData.EVERYTHING)) {
                PSPUtil.setReferences(pso, references);
            }
        }

        return pso;
    }

    /**
     * Determine the schema entity appropriate for the given <code>LdapEntry</code>.
     * 
     * @param entry the <code>LdapEntry</code>
     * @return the <code>PSODefintion</code>
     * @throws PspException if the schema entity cannot be determined.
     */
    protected Pso getPSODefinition(LdapEntry entry) throws PspException {

        Attributes attributes = entry.getLdapAttributes().toAttributes();

        Pso definition = null;

        for (Pso psoDefinition : getPSP().getPsos(getId())) {
            PsoIdentifyingAttribute ia = psoDefinition.getPsoIdentifyingAttribute();
            if (ia == null) {
                continue;
            }
            String idAttrName = ia.getName();
            String idAttrValue = ia.getValue();
            Attribute attribute = attributes.get(idAttrName);
            if (attribute != null && attribute.contains(idAttrValue)) {
                if (definition != null) {
                    LOG.error("More than one schema entity found for " + entry.getDn());
                    throw new PspException("More than one schema entity found for " + entry.getDn());
                }
                definition = psoDefinition;
            }
        }
        if (definition == null) {
            LOG.error("Unable to determine schema entity for " + entry.getDn());
            throw new PspException("Unable to determine schema entity for " + entry.getDn());
        }

        return definition;
    }

    /**
     * Converts spml modifications to jndi modifications.
     * 
     * @param modification the spml modification
     * @return the jndi modifications
     * @throws PspException if a psp error occurs
     */
    protected List<ModificationItem> getReferenceMods(Modification modification) throws PspException {
        List<ModificationItem> mods = new ArrayList<ModificationItem>();

        Map<String, List<Reference>> references = PSPUtil.getReferences(modification.getCapabilityData());

        if (references.isEmpty()) {
            return mods;
        }

        for (String typeOfReference : references.keySet()) {

            List<String> ids = new ArrayList<String>();
            for (Reference reference : references.get(typeOfReference)) {
                if (reference.getToPsoID().getTargetID().equals(getId())) {
                    String id = reference.getToPsoID().getID();
                    // fake empty string since the spml toolkit ignores an empty string psoID
                    // if (id.equals(PSOReferencesDefinition.EMPTY_STRING)) {
                    // id = "";
                    // }
                    if (id == null) {
                        id = "";
                    }
                    ids.add(id);
                }
            }

            Attribute attribute = new BasicAttribute(typeOfReference);
            for (String id : ids) {
                attribute.add(id);
            }

            int op = -1;
            if (modification.getModificationMode().equals(ModificationMode.ADD)) {
                op = DirContext.ADD_ATTRIBUTE;
            } else if (modification.getModificationMode().equals(ModificationMode.DELETE)) {
                op = DirContext.REMOVE_ATTRIBUTE;
            } else if (modification.getModificationMode().equals(ModificationMode.REPLACE)) {
                op = DirContext.REPLACE_ATTRIBUTE;
            } else {
                throw new PspException("Unknown modification operation : " + modification.getModificationMode());
            }

            mods.add(new ModificationItem(op, attribute));
        }

        return mods;
    }

    /**
     * Constructs references whose type is the given name and whose ids are the given values.
     * 
     * @param name the type of references
     * @param values the ids of the references
     * @return the references whose type is the given name and whose ids are the given values
     * @throws Spml2Exception if an spml error occurs
     */
    protected List<Reference> getReferences(String name, Collection<String> values) throws Spml2Exception {
        try {
            List<Reference> references = new ArrayList<Reference>();
            for (String value : values) {
                Reference reference = new Reference();
                PSOIdentifier toPSOId = new PSOIdentifier();
                toPSOId.setID(LdapSpmlTarget.canonicalizeDn(value));
                toPSOId.setTargetID(getId());

                // TODO containerID ?
                // PSOIdentifier containerID = new PSOIdentifier();
                // containerID.setID(LdapUtil.getParentDn(toPSOId.getID()));
                // containerID.setTargetID(getId());
                // toPSOId.setContainerID(containerID);

                reference.setToPsoID(toPSOId);
                reference.setTypeOfReference(name);
                references.add(reference);
            }
            return references;
        } catch (InvalidNameException e) {
            LOG.error("Unable to canonicalize name", e);
            throw new Spml2Exception(e);
        }
    }

    /**
     * Handle provisioning add requests with no references to a target which requires references to not be empty, such
     * as OpenLDAP.
     * 
     * @param addRequest the add request
     * @throws PspException if a psp error occurs
     * @throws DSMLProfileException if a dsml error occurs
     */
    protected void handleEmptyReferences(AddRequest addRequest) throws PspException, DSMLProfileException {

        if (!addRequest.getReturnData().equals(ReturnData.DATA)) {
            return;
        }

        String entityName = addRequest.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);
        if (entityName == null) {
            throw new PspException("Null entity name.");
        }

        Pso psoDefinition = getPSP().getPso(getId(), entityName);
        if (psoDefinition == null) {
            throw new PspException("Unknown pso.");
        }

        Map<String, DSMLAttr> dsmlAttrs = PSPUtil.getDSMLAttrMap(addRequest.getData());

        for (PsoReferences refsDef : psoDefinition.getReferences()) {
            String emptyValue = refsDef.getEmptyValue();
            if (emptyValue != null) {
                DSMLAttr member = dsmlAttrs.get(refsDef.getName());
                if (member == null || member.getValues().length == 0) {
                    addRequest.getData()
                            .addOpenContentElement(new DSMLAttr(refsDef.getName(), refsDef.getEmptyValue()));
                }
            }
        }
    }

    /**
     * Handle modify requests which attempt to delete the last value of the member or uniqueMember attribute of the
     * OpenLDAP groupOfNames and groupOfUniqueNames objectClass. If the modify request comprises one delete modification
     * of a reference attribute configured with an emptyValue, then first add a new reference whose ID is the empty
     * value before deleting the last value of the attribute.
     * 
     * @param modifyRequest the modify request
     * @return the modified request suitable for retry or null if all preconditions are not met
     * 
     * @throws PspException if capability data in the request which must be understood is not understood
     */
    protected ModifyRequest handleEmptyReferences(ModifyRequest modifyRequest) throws PspException {

        LOG.debug("Modify request before:\n{}", toXML(modifyRequest));

        // determine the pso definition name
        String entityName = modifyRequest.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);
        if (entityName == null) {
            LOG.error("Unable to determine entity " + PSPUtil.toString(modifyRequest));
            return null;
        }

        // the pso definition
        Pso psoDefinition = getPSP().getPso(getId(), entityName);
        if (psoDefinition == null) {
            LOG.error("Unable to determine provisioned object " + PSPUtil.toString(modifyRequest));
            return null;
        }

        // the original modification, should be a delete
        Modification[] modifications = modifyRequest.getModifications();
        if (modifications.length != 1) {
            LOG.debug("Only one modification is supported " + PSPUtil.toString(modifyRequest));
            return null;
        }
        Modification originalModification = modifications[0];
        if (!originalModification.getModificationMode().equals(ModificationMode.DELETE)) {
            LOG.debug("Only the delete modification mode is supported " + PSPUtil.toString(modifyRequest));
            return null;
        }

        // TODO exception

        // convert the modification into reference modifications
        List<ModificationItem> modificationItems = getReferenceMods(originalModification);
        if (modificationItems.size() != 1) {
            LOG.debug("Only one reference modification is supported " + PSPUtil.toString(modifyRequest));
            return null;
        }

        // the reference modification item
        ModificationItem modItem = modificationItems.get(0);

        // the references definition matching the reference modification
        PsoReferences refsDef = psoDefinition.getReferences(modItem.getAttribute().getID());
        if (refsDef == null) {
            LOG.debug("Unable to determine references definition " + PSPUtil.toString(modifyRequest));
            return null;
        }

        // the configured empty value
        String emptyValue = refsDef.getEmptyValue();
        if (emptyValue == null) {
            LOG.debug("An empty value is not configured for references definition '" + refsDef.getName() + "' "
                    + PSPUtil.toString(modifyRequest));
            return null;
        }

        // a reference to the empty value
        Reference reference = new Reference();
        reference.setToPsoID(new PSOIdentifier(emptyValue, null, getId()));
        reference.setTypeOfReference(refsDef.getName());

        // a modification adding the reference to the empty value
        Modification newModification = new Modification();
        try {
            newModification.addCapabilityData(PSPUtil.fromReferences(Arrays.asList(new Reference[] {reference})));
        } catch (Spml2Exception e) {
            LOG.error("Unable to add reference capability data " + PSPUtil.toString(modifyRequest), e);
            return null;
        }
        newModification.setModificationMode(ModificationMode.ADD);

        // clear modifications, add the reference to the empty value, and then re-add the original modification
        modifyRequest.clearModifications();
        modifyRequest.addModification(newModification);
        modifyRequest.addModification(originalModification);

        LOG.debug("Modify request after:\n{}", toXML(modifyRequest));

        return modifyRequest;
    }

    /**
     * Whether or not to log ldif.
     * 
     * @return whether or not to log ldif
     */
    public boolean isLogLdif() {
        return logLdif;
    }

    /** {@inheritDoc} */
    protected void onNewContextCreated(ApplicationContext newServiceContext) throws ServiceException {

        LdapPool<Ldap> oldPool = ldapPool;
        try {
            LOG.debug("Target '{}' - Loading ldap pool '{}'", getId(), getLdapPoolId());
            if (ldapPoolIdSource.equals("spring")) {
                LOG.debug("Target '{}' - Loading ldap pool '{}' from spring", getId(), getLdapPoolId());
                ldapPool = (LdapPool<Ldap>) newServiceContext.getBean(getLdapPoolId());
            }
            if (ldapPoolIdSource.equals("grouper")) {
                LOG.debug("Target '{}' - Loading ldap pool '{}' from grouper", getId(), getLdapPoolId());
                Source source = SourceManager.getInstance().getSource(getLdapPoolId());
                LdapSourceAdapter lsa = (LdapSourceAdapter) source;
                ldapPool = lsa.getLdapPool();
            }
        } catch (Exception e) {
            ldapPool = oldPool;
            LOG.error(getId() + " configuration is not valid, retaining old configuration", e);
            throw new ServiceException(getId() + " configuration is not valid, retaining old configuration", e);
        }
    }

    /** {@inheritDoc} */
    public Set<PSOIdentifier> orderForDeletion(final Set<PSOIdentifier> psoIdentifiers) throws PspException {

        // tree map keys are in ascending order, this will need to be reversed
        Map<LdapName, PSOIdentifier> map = new TreeMap<LdapName, PSOIdentifier>();

        try {
            for (PSOIdentifier psoIdentifier : psoIdentifiers) {
                LdapName ldapName = new LdapName(psoIdentifier.getID());
                map.put(ldapName, psoIdentifier);
            }
        } catch (InvalidNameException e) {
            LOG.error("An error occurred ordering the PSO identifiers.", e);
            throw new PspException(e);
        }

        // linked hash set to preserver insertion order
        Set<PSOIdentifier> psoIdsOrderedForDeletion = new LinkedHashSet<PSOIdentifier>();

        ArrayList<LdapName> ldapNames = new ArrayList<LdapName>(map.keySet());

        // reverse the order of the keys, suitable for deletion
        Collections.reverse(ldapNames);

        for (LdapName ldapName : ldapNames) {
            psoIdsOrderedForDeletion.add(map.get(ldapName));
        }

        if (LOG.isTraceEnabled()) {
            for (PSOIdentifier psoId : psoIdsOrderedForDeletion) {
                LOG.trace("correct pso id '{}'", PSPUtil.toString(psoId));
            }
        }

        return psoIdsOrderedForDeletion;
    }

    /**
     * Sets the id of the ldap pool.
     * 
     * @param ldapPoolId the id of the ldap pool
     */
    public void setLdapPoolId(String ldapPoolId) {
        this.ldapPoolId = ldapPoolId;
    }

    /**
     * Sets the source of the ldap pool id.
     * 
     * @param ldapPoolIdSource the source of the ldap pool id
     */
    public void setLdapPoolIdSource(String ldapPoolIdSource) {
        this.ldapPoolIdSource = ldapPoolIdSource;
    }

    /**
     * Sets whether or not to log ldif.
     * 
     * @param logLdif whether or not to log ldif
     */
    public void setLogLdif(boolean logLdif) {
        this.logLdif = logLdif;
    }
}

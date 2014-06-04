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

package edu.internet2.middleware.psp.grouper;

/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.opensaml.xml.util.DatatypeHelper;
import org.openspml.v2.msg.spml.AddRequest;
import org.openspml.v2.msg.spml.AddResponse;
import org.openspml.v2.msg.spml.CapabilityData;
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
import org.openspml.v2.msg.spml.Request;
import org.openspml.v2.msg.spml.Response;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spml.StatusCode;
import org.openspml.v2.msg.spmlref.HasReference;
import org.openspml.v2.msg.spmlref.Reference;
import org.openspml.v2.msg.spmlsearch.Query;
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

import edu.internet2.middleware.grouper.Field;
import edu.internet2.middleware.grouper.FieldFinder;
import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.GroupFinder;
import edu.internet2.middleware.grouper.GroupSave;
import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.Member;
import edu.internet2.middleware.grouper.Stem;
import edu.internet2.middleware.grouper.Stem.Scope;
import edu.internet2.middleware.grouper.StemFinder;
import edu.internet2.middleware.grouper.StemSave;
import edu.internet2.middleware.grouper.SubjectFinder;
import edu.internet2.middleware.grouper.exception.GroupAddException;
import edu.internet2.middleware.grouper.exception.GroupDeleteException;
import edu.internet2.middleware.grouper.exception.InsufficientPrivilegeException;
import edu.internet2.middleware.grouper.exception.SchemaException;
import edu.internet2.middleware.grouper.exception.StemAddException;
import edu.internet2.middleware.grouper.util.GrouperUtil;
import edu.internet2.middleware.psp.PspException;
import edu.internet2.middleware.psp.spml.config.Pso;
import edu.internet2.middleware.psp.spml.config.PsoIdentifyingAttribute;
import edu.internet2.middleware.psp.spml.provider.BaseSpmlTarget;
import edu.internet2.middleware.psp.spml.request.AlternateIdentifier;
import edu.internet2.middleware.psp.util.PSPUtil;
import edu.internet2.middleware.shibboleth.common.service.ServiceException;
import edu.internet2.middleware.subject.Subject;
import edu.internet2.middleware.subject.SubjectNotFoundException;
import edu.internet2.middleware.subject.SubjectNotUniqueException;

/** An (incomplete) spmlv2 provisioning target which provisions Grouper. */
public class GrouperSpmlTarget extends BaseSpmlTarget {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(GrouperSpmlTarget.class);

    /** The Grouper session. */
    private GrouperSession grouperSession;

    /**
     * {@inheritDoc}
     * 
     * Add groups or stems.
     * 
     * If the identifying attribute of the pso configuration has objectclass equal to group, then add as a group. If the
     * identifying attribute of the pso configuration has objectclass equal to stem, then add as a stem.
     */
    public void execute(AddRequest addRequest, AddResponse addResponse) {

        // Given an id, return a group, stem, or neither.
        Object object = getObject(addRequest.getPsoID().getID(), addRequest, addResponse);

        // if object already exists
        if (object != null) {
            fail(addResponse, ErrorCode.ALREADY_EXISTS);
            return;
        }

        // if there was a failure during lookup
        if (addResponse.getStatus().equals(StatusCode.FAILURE)) {
            return;
        }

        // the entity name
        String entityName = addRequest.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);

        // the pso definition
        Pso psoDefinition = getPSP().getPso(addRequest.getTargetId(), entityName);
        if (psoDefinition == null) {
            fail(addResponse, ErrorCode.MALFORMED_REQUEST);
            return;
        }

        // the identifying attribute
        PsoIdentifyingAttribute identifyingAttribute = psoDefinition.getPsoIdentifyingAttribute();
        if (identifyingAttribute == null) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, "An identifying attribute is required.");
            return;
        }
        if (!identifyingAttribute.getName().equalsIgnoreCase("objectclass")) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, "The identifying attribute value must be 'objectClass'.");
            return;
        }

        // if the identifying attribute objectclass = group, add as a group
        if (identifyingAttribute.getValue().equals("group")) {
            executeAddGroup(addRequest, addResponse);
            // if the identifying attribute objectclass = stem, add as a stem
        } else if (identifyingAttribute.getValue().equals("stem")) {
            executeAddStem(addRequest, addResponse);
        } else {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, "The identifying attribute value must be 'group' or 'stem'.");
            return;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Delete groups or stems.
     */
    public void execute(DeleteRequest deleteRequest, DeleteResponse deleteResponse) {

        // Given an id, return a group, stem, or neither.
        Object object = getObject(deleteRequest.getPsoID().getID(), deleteRequest, deleteResponse);

        if (object == null) {
            fail(deleteResponse, ErrorCode.NO_SUCH_IDENTIFIER);
            return;
        }

        if (deleteResponse.getStatus().equals(StatusCode.FAILURE)) {
            return;
        }

        if (object instanceof Group) {
            execute(deleteRequest, deleteResponse, (Group) object);
        } else if (object instanceof Stem) {
            execute(deleteRequest, deleteResponse, (Stem) object);
        } else {
            fail(deleteResponse, ErrorCode.CUSTOM_ERROR, "Unable to delete object of type " + object.getClass());
            return;
        }
    }

    /**
     * Delete a group.
     * 
     * @param deleteRequest the delete request
     * @param deleteResponse the delete response
     * @param group the group to delete
     */
    public void execute(DeleteRequest deleteRequest, DeleteResponse deleteResponse, Group group) {

        // TODO support recursive delete requests

        try {
            LOG.debug("Target '{}' - Deleting group '{}'", getId(), group);
            group.delete();
            LOG.info("Target '{}' - Deleted group '{}'", getId(), group);
        } catch (InsufficientPrivilegeException e) {
            fail(deleteResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (GroupDeleteException e) {
            fail(deleteResponse, ErrorCode.CUSTOM_ERROR, e);
        }
    }

    /**
     * Delete a stem.
     * 
     * @param deleteRequest the delete request
     * @param deleteResponse the delete response
     * @param stem the stem to delete
     */
    public void execute(DeleteRequest deleteRequest, DeleteResponse deleteResponse, Stem stem) {

        // TODO support recursive delete requests

        try {
            LOG.debug("Target '{}' - Deleting stem '{}'", getId(), stem);
            stem.delete();
            LOG.info("Target '{}' - Deleted stem '{}'", getId(), stem);
        } catch (InsufficientPrivilegeException e) {
            fail(deleteResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (GroupDeleteException e) {
            fail(deleteResponse, ErrorCode.CUSTOM_ERROR, e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Lookup a group or stem.
     */
    public void execute(LookupRequest lookupRequest, LookupResponse lookupResponse) {

        // Given an id, return a group, stem, or neither.
        Object object = getObject(lookupRequest.getPsoID().getID(), lookupRequest, lookupResponse);

        if (object == null) {
            fail(lookupResponse, ErrorCode.NO_SUCH_IDENTIFIER);
            return;
        }

        if (lookupResponse.getStatus().equals(StatusCode.FAILURE)) {
            return;
        }

        if (object instanceof Group) {
            execute(lookupRequest, lookupResponse, (Group) object);
        } else if (object instanceof Stem) {
            execute(lookupRequest, lookupResponse, (Stem) object);
        } else {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, "Unable to lookup object of type " + object.getClass());
            return;
        }
    }

    /**
     * Include the pso representation of the group in the response.
     * 
     * @param lookupRequest the lookup request
     * @param lookupResponse the lookup response
     * @param group the group
     */
    public void execute(LookupRequest lookupRequest, LookupResponse lookupResponse, Group group) {
        try {
            lookupResponse.setPso(getPSO(group, lookupRequest.getReturnData()));
        } catch (Spml2Exception e) {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (PspException e) {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, e);
        }
    }

    /**
     * Include the pso representation of the stem in the response.
     * 
     * @param lookupRequest the lookup request
     * @param lookupResponse the lookup response
     * @param stem the stem
     */
    public void execute(LookupRequest lookupRequest, LookupResponse lookupResponse, Stem stem) {
        try {
            lookupResponse.setPso(getPSO(stem, lookupRequest.getReturnData()));
        } catch (Spml2Exception e) {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (PspException e) {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Modify groups or stems.
     */
    public void execute(ModifyRequest modifyRequest, ModifyResponse modifyResponse) {

        // Given an id, return a group, stem, or neither.
        Object object = getObject(modifyRequest.getPsoID().getID(), modifyRequest, modifyResponse);

        if (object == null) {
            fail(modifyResponse, ErrorCode.NO_SUCH_IDENTIFIER);
            return;
        }

        if (modifyResponse.getStatus().equals(StatusCode.FAILURE)) {
            return;
        }

        if (object instanceof Group) {
            execute(modifyRequest, modifyResponse, (Group) object);
        } else if (object instanceof Stem) {
            execute(modifyRequest, modifyResponse, (Stem) object);
        } else {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Unable to modify object of type " + object.getClass());
            return;
        }
    }

    /**
     * Modify a group.
     * 
     * Renaming a group extension is supported. Moving a group is not supported.
     * 
     * Modification of description, displayExtension, and memberships are supported.
     * 
     * @param modifyRequest the modify request
     * @param modifyResponse the modify response
     * @param group the group to be modified
     */
    public void execute(ModifyRequest modifyRequest, ModifyResponse modifyResponse, Group group) {

        // determine type of modification
        List<AlternateIdentifier> alternateIdentifiers = new ArrayList<AlternateIdentifier>();
        List<DSMLModification> dsmlModifications = new ArrayList<DSMLModification>();
        List<Modification> referenceModifications = new ArrayList<Modification>();
        for (Modification modification : modifyRequest.getModifications()) {
            // alternate identifier modifications
            alternateIdentifiers.addAll(PSPUtil.getAlternateIdentifiers(modification));
            // data modifications
            for (Object mod : modification.getOpenContentElements(DSMLModification.class)) {
                dsmlModifications.add((DSMLModification) mod);
            }
            // reference modifications
            try {
                if (!PSPUtil.getReferences(modification.getCapabilityData()).isEmpty()) {
                    referenceModifications.add(modification);
                }
            } catch (PspException e) {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR, e);
                return;
            }
        }

        // rename
        if (alternateIdentifiers.size() == 1) {
            AlternateIdentifier alternateIdentifier = alternateIdentifiers.get(0);
            if (!alternateIdentifier.getTargetID().equals(getId())) {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Unable to rename object with a different target ID.");
                return;
            }

            String oldParentStemName = GrouperUtil.parentStemNameFromName(group.getName());
            String newParentStemName = GrouperUtil.parentStemNameFromName(alternateIdentifier.getID());

            // if old and new parent stems are the same, rename via extension
            if (oldParentStemName.equals(newParentStemName)) {
                String newExtension = GrouperUtil.extensionFromName(alternateIdentifier.getID());
                LOG.info("Target '{}' - Renaming '{}' to '{}'", new Object[] {getId(), group.getName(),
                        alternateIdentifier.getID(),});
                group.setExtension(newExtension, true);
                group.store();
            } else {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Unable to move group.");
                return;
            }
        }

        // data modifications
        for (DSMLModification dsmlModification : dsmlModifications) {
            if (dsmlModification.getName().equals("displayExtension")) {
                modifyDisplayExtension(dsmlModification, modifyResponse, group);
            } else if (dsmlModification.getName().equals("description")) {
                modifyDescription(dsmlModification, modifyResponse, group);
            } else {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Attribute '" + dsmlModification.getName()
                        + "' not supported.");
                return;
            }
        }

        // reference modifications
        for (Modification referenceModification : referenceModifications) {
            if (referenceModification.getModificationMode().equals(ModificationMode.REPLACE)) {
                LOG.error("Target '{}' - This target does not support 'replace' modifications.");
                fail(modifyResponse, ErrorCode.UNSUPPORTED_OPERATION);
                return;
            }
            modifyMemberships(group, referenceModification.getCapabilityData(), ModificationMode.ADD, modifyResponse);
            if (!modifyResponse.getStatus().equals(StatusCode.SUCCESS)) {
                return;
            }
        }
    }

    /**
     * Modify a stem.
     * 
     * Renaming a stem extension is supported. Moving a stem is not supported.
     * 
     * Modification of description and displayExtension are supported.
     * 
     * @param modifyRequest the modify request
     * @param modifyResponse the modify response
     * @param stem the stem to be modified
     */
    public void execute(ModifyRequest modifyRequest, ModifyResponse modifyResponse, Stem stem) {

        // determine type of modification
        List<AlternateIdentifier> alternateIdentifiers = new ArrayList<AlternateIdentifier>();
        List<DSMLModification> dsmlModifications = new ArrayList<DSMLModification>();

        for (Modification modification : modifyRequest.getModifications()) {
            // alternate identifier modifications
            alternateIdentifiers.addAll(PSPUtil.getAlternateIdentifiers(modification));
            // data modifications
            for (Object mod : modification.getOpenContentElements(DSMLModification.class)) {
                dsmlModifications.add((DSMLModification) mod);
            }
        }

        // rename
        if (alternateIdentifiers.size() == 1) {
            AlternateIdentifier alternateIdentifier = alternateIdentifiers.get(0);
            if (!alternateIdentifier.getTargetID().equals(getId())) {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Unable to rename object with a different target ID.");
                return;
            }

            String oldParentStemName = GrouperUtil.parentStemNameFromName(stem.getName());
            String newParentStemName = GrouperUtil.parentStemNameFromName(alternateIdentifier.getID());

            // if old and new parent stems are the same, rename via extension
            if (oldParentStemName.equals(newParentStemName)) {
                String newExtension = GrouperUtil.extensionFromName(alternateIdentifier.getID());
                LOG.info("Target '{}' - Renaming '{}' to '{}'", new Object[] {getId(), stem.getName(),
                        alternateIdentifier.getID(),});
                stem.setExtension(newExtension, true);
                stem.store();
            } else {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Unable to move stem.");
                return;
            }
        }

        // data modifications
        for (DSMLModification dsmlModification : dsmlModifications) {
            if (dsmlModification.getName().equals("displayExtension")) {
                modifyDisplayExtension(dsmlModification, modifyResponse, stem);
            } else if (dsmlModification.getName().equals("description")) {
                modifyDescription(dsmlModification, modifyResponse, stem);
            } else {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Attribute '" + dsmlModification.getName()
                        + "' not supported.");
                return;
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * The only currently supported search is an equality match on 'objectclass = group' or 'objectclass = stem'.
     */
    public void execute(SearchRequest searchRequest, SearchResponse searchResponse) {

        // query
        Query query = searchRequest.getQuery();

        // return data
        ReturnData returnData = searchRequest.getReturnData();
        if (returnData == null) {
            returnData = ReturnData.EVERYTHING;
        }

        // scope, default to subtree
        Scope scope = Scope.SUB;
        if (query.getScope() != null) {
            if (query.getScope().equals(org.openspml.v2.msg.spmlsearch.Scope.SUBTREE)) {
                scope = Scope.SUB;
            } else if (query.getScope().equals(org.openspml.v2.msg.spmlsearch.Scope.ONELEVEL)) {
                scope = Scope.ONE;
            } else if (query.getScope().equals(org.openspml.v2.msg.spmlsearch.Scope.PSO)) {
                fail(searchResponse, ErrorCode.MALFORMED_REQUEST, "Unsupported scope PSO.");
                return;
            }
        }

        // query base
        Stem baseStem = null;
        if (query.getBasePsoID() != null) {
            if (query.getBasePsoID().getTargetID() != null && !query.getBasePsoID().getTargetID().equals(getId())) {
                fail(searchResponse, ErrorCode.MALFORMED_REQUEST, "Unknown base target ID.");
                return;
            }
            baseStem = StemFinder.findByName(grouperSession, query.getBasePsoID().getID(), false);
            if (baseStem == null) {
                fail(searchResponse, ErrorCode.MALFORMED_REQUEST, "Unable to find base pso ID.");
                return;
            }
        }
        // use root stem as base if base pso id is not specified
        if (baseStem == null) {
            baseStem = StemFinder.findRootStem(grouperSession);
        }

        // query clause
        for (QueryClause queryClause : query.getQueryClauses()) {
            if (queryClause instanceof HasReference) {

                HasReference hasReference = (HasReference) queryClause;

                if (hasReference.getTypeOfReference() != null && hasReference.getToPsoID() != null
                        && hasReference.getToPsoID().getID() != null) {
                    // TODO implement
                }

                fail(searchResponse, ErrorCode.MALFORMED_REQUEST, "Unsupported query.");
                return;

            } else if (queryClause instanceof Filter) {

                FilterItem filterItem = ((Filter) queryClause).getItem();

                if (!(filterItem instanceof EqualityMatch)) {
                    fail(searchResponse, ErrorCode.MALFORMED_REQUEST, "The only supported filter is equality match.");
                    return;
                }

                String name = ((EqualityMatch) filterItem).getName();
                String value = ((EqualityMatch) filterItem).getValue().getValue();

                if (!name.equalsIgnoreCase("objectclass")) {
                    fail(searchResponse, ErrorCode.MALFORMED_REQUEST, "The only supported name is 'objectclass'.");
                    return;
                }

                if (value.equals("group")) {
                    Set<Group> groups = baseStem.getChildGroups(scope);
                    for (Group group : groups) {
                        try {
                            searchResponse.addPSO(getPSO(group, returnData));
                        } catch (Spml2Exception e) {
                            fail(searchResponse, ErrorCode.CUSTOM_ERROR, e);
                            return;
                        } catch (PspException e) {
                            fail(searchResponse, ErrorCode.CUSTOM_ERROR, e);
                            return;
                        }
                    }
                } else if (value.equals("stem")) {
                    Set<Stem> stems = baseStem.getChildStems(scope);
                    for (Stem stem : stems) {
                        try {
                            searchResponse.addPSO(getPSO(stem, returnData));
                        } catch (DSMLProfileException e) {
                            fail(searchResponse, ErrorCode.CUSTOM_ERROR, e);
                            return;
                        } catch (PspException e) {
                            fail(searchResponse, ErrorCode.CUSTOM_ERROR, e);
                            return;
                        }
                    }
                } else {
                    fail(searchResponse, ErrorCode.MALFORMED_REQUEST, "Unsupported objectclass.");
                    return;
                }

            } else {
                fail(searchResponse, ErrorCode.MALFORMED_REQUEST, "Unsupported query.");
                return;
            }
        }
    }

    /**
     * Add a group.
     * 
     * @param addRequest the add request
     * @param addResponse the add response
     */
    public void executeAddGroup(AddRequest addRequest, AddResponse addResponse) {

        String groupName = addRequest.getPsoID().getID();

        String parentStemName = GrouperUtil.parentStemNameFromName(groupName);

        Stem parentStem = StemFinder.findByName(grouperSession, parentStemName, false);
        if (parentStem == null) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, "Unable to add group. Parent stem not found.");
            return;
        }

        // data
        Extensible data = addRequest.getData();
        Map<String, DSMLAttr> dsmlAttrs = PSPUtil.getDSMLAttrMap(data);

        String displayExtension = getDsmlValue(dsmlAttrs, "displayExtension");
        if (displayExtension == null) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, "Unable to add group. Display extension is required.");
            return;
        }

        // group save object
        GroupSave groupSave = new GroupSave(grouperSession);
        groupSave.assignCreateParentStemsIfNotExist(false);

        groupSave.assignName(groupName);
        groupSave.assignDisplayExtension(displayExtension);

        String displayName = getDsmlValue(dsmlAttrs, "displayName");
        if (displayName != null) {
            groupSave.assignDisplayName(displayName);
        }

        String description = getDsmlValue(dsmlAttrs, "description");
        if (description != null) {
            groupSave.assignDescription(description);
        }

        Group group = null;
        try {
            LOG.debug("Target '{}' - Adding group '{}'", getId(), group);
            group = groupSave.save();
            LOG.info("Target '{}' - Added group '{}'", getId(), group);
        } catch (GroupAddException e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
            return;
        } catch (InsufficientPrivilegeException e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
            return;
        }

        // references
        if (addRequest.getReturnData().equals(ReturnData.EVERYTHING)) {
            modifyMemberships(group, addRequest.getCapabilityData(), ModificationMode.ADD, addResponse);
            if (!addResponse.getStatus().equals(StatusCode.SUCCESS)) {
                return;
            }
        }

        try {
            addResponse.setPso(getPSO(group, addRequest.getReturnData()));
            addResponse.setStatus(StatusCode.SUCCESS);
        } catch (Spml2Exception e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (PspException e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
        }
    }

    /**
     * Add a stem.
     * 
     * @param addRequest the add request
     * @param addResponse the add response
     */
    public void executeAddStem(AddRequest addRequest, AddResponse addResponse) {

        String stemName = addRequest.getPsoID().getID();

        String parentStemName = GrouperUtil.parentStemNameFromName(stemName);

        Stem parentStem = null;
        if (parentStemName == null) {
            parentStem = StemFinder.findRootStem(grouperSession);
        } else {
            parentStem = StemFinder.findByName(grouperSession, parentStemName, false);
        }

        if (parentStem == null) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, "Unable to add stem. Parent stem not found.");
            LOG.error(PSPUtil.toString(addResponse));
            return;
        }

        // data
        Extensible data = addRequest.getData();
        Map<String, DSMLAttr> dsmlAttrs = PSPUtil.getDSMLAttrMap(data);

        String displayExtension = getDsmlValue(dsmlAttrs, "displayExtension");
        if (displayExtension == null) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, "Unable to add stem. Display extension is required.");
            LOG.error(PSPUtil.toString(addResponse));
            return;
        }

        // stem save object
        StemSave stemSave = new StemSave(grouperSession);
        stemSave.assignCreateParentStemsIfNotExist(false);

        // required attributes
        stemSave.assignName(stemName);
        stemSave.assignDisplayExtension(displayExtension);

        String displayName = getDsmlValue(dsmlAttrs, "displayName");
        if (displayName != null) {
            stemSave.assignDisplayName(displayName);
        }

        String description = getDsmlValue(dsmlAttrs, "description");
        if (description != null) {
            stemSave.assignDescription(description);
        }

        try {
            // create the stem
            Stem stem = stemSave.save();
            LOG.info("Target '{}' - Added stem '{}'", getId(), stem);
            addResponse.setPso(getPSO(stem, addRequest.getReturnData()));
            addResponse.setStatus(StatusCode.SUCCESS);
        } catch (StemAddException e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
            LOG.error(PSPUtil.toString(addResponse));
        } catch (InsufficientPrivilegeException e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
            LOG.error(PSPUtil.toString(addResponse));
        } catch (DSMLProfileException e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
            LOG.error(PSPUtil.toString(addResponse));
        } catch (PspException e) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, e);
            LOG.error(PSPUtil.toString(addResponse));
        }
    }

    /**
     * Return the Grouper object identified by the given id. Returns either a {@link Group}, {@link Stem}, or null if
     * nothing is found.
     * 
     * If the {@link Request} has a {@link Pso.ENTITY_NAME_ATTRIBUTE} attribute of "group", then attempt to find a group
     * by name.
     * 
     * If the {@link Request} has a {@link Pso.ENTITY_NAME_ATTRIBUTE} attribute of "stem", then attempt to find a stem
     * by name.
     * 
     * If the {@link Request} does not have a {@link Pso.ENTITY_NAME_ATTRIBUTE}, attempt to find a group first then a
     * stem. If both a group and stem is found, set the response status to {@link ErrorCode.CUSTOM_ERROR} and return
     * null.
     * 
     * @param id the identifier
     * @param request the SPML request
     * @param response the SPML response
     * @return The group, stem, or null.
     */
    protected Object getObject(String id, Request request, Response response) {

        String entityName = request.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);

        Group group = null;
        if (entityName == null || entityName.equals("group")) {
            group = GroupFinder.findByName(grouperSession, id, false);
        }

        Stem stem = null;
        if (entityName == null || entityName.equals("stem")) {
            stem = StemFinder.findByName(grouperSession, id, false);
        }

        if (group == null && stem == null) {
            return null;
        }

        if (group != null && stem != null) {
            fail(response, ErrorCode.CUSTOM_ERROR, "More than one result found.");
            return null;
        }

        if (group != null) {
            return group;
        } else {
            return stem;
        }
    }

    /**
     * Returns a {@link PSO} applicable for the given group.
     * 
     * The "displayExtension", "description", and custom attributes are supported.
     * 
     * Group members are returned as references of type "members".
     * 
     * @param group the group
     * @param returnData the spml return data
     * @return the pso representation of the group
     * @throws Spml2Exception if an spml error occurs
     * @throws PspException if a psp error occurs
     */
    protected PSO getPSO(Group group, ReturnData returnData) throws Spml2Exception, PspException {

        PSO pso = new PSO();

        // return the name as the pso identifier id
        PSOIdentifier psoIdentifier = new PSOIdentifier();
        psoIdentifier.setID(group.getName());
        psoIdentifier.setTargetID(getId());
        pso.setPsoID(psoIdentifier);

        // determine schema entity
        Pso psoDefinition = getPSODefinition(group);
        pso.addOpenContentAttr(Pso.ENTITY_NAME_ATTRIBUTE, psoDefinition.getId());

        // attributes
        if (returnData.equals(ReturnData.DATA) || returnData.equals(ReturnData.EVERYTHING)) {

            Extensible data = new Extensible();

            if (psoDefinition.getAttributeNames().contains("displayExtension")) {
                String displayExtension = group.getDisplayExtension();
                if (!DatatypeHelper.isEmpty(displayExtension)) {
                    data.addOpenContentElement(new DSMLAttr("displayExtension", displayExtension));
                }
            }

            if (psoDefinition.getAttributeNames().contains("description")) {
                String description = group.getDescription();
                if (!DatatypeHelper.isEmpty(description)) {
                    data.addOpenContentElement(new DSMLAttr("description", description));
                }
            }

            // custom attributes
            Map<String, edu.internet2.middleware.grouper.Attribute> customAttributes = group.getAttributesMap(false);
            for (String attributeName : customAttributes.keySet()) {
                if (psoDefinition.getAttributeNames().contains(attributeName)) {
                    String value = customAttributes.get(attributeName).getValue();
                    if (!DatatypeHelper.isEmpty(value)) {
                        data.addOpenContentElement(new DSMLAttr(attributeName, value));
                    }
                }
            }

            pso.setData(data);
        }

        // memberships
        if (returnData.equals(ReturnData.EVERYTHING)) {
            List<Reference> references = new ArrayList<Reference>();
            Set<Member> sorted = new TreeSet<Member>(group.getMembers());
            for (Member member : sorted) {
                // reference to pso id
                PSOIdentifier toPSOId = new PSOIdentifier();
                // assume same target ?
                toPSOId.setTargetID(getId());
                // FUTURE containerID ?
                if (member.getSubjectSourceId().equals(SubjectFinder.internal_getGSA().getId())) {
                    toPSOId.setID(member.toGroup().getName());
                } else {
                    toPSOId.setID(member.getSubjectId());
                }

                Reference reference = new Reference();
                reference.setTypeOfReference("members");
                reference.setToPsoID(toPSOId);

                references.add(reference);
            }

            PSPUtil.setReferences(pso, references);
        }

        return pso;
    }

    /**
     * Returns a {@link PSO} applicable for the given stem.
     * 
     * The "displayExtension" and "description" attributes are supported.
     * 
     * @param stem the stem
     * @param returnData the spml return data
     * @return the pso representation of the stem
     * @throws DSMLProfileException if a dsml error occurs
     * @throws PspException if a psp error occurs
     */
    protected PSO getPSO(Stem stem, ReturnData returnData) throws DSMLProfileException, PspException {

        PSO pso = new PSO();

        // return the name as the pso identifier id
        PSOIdentifier psoIdentifier = new PSOIdentifier();
        psoIdentifier.setID(stem.getName());
        psoIdentifier.setTargetID(getId());
        pso.setPsoID(psoIdentifier);

        // determine schema entity
        Pso psoDefinition = getPSODefinition(stem);
        pso.addOpenContentAttr(Pso.ENTITY_NAME_ATTRIBUTE, psoDefinition.getId());

        // attributes
        if (returnData.equals(ReturnData.DATA) || returnData.equals(ReturnData.EVERYTHING)) {

            Extensible data = new Extensible();

            if (psoDefinition.getAttributeNames().contains("displayExtension")) {
                String displayExtension = stem.getDisplayExtension();
                if (!DatatypeHelper.isEmpty(displayExtension)) {
                    data.addOpenContentElement(new DSMLAttr("displayExtension", displayExtension));
                }
            }

            if (psoDefinition.getAttributeNames().contains("description")) {
                String description = stem.getDescription();
                if (!DatatypeHelper.isEmpty(description)) {
                    data.addOpenContentElement(new DSMLAttr("description", description));
                }
            }

            pso.setData(data);
        }

        return pso;
    }

    /**
     * Get the pso configuration applicable to the given group.
     * 
     * Currently, an identifying attribute with name "objectclass" and value "group" is supported.
     * 
     * @param group the group
     * @return the applicable pso configuration
     * @throws PspException if one and only one pso configuration is not found
     */
    protected Pso getPSODefinition(Group group) throws PspException {

        Pso definition = null;

        for (Pso psoDefinition : getPSP().getPsos(getId())) {
            PsoIdentifyingAttribute identifyingAttribute = psoDefinition.getPsoIdentifyingAttribute();
            if (identifyingAttribute == null) {
                continue;
            }
            String idAttrName = identifyingAttribute.getName();
            String idAttrValue = identifyingAttribute.getValue();

            if (idAttrName.equalsIgnoreCase("objectClass") && idAttrValue.equals("group")) {
                if (definition != null) {
                    LOG.error("More than one schema entity found for " + group);
                    throw new PspException("More than one schema entity found for " + group);
                }
                definition = psoDefinition;
            }
        }
        if (definition == null) {
            LOG.error("Unable to determine schema entity for " + group);
            throw new PspException("Unable to determine schema entity for " + group);
        }

        return definition;
    }

    /**
     * Get the pso configuration applicable to the given stem.
     * 
     * Currently, an identifying attribute with name "objectclass" and value "stem" is supported.
     * 
     * @param stem the stem
     * @return the applicable pso configuration
     * @throws PspException if one and only one pso configuration is not found
     */
    protected Pso getPSODefinition(Stem stem) throws PspException {

        Pso definition = null;

        for (Pso psoDefinition : getPSP().getPsos(getId())) {
            PsoIdentifyingAttribute identifyingAttribute = psoDefinition.getPsoIdentifyingAttribute();
            if (identifyingAttribute == null) {
                continue;
            }
            String idAttrName = identifyingAttribute.getName();
            String idAttrValue = identifyingAttribute.getValue();

            if (idAttrName.equalsIgnoreCase("objectClass") && idAttrValue.equals("stem")) {
                if (definition != null) {
                    LOG.error("More than one schema entity found for " + stem);
                    throw new PspException("More than one schema entity found for " + stem);
                }
                definition = psoDefinition;
            }
        }
        if (definition == null) {
            LOG.error("Unable to determine schema entity for " + stem);
            throw new PspException("Unable to determine schema entity for " + stem);
        }

        return definition;
    }

    /**
     * Modify the group description.
     * 
     * @param dsmlModification the dsml modification
     * @param modifyResponse the spml response
     * @param group the group to modify
     */
    public void modifyDescription(DSMLModification dsmlModification, ModifyResponse modifyResponse, Group group) {

        if (!dsmlModification.getName().equals("description")) {
            return;
        }

        DSMLValue[] dsmlValues = dsmlModification.getValues();

        // fail if modification contains more than one value
        if (dsmlModification.getValues().length > 1) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Description must be single valued.");
            return;
        }

        if (dsmlModification.getOperation().equals(ModificationMode.DELETE)) {
            // fail if description does not exist
            if (DatatypeHelper.isEmpty(group.getDescription())) {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR,
                        "Unable to delete description. Description does not exist.");
                return;
            }

            // fail if current description and description to delete do not match
            if (dsmlValues.length == 1 && !group.getDescription().equals(dsmlValues[0].getValue())) {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR,
                        "Unable to delete description. Current description and modification do not match.");
                return;
            }

            // 'delete' the description
            group.setDescription("");
            group.store();
        }

        // fail if description already exists
        if (dsmlModification.getOperation().equals(ModificationMode.ADD)
                && !DatatypeHelper.isEmpty(group.getDescription())) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Unable to add description. Description already exists.");
            return;
        }

        // set the description
        group.setDescription(dsmlValues[0].getValue());
        group.store();
    }

    /**
     * Modify the group description.
     * 
     * @param dsmlModification the dsml modification
     * @param modifyResponse the spml response
     * @param stem the stem to modify
     */
    public void modifyDescription(DSMLModification dsmlModification, ModifyResponse modifyResponse, Stem stem) {

        if (!dsmlModification.getName().equals("description")) {
            return;
        }

        DSMLValue[] dsmlValues = dsmlModification.getValues();

        // fail if modification contains more than one value
        if (dsmlModification.getValues().length > 1) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Description must be single valued.");
            return;
        }

        if (dsmlModification.getOperation().equals(ModificationMode.DELETE)) {
            // fail if description does not exist
            if (DatatypeHelper.isEmpty(stem.getDescription())) {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR,
                        "Unable to delete description. Description does not exist.");
                return;
            }

            // fail if current description and description to delete do not match
            if (dsmlValues.length == 1 && !stem.getDescription().equals(dsmlValues[0].getValue())) {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR,
                        "Unable to delete description. Current description and modification do not match.");
                return;
            }

            // 'delete' the description
            stem.setDescription("");
            stem.store();
        }

        // fail if description already exists
        if (dsmlModification.getOperation().equals(ModificationMode.ADD)
                && !DatatypeHelper.isEmpty(stem.getDescription())) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Unable to add description. Description already exists.");
            return;
        }

        // set the description
        stem.setDescription(dsmlValues[0].getValue());
        stem.store();
    }

    /**
     * Modify the group display extension.
     * 
     * @param dsmlModification the dsml modification
     * @param modifyResponse the spml response
     * @param group the group to modify
     */
    public void modifyDisplayExtension(DSMLModification dsmlModification, ModifyResponse modifyResponse, Group group) {

        if (!dsmlModification.getName().equals("displayExtension")) {
            return;
        }

        DSMLValue[] dsmlValues = dsmlModification.getValues();

        if (dsmlModification.getOperation().equals(ModificationMode.DELETE)) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Unable to delete display extension, it is required.");
            return;
        }

        // fail if modification contains more than one value
        if (dsmlModification.getValues().length > 1) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Display extension must be single valued.");
            return;
        }

        // fail if description already exists when adding a value
        if (dsmlModification.getOperation().equals(ModificationMode.ADD)) {
            if (!DatatypeHelper.isEmpty(group.getDisplayExtension())) {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR,
                        "Unable to add display extension. Display extension already exists.");
                return;
            }
        }

        // set the displayExtension
        group.setDisplayExtension(dsmlValues[0].getValue());
        group.store();
    }

    /**
     * Modify the group display extension.
     * 
     * @param dsmlModification the dsml modification
     * @param modifyResponse the spml response
     * @param stem the stem to modify
     */
    public void modifyDisplayExtension(DSMLModification dsmlModification, ModifyResponse modifyResponse, Stem stem) {

        if (!dsmlModification.getName().equals("displayExtension")) {
            return;
        }

        DSMLValue[] dsmlValues = dsmlModification.getValues();

        if (dsmlModification.getOperation().equals(ModificationMode.DELETE)) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Unable to delete display extension, it is required.");
            return;
        }

        // fail if modification contains more than one value
        if (dsmlModification.getValues().length > 1) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Display extension must be single valued.");
            return;
        }

        // fail if description already exists when adding a value
        if (dsmlModification.getOperation().equals(ModificationMode.ADD)) {
            if (!DatatypeHelper.isEmpty(stem.getDisplayExtension())) {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR,
                        "Unable to add display extension. Display extension already exists.");
                return;
            }
        }

        // set the displayExtension
        stem.setDisplayExtension(dsmlValues[0].getValue());
        stem.store();
    }

    /**
     * Add or delete a group membership.
     * 
     * @param group the group
     * @param reference the spml reference
     * @param modMode the modification mode, only add and delete are supported
     * @param response the spml response
     */
    public void modifyMembership(Group group, Reference reference, ModificationMode modMode, Response response) {
        // match target id
        if (!reference.getToPsoID().getTargetID().equals(getId())) {
            LOG.info("Target '{}' - Ignoring reference with target id '{}'", getId(), reference.getToPsoID()
                    .getTargetID());
            return;
        }

        // subject id
        String id = reference.getToPsoID().getID();
        if (DatatypeHelper.isEmpty(id)) {
            LOG.error("Target '{}' - A reference id is required '{}'.", getId(), PSPUtil.toString(reference));
            fail(response, ErrorCode.MALFORMED_REQUEST);
            return;
        }

        // subject
        // TODO source ?
        Subject subject = null;
        try {
            subject = SubjectFinder.findByIdOrIdentifier(id, true);
        } catch (SubjectNotFoundException e) {
            fail(response, ErrorCode.CUSTOM_ERROR, e);
            return;
        } catch (SubjectNotUniqueException e) {
            fail(response, ErrorCode.CUSTOM_ERROR, e);
            return;
        }

        Field field = null;
        try {
            field = FieldFinder.find(reference.getTypeOfReference(), true);
        } catch (SchemaException e) {
            fail(response, ErrorCode.CUSTOM_ERROR, e);
            return;
        }

        if (modMode.equals(ModificationMode.ADD)) {
            boolean didNotAlreadyExist = group.addMember(subject, field, false);
            LOG.debug("Target '{}' - Add '{}' to '{}'. Did not already exist '{}'", new Object[] {getId(), subject,
                    group, didNotAlreadyExist,});
        }

        if (modMode.equals(ModificationMode.DELETE)) {
            boolean notAlreadyDeleted = group.deleteMember(subject, field, false);
            LOG.debug("Target '{}' - Delete '{}' from '{}'. Was not already deleted '{}'", new Object[] {getId(),
                    subject, group, notAlreadyDeleted,});
        }

        if (modMode.equals(ModificationMode.REPLACE)) {
            fail(response, ErrorCode.UNSUPPORTED_OPERATION);
            return;
        }
    }

    /**
     * Add or delete group memberships.
     * 
     * @param group the group
     * @param capabilityDataArray the spml reference capability data
     * @param modMode the modification mode, only add and delete are supported
     * @param response the spml response
     */
    public void modifyMemberships(Group group, CapabilityData[] capabilityDataArray, ModificationMode modMode,
            Response response) {

        Map<String, List<Reference>> references = null;
        try {
            references = PSPUtil.getReferences(capabilityDataArray);
        } catch (PspException e) {
            fail(response, ErrorCode.CUSTOM_ERROR, e);
            return;
        }

        for (String typeOfReference : references.keySet()) {
            for (Reference reference : references.get(typeOfReference)) {
                modifyMembership(group, reference, ModificationMode.ADD, response);
                if (!response.getStatus().equals(StatusCode.SUCCESS)) {
                    return;
                }
            }
        }
    }

    /** {@inheritDoc} */
    protected void onNewContextCreated(ApplicationContext newServiceContext) throws ServiceException {
        LOG.debug("Target '{}' - Starting Grouper session,", getId());
        grouperSession = GrouperSession.startRootSession();
        LOG.info("Target '{}' - Started Grouper session '{}'", getId(), grouperSession);
    }

    /**
     * {@inheritDoc}
     * 
     * Sort by reverse grouper name order.
     */
    public Set<PSOIdentifier> orderForDeletion(Set<PSOIdentifier> psoIdentifiers) throws PspException {

        Map<String, PSOIdentifier> idToPsoIdMap = new TreeMap<String, PSOIdentifier>();

        for (PSOIdentifier psoIdentifier : psoIdentifiers) {
            idToPsoIdMap.put(psoIdentifier.getID(), psoIdentifier);
        }

        ArrayList<String> orderedIds = new ArrayList<String>(idToPsoIdMap.keySet());

        Collections.reverse(orderedIds);

        Set<PSOIdentifier> orderedForDeletion = new LinkedHashSet<PSOIdentifier>(idToPsoIdMap.size());
        for (String orderedId : orderedIds) {
            orderedForDeletion.add(idToPsoIdMap.get(orderedId));
        }

        return orderedForDeletion;
    }
}

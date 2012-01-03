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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.Stem.Scope;
import edu.internet2.middleware.grouper.StemFinder;
import edu.internet2.middleware.grouper.exception.GrouperSessionException;
import edu.internet2.middleware.grouper.misc.GrouperSessionHandler;
import edu.internet2.middleware.grouper.shibboleth.dataConnector.GroupDataConnector;
import edu.internet2.middleware.grouper.shibboleth.filter.Filter;
import edu.internet2.middleware.psp.spml.request.BulkProvisioningRequest;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/** A data connector which returns the names of all {@link Group}s matching the filter. */
public class AllGroupNamesDataConnector extends GroupDataConnector {

    /** The id of the attribute whose values are all group names. */
    public static final String ALL_IDENTIFIERS_ATTRIBUTE_ID = "groupNames";

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(AllGroupNamesDataConnector.class);

    /**
     * {@inheritDoc}
     * 
     * If the principal name is {@link BulkProvisioningRequest.BULK_REQUEST_ID}, then return a single attribute with ID
     * {@link ALL_IDENTIFIERS_ATTRIBUTE_ID} and values consisting of all group names either matching the filter or if no
     * filter is configured the names of all groups under the root stem.
     */
    public Map<String, BaseAttribute> resolve(final ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {

        try {
            Map<String, BaseAttribute> attributes =
                    (Map<String, BaseAttribute>) GrouperSession.callbackGrouperSession(getGrouperSession(),
                            new GrouperSessionHandler() {
                                public Map<String, BaseAttribute> callback(GrouperSession grouperSession) {
                                    return getAllGroupNames(resolutionContext, grouperSession);
                                }
                            });
            return attributes;
        } catch (GrouperSessionException e) {
            LOG.error("All group names data connector '" + getId() + "' - An error occured.", e);
            throw new AttributeResolutionException(e);
        }
    }

    /**
     * Return a single attribute with ID {@link ALL_IDENTIFIERS_ATTRIBUTE_ID} and values consisting of all group names
     * either matching the filter or if no filter is configured the names of all groups under the root stem.
     * 
     * @param resolutionContext the attribute resolver context
     * @param grouperSession the grouper session
     * @return the map of attributes
     * @throws AttributeResolutionException
     */
    protected Map<String, BaseAttribute> getAllGroupNames(final ShibbolethResolutionContext resolutionContext,
            GrouperSession grouperSession) {

        String principalName = resolutionContext.getAttributeRequestContext().getPrincipalName();

        LOG.debug("All group names data connector '{}' - Resolve principal '{}'", getId(), principalName);

        if (!principalName.equals(BulkProvisioningRequest.BULK_REQUEST_ID)) {
            LOG.debug("All group names data connector '{}' - Ignoring principal name '{}'", getId(), principalName);
            return Collections.EMPTY_MAP;
        }

        Set<Group> groups = new TreeSet<Group>();

        Filter<Group> filter = getFilter();
        if (filter == null) {
            groups.addAll(StemFinder.findRootStem(grouperSession).getChildGroups(Scope.SUB));
        } else {
            groups.addAll(getFilter().getResults(grouperSession));
        }

        Set<String> identifiers = new TreeSet<String>();
        for (Group group : groups) {
            identifiers.add(group.getName());
        }
        LOG.debug("All group names data connector '{}' - Get all group names found {}.", getId(), identifiers.size());

        // build attributes
        Map<String, BaseAttribute> attributes = new HashMap<String, BaseAttribute>();

        BasicAttribute basicAttribute = new BasicAttribute();
        basicAttribute.setId(ALL_IDENTIFIERS_ATTRIBUTE_ID);
        basicAttribute.setValues(identifiers);

        attributes.put(basicAttribute.getId(), basicAttribute);

        return attributes;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {

    }
}

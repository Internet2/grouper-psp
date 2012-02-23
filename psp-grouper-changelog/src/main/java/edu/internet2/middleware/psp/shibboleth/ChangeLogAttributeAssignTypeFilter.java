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
import java.util.Set;

import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.attr.assign.AttributeAssign;
import edu.internet2.middleware.grouper.attr.assign.AttributeAssignType;
import edu.internet2.middleware.grouper.attr.finder.AttributeAssignFinder;
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;
import edu.internet2.middleware.grouper.exception.QueryException;
import edu.internet2.middleware.grouper.shibboleth.filter.AbstractFilter;

/**
 * Matches a {@link ChangeLogEntry} by attribute assign type (group, stem, etc).
 */
public class ChangeLogAttributeAssignTypeFilter extends AbstractFilter<ChangeLogEntry> {

    /** The attribute assign type. */
    private AttributeAssignType desiredAttributeAssignType;

    /**
     * Constructor.
     * 
     * @param attributeAssignType the attribute assign type
     * @throws RuntimeException if the type is unknown
     */
    public ChangeLogAttributeAssignTypeFilter(String attributeAssignType) {
        desiredAttributeAssignType = AttributeAssignType.valueOfIgnoreCase(attributeAssignType, true);
    }

    /**
     * Returns an empty set since this method is not applicable for change log entries.
     * 
     * {@inheritDoc}
     */
    @Override public Set<ChangeLogEntry> getResults(GrouperSession s) throws QueryException {
        return Collections.EMPTY_SET;
    }

    /**
     * Returns true if the attribute assign type of the given {@link ChangeLogEntry} matches the defined attribute
     * assign type.
     * 
     * {@inheritDoc}
     */
    public boolean matches(ChangeLogEntry changeLogEntry) {

        if (changeLogEntry == null) {
            return false;
        }

        String attributeAssignId = null;
        try {
            attributeAssignId = changeLogEntry.retrieveValueForLabel("attributeAssignId");
        } catch (RuntimeException e) {
            // can not find label
            return false;
        }

        if (attributeAssignId == null) {
            return false;
        }

        AttributeAssign attributeAssign = AttributeAssignFinder.findById(attributeAssignId, false);
        if (attributeAssign == null) {
            return false;
        }

        AttributeAssignType entryAttributeAssignType = attributeAssign.getAttributeAssignType();
        if (entryAttributeAssignType == null) {
            return false;
        }

        if (entryAttributeAssignType.equals(desiredAttributeAssignType)) {
            return true;
        }

        return false;
    }

}

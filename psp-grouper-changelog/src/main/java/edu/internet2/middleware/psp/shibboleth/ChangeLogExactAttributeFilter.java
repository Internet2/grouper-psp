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
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;
import edu.internet2.middleware.grouper.exception.QueryException;
import edu.internet2.middleware.grouper.shibboleth.filter.AbstractFilter;

/**
 * Matches a {@link ChangeLogEntry} by exact attribute value.
 */
public class ChangeLogExactAttributeFilter extends AbstractFilter<ChangeLogEntry> {

    /** The attribute name. */
    private String name;

    /** The attribute value. */
    private String value;

    /**
     * Constructor.
     * 
     * @param name the attribute name
     * @param value the attribute value
     */
    public ChangeLogExactAttributeFilter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns an empty set since this method is not applicable for change log entries.
     * 
     * {@inheritDoc}
     */
    @Override
    public Set<ChangeLogEntry> getResults(GrouperSession s) throws QueryException {
        return Collections.EMPTY_SET;
    }

    /**
     * Returns true if the change log entry has the configured attribute name and value.
     * 
     * {@inheritDoc}
     */
    public boolean matches(Object o) {

        if (o == null) {
            return false;
        }

        if (!(o instanceof ChangeLogEntry)) {
            return false;
        }

        ChangeLogEntry changeLogEntry = (ChangeLogEntry) o;

        try {
            String retrievedValue = changeLogEntry.retrieveValueForLabel(name);
            if (value.equals(retrievedValue)) {
                return true;
            }
        } catch (RuntimeException e) {
            // a runtime exception is thrown if the change log entry does not have the named label
        }

        return false;
    }

}

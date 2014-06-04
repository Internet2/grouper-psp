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

import java.util.regex.Pattern;

import javax.naming.directory.SearchResult;

import edu.vt.middleware.ldap.handler.CopySearchResultHandler;
import edu.vt.middleware.ldap.handler.SearchCriteria;

/**
 * The <code>QuotedDnResultHandler</code> rewrites relative dns without starting and ending quotes, e.g.
 * "CN=Quoted/Name",DC=edu is rewritten as CN=Quoted/Name,DC=edu. This <code>SearchResultHandler</code> must be called
 * before the default <code>FqdnSearchResultHandler</code> since it relies on relative names.
 */
public class QuotedDnResultHandler extends CopySearchResultHandler {

    /** The string expression matching the start of the dn. */
    public static final String START_REGEX = "^\"";

    /** The string expression matching the end of the dn. */
    public static final String END_REGEX = "\"$";

    /** The pattern matching the start of the dn. */
    private static final Pattern START_PATTERN = Pattern.compile(START_REGEX);

    /** The pattern matching the end of the dn. */
    private static final Pattern END_PATTERN = Pattern.compile(END_REGEX);

    /**
     * {@inheritDoc}
     */
    protected String processDn(final SearchCriteria sc, final SearchResult sr) {

        String dn = sr.getName();

        if (sr.isRelative()) {
            if (START_PATTERN.matcher(dn).find() && END_PATTERN.matcher(dn).find()) {
                dn = START_PATTERN.matcher(dn).replaceFirst("");
                dn = END_PATTERN.matcher(dn).replaceFirst("");
            }
        }

        return dn;
    }
}

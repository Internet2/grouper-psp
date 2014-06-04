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

package edu.internet2.middleware.psp.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.joda.time.DateTime;
import org.opensaml.util.resource.Resource;
import org.opensaml.util.resource.ResourceException;

/** A do nothing {@link Resource}. */
public class EmptyResource implements Resource {

    /** Last modification time, set to when resources is created. */
    private DateTime lastModTime;

    /** Constructor. */
    public EmptyResource() {
        lastModTime = new DateTime();
    }

    /** {@inheritDoc} */
    public String getLocation() {
        return "empty";
    }

    /** {@inheritDoc} */
    public boolean exists() throws ResourceException {
        return true;
    }

    /** {@inheritDoc} */
    public InputStream getInputStream() throws ResourceException {
        return new ByteArrayInputStream("foo".getBytes());
    }

    /** {@inheritDoc} */
    public DateTime getLastModifiedTime() throws ResourceException {
        return lastModTime;
    }

}

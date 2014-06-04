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

package edu.internet2.middleware.psp.spml.provider;

import java.util.Set;

import org.openspml.v2.msg.spml.PSOIdentifier;

import edu.internet2.middleware.psp.Psp;
import edu.internet2.middleware.psp.PspException;

/**
 * An SPML Provisioning Service Provider which is also a Provisioning Service Target.
 */
public interface SpmlTarget extends SpmlProvider {

    /**
     * Set the provisioning service provider.
     * 
     * @param psp the provisioning service provider
     */
    public void setPSP(Psp psp);

    /**
     * Get the provisioning service provider.
     * 
     * @return the provisioning service provider
     */
    public Psp getPSP();

    /**
     * If true, this target supports multiple modifications in a single modify request. If false, this target only
     * supports one modification per modify request.
     * 
     * @return whether or not multiple modifications are supported in a single request
     */
    public boolean isBundleModifications();

    /**
     * Set whether or not multiple modifications are supported in a single modify request.
     * 
     * @param bundleModifications boolean
     */
    public void setBundleModifications(boolean bundleModifications);

    /**
     * Returns a set of {@link PSOIdentifiers}s in order suitable for deletion from the given set.
     * 
     * @param psoIdentifiers a set of identifiers
     * @return a new set of identifiers ordered for deletion
     * @throws PspException if an error occurs
     */
    public Set<PSOIdentifier> orderForDeletion(final Set<PSOIdentifier> psoIdentifiers) throws PspException;
}

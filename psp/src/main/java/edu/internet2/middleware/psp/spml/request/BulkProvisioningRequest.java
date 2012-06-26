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

package edu.internet2.middleware.psp.spml.request;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.openspml.v2.msg.spmlbatch.BatchRequest;
import org.openspml.v2.msg.spmlbatch.OnError;

public abstract class BulkProvisioningRequest extends ProvisioningRequest {

    // TODO extend BatchRequest ?

    // TODO use the same dateTime implementation as SPML, e.g. UpdatesRequest, when
    // available

    /** The ID of all bulk requests. */
    public static final String BULK_REQUEST_ID = BulkProvisioningRequest.class.getName();

    /** Whether or not to return diff responses in bulk response. */
    private boolean m_returnDiffResponses = true;

    /** Whether or not to return sync responses in bulk response. */
    private boolean m_returnSyncResponses = true;

    /** What to do on error, copied from {@link BatchRequest}. */
    private OnError m_onError = OnError.RESUME;

    public BulkProvisioningRequest() {
        super();
        this.setId(BULK_REQUEST_ID);
    };

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BulkProvisioningRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final BulkProvisioningRequest that = (BulkProvisioningRequest) o;

        if (m_onError != null ? !m_onError.equals(that.m_onError) : that.m_onError != null) {
            return false;
        }

        if (m_returnDiffResponses != that.m_returnDiffResponses) {
            return false;
        }

        if (m_returnSyncResponses != that.m_returnSyncResponses) {
            return false;
        }

        return true;
    }

    public OnError getOnError() {
        return m_onError;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (m_onError != null ? m_onError.hashCode() : 0);
        return result;
    }

    public boolean returnDiffResponses() {
        return m_returnDiffResponses;
    }

    public boolean returnSyncResponses() {
        return m_returnSyncResponses;
    }

    public void setReturnDiffResponses(boolean returnDiffResponses) {
        m_returnDiffResponses = returnDiffResponses;
    }

    public void setReturnSyncResponses(boolean returnSyncResponses) {
        m_returnSyncResponses = returnSyncResponses;
    }

    public void setOnError(OnError onError) {
        m_onError = onError;
    }

    public String toString() {
        ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        toStringBuilder.appendSuper(super.toString());
        toStringBuilder.append("onError", this.getOnError());
        toStringBuilder.append("returnDiffResponses", this.returnDiffResponses());
        toStringBuilder.append("returnSyncResponses", this.returnSyncResponses());
        return toStringBuilder.toString();
    }
}

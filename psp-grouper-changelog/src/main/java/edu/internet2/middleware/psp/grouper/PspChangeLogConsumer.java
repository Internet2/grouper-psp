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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.opensaml.util.resource.ResourceException;
import org.openspml.v2.msg.spml.CapabilityData;
import org.openspml.v2.msg.spml.DeleteRequest;
import org.openspml.v2.msg.spml.DeleteResponse;
import org.openspml.v2.msg.spml.ErrorCode;
import org.openspml.v2.msg.spml.LookupRequest;
import org.openspml.v2.msg.spml.LookupResponse;
import org.openspml.v2.msg.spml.Modification;
import org.openspml.v2.msg.spml.ModificationMode;
import org.openspml.v2.msg.spml.ModifyRequest;
import org.openspml.v2.msg.spml.ModifyResponse;
import org.openspml.v2.msg.spml.PSO;
import org.openspml.v2.msg.spml.Response;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spml.StatusCode;
import org.openspml.v2.msg.spmlref.Reference;
import org.openspml.v2.profiles.dsml.DSMLAttr;
import org.openspml.v2.profiles.dsml.DSMLModification;
import org.openspml.v2.profiles.dsml.DSMLValue;
import org.openspml.v2.util.Spml2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.grouper.app.loader.GrouperLoaderConfig;
import edu.internet2.middleware.grouper.changeLog.ChangeLogConsumer;
import edu.internet2.middleware.grouper.changeLog.ChangeLogConsumerBase;
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;
import edu.internet2.middleware.grouper.changeLog.ChangeLogLabel;
import edu.internet2.middleware.grouper.changeLog.ChangeLogLabels;
import edu.internet2.middleware.grouper.changeLog.ChangeLogProcessorMetadata;
import edu.internet2.middleware.psp.Psp;
import edu.internet2.middleware.psp.PspException;
import edu.internet2.middleware.psp.PspOptions;
import edu.internet2.middleware.psp.shibboleth.ChangeLogDataConnector;
import edu.internet2.middleware.psp.spml.config.Pso;
import edu.internet2.middleware.psp.spml.request.BulkSyncRequest;
import edu.internet2.middleware.psp.spml.request.BulkSyncResponse;
import edu.internet2.middleware.psp.spml.request.CalcRequest;
import edu.internet2.middleware.psp.spml.request.CalcResponse;
import edu.internet2.middleware.psp.spml.request.SyncRequest;
import edu.internet2.middleware.psp.spml.request.SyncResponse;
import edu.internet2.middleware.psp.util.PSPUtil;

/**
 * A {@link ChangeLogConsumer} which provisions via a {@link Psp}.
 * 
 * Extensions should override the process methods to change provisioning behavior.
 */
public class PspChangeLogConsumer extends ChangeLogConsumerBase {

    /** LDAP error returned when a stem/ou is renamed and the DSA does not support subtree renaming. */
    public static final String ERROR_SUBTREE_RENAME_NOT_SUPPORTED =
            "[LDAP: error code 66 - subtree rename not supported]";

    /** Boolean used to delay change log processing when a full sync is running. */
    private static boolean fullSyncIsRunning;

    /** Maps change log entry category and action (change log type) to methods. */
    enum EventType {

        /** Process the add group change log entry type. */
        group__addGroup {
            /** {@inheritDoc} */
            public void process(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processGroupAdd(consumer, changeLogEntry);
            }
        },

        /** Process the delete group change log entry type. */
        group__deleteGroup {
            /** {@inheritDoc} */
            public void process(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processGroupDelete(consumer, changeLogEntry);
            }
        },

        /** Process the update group change log entry type. */
        group__updateGroup {
            /** {@inheritDoc} */
            public void process(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processGroupUpdate(consumer, changeLogEntry);
            }
        },

        /** Process the add membership change log entry type. */
        membership__addMembership {
            /** {@inheritDoc} */
            public void process(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processMembershipAdd(consumer, changeLogEntry);
            }
        },

        /** Process the delete membership change log entry type. */
        membership__deleteMembership {
            /** {@inheritDoc} */
            public void process(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processMembershipDelete(consumer, changeLogEntry);
            }
        },

        /** Process the add stem change log entry type. */
        stem__addStem {
            /** {@inheritDoc} */
            public void process(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processStemAdd(consumer, changeLogEntry);
            }
        },

        /** Process the delete stem change log entry type. */
        stem__deleteStem {
            /** {@inheritDoc} */
            public void process(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processStemDelete(consumer, changeLogEntry);
            }
        },

        /** Process the update stem change log entry type. */
        stem__updateStem {
            /** {@inheritDoc} */
            public void process(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processStemUpdate(consumer, changeLogEntry);
            }
        },

        ;

        /**
         * Process the change log entry.
         * 
         * @param consumer the psp change log consumer
         * @param changeLogEntry the change log entry
         * @throws Exception if any error occurs
         */
        public abstract void process(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception;

    }

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PspChangeLogConsumer.class);

    /** The Provisioning Service Provider. */
    private static Psp psp;

    /** The change log consumer name from the processor metadata. */
    private String name;

    /**
     * 
     * Constructor. Initializes the underlying {@link Psp}.
     * 
     * @throws ResourceException if the configuration cannot be loaded
     */
    public PspChangeLogConsumer() throws ResourceException {
        initialize();
    }

    /**
     * Return the {@link Psp}.
     * 
     * @return the provisioning service provider
     */
    public Psp getPsp() {
        return psp;
    }

    /**
     * If the underlying {@link Psp} has not been initialized, instantiate the {@link Psp}. Use the configuration
     * directory from the 'changeLog.consumer.ldappcng.confDir' property. If this property has not been set, then
     * configuration resources on the classpath will be used.
     * 
     * @throws ResourceException if the configuration cannot be loaded
     */
    public void initialize() throws ResourceException {

        if (psp == null) {
            PspOptions pspOptions = new PspOptions(null);
            String confDir = GrouperLoaderConfig.getPropertyString("changeLog.consumer.psp.confDir");
            if (!confDir.isEmpty()) {
                LOG.info("Configuration directory {} set via property changeLog.consumer.psp.confDir", confDir);
                pspOptions.setConfDir(confDir);
            }
            psp = Psp.getPSP(pspOptions);
        }
    }

    /**
     * Execute each {@link ModifyRequest}. If an error occurs executing a request, continue to execute requests, but
     * throw an exception upon completion.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @param modifyRequests the modify requests
     * @throws PspException if an error occurs processing any modify request
     */
    public void executeModifyRequests(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry,
            Collection<ModifyRequest> modifyRequests) throws PspException {

        boolean isError = false;

        for (ModifyRequest modifyRequest : modifyRequests) {

            ModifyResponse modifyResponse = consumer.getPsp().execute(modifyRequest);

            if (modifyResponse.getStatus().equals(StatusCode.SUCCESS)) {
                LOG.info("PSP Consumer '{}' - Change log entry '{}' Modify '{}'", new Object[] {name,
                        toString(changeLogEntry), PSPUtil.toString(modifyResponse),});
            } else {
                LOG.error("PSP Consumer '{}' - Change log entry '{}' Modify failed '{}'", new Object[] {name,
                        toString(changeLogEntry), PSPUtil.toString(modifyResponse),});
                isError = true;
            }
        }

        if (isError) {
            String message =
                    "PSP Consumer '" + name + "' - Change log entry '" + toString(changeLogEntry) + "' Modify failed";
            LOG.error(message);
            throw new PspException(message);
        }
    }

    /**
     * Create and execute a {@link SyncRequest}. The request identifier is retrieved from the {@link ChangeLogEntry}
     * using the {@link ChangeLogLabel}.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @param changeLogLabel the change log label used to determine the sync request identifier
     * @throws PspException if an error occurs
     */
    public void
            executeSync(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry, ChangeLogLabel changeLogLabel)
                    throws PspException {

        // will throw a RuntimeException on error
        String principalName = changeLogEntry.retrieveValueForLabel(changeLogLabel);

        SyncRequest syncRequest = new SyncRequest();
        syncRequest.setId(principalName);
        syncRequest.setRequestID(PSPUtil.uniqueRequestId());

        LOG.debug("PSP Consumer '{}' - Change log entry '{}' Will attempt to sync '{}'", new Object[] {name,
                toString(changeLogEntry), PSPUtil.toString(syncRequest),});

        SyncResponse syncResponse = consumer.getPsp().execute(syncRequest);

        if (syncResponse.getStatus().equals(StatusCode.SUCCESS)) {
            LOG.info("PSP Consumer '{}' - Change log entry '{}' Sync was successfull '{}'", new Object[] {name,
                    toString(changeLogEntry), PSPUtil.toString(syncResponse),});
        } else if (syncResponse.getError().equals(ErrorCode.NO_SUCH_IDENTIFIER)) {
            LOG.info("PSP Consumer '{}' - Change log entry '{}' Sync unable to calculate provisioning '{}'",
                    new Object[] {name, toString(changeLogEntry), PSPUtil.toString(syncResponse),});
        } else {
            LOG.error("PSP Consumer '{}' - Change log entry '{}' Sync failed '{}'", new Object[] {name,
                    toString(changeLogEntry), PSPUtil.toString(syncResponse),});
            throw new PspException(PSPUtil.toString(syncResponse));
        }
    }

    /**
     * Run a full synchronization by executing a {@link BulkSyncRequest}.
     * 
     * @return the response
     */
    public synchronized Response fullSync() {

        LOG.info("PSP Consumer '{}' - Starting full sync", name);

        fullSyncIsRunning = true;

        BulkSyncResponse response = psp.execute(new BulkSyncRequest());

        fullSyncIsRunning = false;

        if (response.getStatus().equals(StatusCode.SUCCESS)) {
            LOG.info("PSP Consumer '{}' - Full sync was successfull '{}'", name, PSPUtil.toString(response));
        } else {
            LOG.error("PSP Consumer '{}' - Full sync was not successfull '{}'", name, PSPUtil.toString(response));
        }

        LOG.info("PSP Consumer '{}' - Finished full sync", name);

        return response;
    }

    /** {@inheritDoc} */
    public long processChangeLogEntries(final List<ChangeLogEntry> changeLogEntryList,
            ChangeLogProcessorMetadata changeLogProcessorMetadata) {

        long currentId = -1;

        // initialize this consumer's name from the change log metadata
        if (name == null) {
            name = changeLogProcessorMetadata.getConsumerName();
            LOG.trace("PSP Consumer '{}' - Setting name.", name);
        }

        LOG.debug("PSP Consumer '{}' - Processing change log entry list size '{}'", name, changeLogEntryList.size());

        // try catch so we can track that we made some progress
        try {
            for (ChangeLogEntry changeLogEntry : changeLogEntryList) {
                // increment the current ids
                currentId = changeLogEntry.getSequenceNumber();

                // if full sync is running, return the previous sequence number to process this entry on the next run
                if (fullSyncIsRunning) {
                    LOG.info("PSP Consumer '{}' - Full sync is running, returning sequence number '{}'", name,
                            currentId - 1);
                    return currentId - 1;
                }

                // process the change log entry
                processChangeLogEntry(changeLogEntry);
            }
        } catch (Exception e) {
            String message = "PSP Consumer '" + name + "' - An error occurred processing sequence number " + currentId;
            LOG.error(message, e);
            changeLogProcessorMetadata.registerProblem(e, message, currentId);
            changeLogProcessorMetadata.setHadProblem(true);
            changeLogProcessorMetadata.setRecordException(e);
            changeLogProcessorMetadata.setRecordExceptionSequence(currentId);
            // don't retry change log entry, we could loop indefinitely on error
            return currentId;
        }
        if (currentId == -1) {
            LOG.error("PSP Consumer '" + name + "' - Unable to process any records.");
            throw new RuntimeException("PSP Consumer '" + name + "' - Unable to process any records.");
        }

        LOG.debug("PSP Consumer '{}' - Finished processing change log entries. Last sequence number '{}'", name,
                currentId);

        return currentId;
    }

    /**
     * Call the method of the {@link EventType} enum which matches the {@link ChangeLogEntry} category and action (the
     * change log type).
     * 
     * @param changeLogEntry the change log entry
     * @throws Exception if an error occurs processing the change log entry
     */
    public void processChangeLogEntry(ChangeLogEntry changeLogEntry) throws Exception {

        try {
            // find the method to run via the enum
            String enumKey =
                    changeLogEntry.getChangeLogType().getChangeLogCategory() + "__"
                            + changeLogEntry.getChangeLogType().getActionName();

            EventType ldappcEventType = EventType.valueOf(enumKey);

            if (ldappcEventType == null) {
                LOG.debug("PSP Consumer '{}' - Change log entry '{}' Unsupported category and action.", name,
                        toString(changeLogEntry));
            } else {
                // process the change log event
                LOG.info("PSP Consumer '{}' - Change log entry '{}'", name, toStringDeep(changeLogEntry));
                ldappcEventType.process(this, changeLogEntry);
                LOG.info("PSP Consumer '{}' - Change log entry '{}' Finished processing.", name,
                        toString(changeLogEntry));
            }
        } catch (IllegalArgumentException e) {
            LOG.debug("PSP Consumer '{}' - Change log entry '{}' Unsupported category and action.", name,
                    toString(changeLogEntry));
        }
    }

    /**
     * Add a group.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @throws PspException if an error occurs processing the change log entry
     */
    public void processGroupAdd(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws PspException {

        LOG.debug("PSP Consumer '{}' - Change log entry '{}' Processing group add.", name, toString(changeLogEntry));

        executeSync(consumer, changeLogEntry, ChangeLogLabels.GROUP_ADD.name);
    }

    /**
     * Delete a group.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @throws PspException if an error occurs processing the change log entry
     */
    public void processGroupDelete(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws PspException {

        LOG.debug("PSP Consumer '{}' - Change log entry '{}' Processing group delete.", name, toString(changeLogEntry));

        processDelete(consumer, changeLogEntry);
    }

    /**
     * Update a group.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @throws PspException if an error occurs processing the change log entry
     */
    public void processGroupUpdate(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws PspException {

        LOG.debug("PSP Consumer '{}' - Change log entry '{}' Processing group update.", name, toString(changeLogEntry));

        processUpdate(consumer, changeLogEntry, ChangeLogLabels.GROUP_UPDATE.name);
    }

    /**
     * Add a membership.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @throws Spml2Exception if an spml error occurs
     * @throws PspException if an error occurs processing the change log entry
     */
    public void processMembershipAdd(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry)
            throws Spml2Exception, PspException {

        LOG.debug("PSP Consumer '{}' - Change log entry '{}' Processing membership add.", name,
                toString(changeLogEntry));

        List<ModifyRequest> modifyRequests =
                consumer.processMembershipChange(consumer, changeLogEntry, ModificationMode.ADD);

        executeModifyRequests(consumer, changeLogEntry, modifyRequests);
    }

    /**
     * Delete a membership.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @throws Spml2Exception if an spml error occurs
     * @throws PspException if an error occurs processing the change log entry
     */
    public void processMembershipDelete(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry)
            throws Spml2Exception, PspException {

        LOG.debug("PSP Consumer '{}' - Change log entry '{}' Processing membership delete.", name,
                toString(changeLogEntry));

        List<ModifyRequest> modifyRequests =
                consumer.processMembershipChange(consumer, changeLogEntry, ModificationMode.DELETE);

        executeModifyRequests(consumer, changeLogEntry, modifyRequests);
    }

    /**
     * Add a stem.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @throws PspException if an error occurs processing the change log entry
     */
    public void processStemAdd(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws PspException {

        LOG.debug("PSP Consumer '{}' - Change log entry '{}' Processing stem add.", name, toString(changeLogEntry));

        executeSync(consumer, changeLogEntry, ChangeLogLabels.STEM_ADD.name);
    }

    /**
     * Delete a stem.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @throws PspException if an error occurs processing the change log entry
     */
    public void processStemDelete(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws PspException {

        LOG.debug("PSP Consumer '{}' - Change log entry '{}' Processing stem delete.", name, toString(changeLogEntry));

        processDelete(consumer, changeLogEntry);
    }

    /**
     * Update a stem.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @throws PspException if an error occurs processing the change log entry
     */
    public void processStemUpdate(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws PspException {

        LOG.debug("PSP Consumer '{}' - Change log entry '{}' Processing stem update.", name, toString(changeLogEntry));

        processUpdate(consumer, changeLogEntry, ChangeLogLabels.STEM_UPDATE.name);
    }

    /**
     * Delete an object. The object identifiers to be deleted are calculated from the change log entry. For every object
     * to be deleted, a lookup is performed on the object identifier to determine if the object exists. If the object
     * exists, it is deleted.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @throws PspException if an error occurs deleting the object
     */
    public void processDelete(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws PspException {

        // calculate the psoID to be deleted from the change log entry
        CalcRequest calcRequest = new CalcRequest();
        calcRequest.setId(ChangeLogDataConnector.principalName(changeLogEntry.getSequenceNumber()));
        calcRequest.setReturnData(ReturnData.IDENTIFIER);
        calcRequest.setRequestID(PSPUtil.uniqueRequestId());

        CalcResponse calcResponse = consumer.getPsp().execute(calcRequest);

        if (!calcResponse.getStatus().equals(StatusCode.SUCCESS)) {
            LOG.error("PSP Consumer '{}' - Calc request '{}' failed {}", new Object[] {name, calcRequest.toString(),
                    PSPUtil.toString(calcResponse),});
            throw new PspException(PSPUtil.toString(calcResponse));
        }

        List<PSO> psos = calcResponse.getPSOs();

        if (psos.isEmpty()) {
            LOG.warn("PSP Consumer '{}' - Change log entry '{}' Unable to calculate identifier.", name,
                    toString(changeLogEntry));
            return;
        }

        for (PSO pso : psos) {
            // lookup object to see if it exists
            LookupRequest lookupRequest = new LookupRequest();
            lookupRequest.setPsoID(pso.getPsoID());
            lookupRequest.setRequestID(PSPUtil.uniqueRequestId());
            lookupRequest.setReturnData(ReturnData.IDENTIFIER);
            LookupResponse lookupResponse = consumer.getPsp().execute(lookupRequest);

            if (!lookupResponse.getStatus().equals(StatusCode.SUCCESS)) {
                LOG.debug("PSP Consumer '{}' - Change log entry '{}' Identifier '{}' does not exist.", new Object[] {
                        name, toString(changeLogEntry), PSPUtil.toString(pso.getPsoID()),});
                continue;
            }

            DeleteRequest deleteRequest = new DeleteRequest();
            deleteRequest.setPsoID(pso.getPsoID());
            deleteRequest.setRequestID(PSPUtil.uniqueRequestId());

            DeleteResponse deleteResponse = consumer.getPsp().execute(deleteRequest);

            if (deleteResponse.getStatus().equals(StatusCode.SUCCESS)) {
                LOG.info("PSP Consumer '{}' - Change log entry '{}' Delete '{}'", new Object[] {name,
                        toString(changeLogEntry), PSPUtil.toString(deleteResponse),});
            } else {
                LOG.error("PSP Consumer '{}' - Change log entry '{}' Delete failed '{}'", new Object[] {name,
                        toString(changeLogEntry), PSPUtil.toString(deleteResponse),});
                throw new PspException(PSPUtil.toString(deleteResponse));
            }
        }
    }

    /**
     * Return a {@link ModifyRequest} for every {@link PSO} whose references need to be modified. Each modify request
     * has a single {@link Modification} consisting of the {@link Reference}s which need to be added or deleted.
     * 
     * @param consumer the psp change log consumer
     * @param changeLogEntry the change log entry
     * @param modificationMode the modification mode, either add or delete
     * @return the possibly empty list of modify requests
     * @throws Spml2Exception if an SPML error occurs
     * @throws PspException if an error occurs processing the change log entry
     */
    public List<ModifyRequest> processMembershipChange(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry,
            ModificationMode modificationMode) throws Spml2Exception, PspException {

        List<ModifyRequest> modifyRequests = new ArrayList<ModifyRequest>();

        CalcRequest calcRequest = new CalcRequest();
        calcRequest.setId(ChangeLogDataConnector.principalName(changeLogEntry.getSequenceNumber()));
        calcRequest.setRequestID(PSPUtil.uniqueRequestId());

        CalcResponse calcResponse = consumer.getPsp().execute(calcRequest);

        for (PSO pso : calcResponse.getPSOs()) {

            List<Reference> references = processMembershipChangeReferences(pso, modificationMode);

            List<DSMLAttr> attributes = processMembershipChangeData(pso, modificationMode);

            if (references.isEmpty() && attributes.isEmpty()) {
                continue;
            }

            ModifyRequest modifyRequest = new ModifyRequest();
            modifyRequest.setRequestID(PSPUtil.uniqueRequestId());
            modifyRequest.setPsoID(pso.getPsoID());
            modifyRequest.addOpenContentAttr(Pso.ENTITY_NAME_ATTRIBUTE,
                    pso.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE));
            modifyRequest.setReturnData(ReturnData.IDENTIFIER);

            if (!references.isEmpty()) {
                Modification modification = new Modification();
                modification.setModificationMode(modificationMode);
                CapabilityData capabilityData = PSPUtil.fromReferences(references);
                modification.addCapabilityData(capabilityData);
                modifyRequest.addModification(modification);
            }

            if (!attributes.isEmpty()) {
                for (DSMLAttr dsmlAttr : attributes) {
                    Modification modification = new Modification();
                    modification.setModificationMode(modificationMode);
                    DSMLModification dsmlMod =
                            new DSMLModification(dsmlAttr.getName(), dsmlAttr.getValues(), modificationMode);
                    modification.addOpenContentElement(dsmlMod);
                    modifyRequest.addModification(modification);
                }
            }

            modifyRequests.add(modifyRequest);
        }

        return modifyRequests;
    }

    /**
     * Return the {@link DSMLAttr}s which need to be added or deleted to the {@link PSO}.
     * 
     * @param pso the provisioned object
     * @param modificationMode the modification mode, either add or delete
     * @return the possibly empty list of attributes
     * @throws Spml2Exception if an SPML error occurs
     * @throws PspException if an error occurs searching for attributes
     */
    public List<DSMLAttr> processMembershipChangeData(PSO pso, ModificationMode modificationMode)
            throws Spml2Exception, PspException {

        // the attributes which need to be modified
        List<DSMLAttr> attributes = new ArrayList<DSMLAttr>();

        // attributes from the pso
        Map<String, DSMLAttr> dsmlAttrMap = PSPUtil.getDSMLAttrMap(pso.getData());

        // for every attribute
        for (String dsmlAttrName : dsmlAttrMap.keySet()) {
            DSMLAttr dsmlAttr = dsmlAttrMap.get(dsmlAttrName);

            // for every attribute value
            for (DSMLValue dsmlValue : dsmlAttr.getValues()) {

                // if modification mode is delete, do not delete value if retain all values is true
                if (modificationMode.equals(ModificationMode.DELETE)) {
                    String entityName = pso.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);
                    if (entityName != null) {
                        Pso psoDefinition = psp.getPso(pso.getPsoID().getTargetID(), entityName);
                        if (psoDefinition != null) {
                            boolean retainAll = psoDefinition.getPsoAttribute(dsmlAttrName).isRetainAll();
                            if (retainAll) {
                                continue;
                            }
                        }
                    }
                }

                // perform a search to determine if the attribute exists
                boolean hasAttribute = psp.hasAttribute(pso.getPsoID(), dsmlAttr.getName(), dsmlValue.getValue());

                // if adding attribute and attribute does not exist, modify
                if (modificationMode.equals(ModificationMode.ADD) && !hasAttribute) {
                    attributes.add(dsmlAttr);
                }

                // if deleting attribute and attribute exists, modify
                if (modificationMode.equals(ModificationMode.DELETE) && hasAttribute) {
                    attributes.add(dsmlAttr);
                }
            }
        }

        return attributes;
    }

    /**
     * Return the {@link Reference}s which need to be added or deleted to the {@link PSO}.
     * 
     * A HasReference query is performed to determine if each {@link Reference} exists.
     * 
     * @param pso the provisioned object
     * @param modificationMode the modification mode, either add or delete
     * @return the possibly empty list of references
     * @throws Spml2Exception if an SPML error occurs
     * @throws PspException if an error occurs searching for references
     */
    public List<Reference> processMembershipChangeReferences(PSO pso, ModificationMode modificationMode)
            throws Spml2Exception, PspException {

        // the references which need to be modified
        List<Reference> references = new ArrayList<Reference>();

        // references from the pso
        Map<String, List<Reference>> referenceMap = PSPUtil.getReferences(pso.getCapabilityData());

        // for every type of reference, i.e. the attribute name
        for (String typeOfReference : referenceMap.keySet()) {
            // for every reference
            for (Reference reference : referenceMap.get(typeOfReference)) {

                // perform a search to determine if the reference exists
                boolean hasReference = psp.hasReference(pso.getPsoID(), reference);

                // if adding reference and reference does not exist, modify
                if (modificationMode.equals(ModificationMode.ADD) && !hasReference) {
                    references.add(reference);
                }

                // if deleting reference and reference exists, modify
                if (modificationMode.equals(ModificationMode.DELETE) && hasReference) {
                    references.add(reference);
                }
            }
        }

        return references;
    }

    /**
     * Process an object update. If the object should be renamed, attempt to rename, otherwise execute a
     * {@link SyncRequest}.
     * 
     * If the attempt to rename the object fails with the {@link ERROR_SUBTREE_RENAME_NOT_SUPPORTED} error, then attempt
     * to sync the object.
     * 
     * @param consumer the change log consumer
     * @param changeLogEntry the change log entry
     * @param principalNameLabel the change log label used to determine the sync request identifier
     * @throws PspException if an error occurs processing the change log entry
     */
    public void processUpdate(PspChangeLogConsumer consumer, ChangeLogEntry changeLogEntry,
            ChangeLogLabel principalNameLabel) throws PspException {

        LOG.debug("PSP Consumer '{}' - Change log entry '{}' Processing object update.", name, toString(changeLogEntry));

        CalcRequest calcRequest = new CalcRequest();
        calcRequest.setId(ChangeLogDataConnector.principalName(changeLogEntry.getSequenceNumber()));
        calcRequest.setReturnData(ReturnData.EVERYTHING);
        calcRequest.setRequestID(PSPUtil.uniqueRequestId());

        CalcResponse calcResponse = consumer.getPsp().execute(calcRequest);

        if (!calcResponse.getStatus().equals(StatusCode.SUCCESS)) {
            if (calcResponse.getError().equals(ErrorCode.NO_SUCH_IDENTIFIER)) {
                // OK just could not calculate identifier;
                LOG.debug("PSP Consumer '{}' - Change log entry '{}' Could not calculate identifier '{}'",
                        new Object[] {name, toString(changeLogEntry), PSPUtil.toString(calcResponse)});
                return;
            } else {
                LOG.error("PSP Consumer '{}' - Change log entry '{}' Processing object update failed '{}'",
                        new Object[] {name, toString(changeLogEntry), PSPUtil.toString(calcResponse)});
                throw new PspException(PSPUtil.toString(calcResponse));
            }
        }

        List<PSO> psos = calcResponse.getPSOs();

        if (psos.isEmpty()) {
            LOG.debug("PSP Consumer '{}' - Change log entry '{}' Nothing to provision.", name, toString(changeLogEntry));
            return;
        }

        boolean isError = false;

        for (PSO pso : psos) {

            // calculate rename
            ModifyRequest modifyRequest = consumer.getPsp().renameRequest(pso);

            if (modifyRequest == null) {
                // not renaming, attempt to sync
                try {
                    executeSync(consumer, changeLogEntry, principalNameLabel);
                } catch (PspException e) {
                    isError = true;
                }
            } else {
                // attempt to rename
                LOG.debug("PSP Consumer '{}' - Change log entry '{}' Will attempt to rename '{}'", new Object[] {name,
                        toString(changeLogEntry), PSPUtil.toString(modifyRequest),});

                ModifyResponse modifyResponse = consumer.getPsp().execute(modifyRequest);

                if (modifyResponse.getStatus().equals(StatusCode.SUCCESS)) {
                    LOG.debug("PSP Consumer '{}' - Change log entry '{}' Rename was successfull '{}'", new Object[] {
                            name, toString(changeLogEntry), PSPUtil.toString(modifyResponse),});
                } else {
                    LOG.error("PSP Consumer '{}' - Change log entry '{}' Rename failed '{}'", new Object[] {name,
                            toString(changeLogEntry), PSPUtil.toString(modifyResponse),});

                    // if target (e.g. openldap with bdb backend) returns subtree rename not supported error, sync
                    if (modifyResponse.getError().equals(ErrorCode.CUSTOM_ERROR)) {
                        List<String> errorMessages = Arrays.asList(modifyResponse.getErrorMessages());
                        if (errorMessages.size() == 1
                                && errorMessages.get(0).equals(ERROR_SUBTREE_RENAME_NOT_SUPPORTED)) {
                            try {
                                LOG.info(
                                        "PSP Consumer '{}' - Change log entry '{}' Rename failed, attempting to sync.",
                                        name, toString(changeLogEntry));
                                executeSync(consumer, changeLogEntry, principalNameLabel);
                            } catch (PspException e) {
                                isError = true;
                            }
                            continue;
                        }
                    }

                    isError = true;
                }
            }
        }

        if (isError) {
            String message =
                    "PSP Consumer '" + name + "' - Change log entry '" + toString(changeLogEntry) + "' Update failed";
            LOG.error(message);
            throw new PspException(message);
        }

        LOG.debug("PSP Consumer '{}' - Change log entry '{}' Processing object update was successfull.", name,
                toString(changeLogEntry));
    }

    /**
     * Gets a simple string representation of the change log entry.
     * 
     * @see ChangeLogDataConnector#toString(ChangeLogEntry)
     * 
     * @param changeLogEntry the change log entry
     * @return the simple string representation of the change log entry
     */
    public static String toString(ChangeLogEntry changeLogEntry) {
        return ChangeLogDataConnector.toString(changeLogEntry);
    }

    /**
     * Gets a deep string representation of the change log entry.
     * 
     * @see ChangeLogDataConnector#toStringDeep(ChangeLogEntry)
     * 
     * @param changeLogEntry the change log entry
     * @return the deep string representation of the change log entry
     */
    public static String toStringDeep(ChangeLogEntry changeLogEntry) {
        return ChangeLogDataConnector.toStringDeep(changeLogEntry);
    }
}

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

package edu.internet2.middleware.psp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opensaml.util.resource.ResourceException;
import org.opensaml.xml.util.DatatypeHelper;
import org.openspml.v2.msg.OCEtoMarshallableAdapter;
import org.openspml.v2.msg.spml.AddRequest;
import org.openspml.v2.msg.spml.AddResponse;
import org.openspml.v2.msg.spml.CapabilitiesList;
import org.openspml.v2.msg.spml.Capability;
import org.openspml.v2.msg.spml.CapabilityData;
import org.openspml.v2.msg.spml.DeleteRequest;
import org.openspml.v2.msg.spml.DeleteResponse;
import org.openspml.v2.msg.spml.ErrorCode;
import org.openspml.v2.msg.spml.ListTargetsRequest;
import org.openspml.v2.msg.spml.ListTargetsResponse;
import org.openspml.v2.msg.spml.LookupRequest;
import org.openspml.v2.msg.spml.LookupResponse;
import org.openspml.v2.msg.spml.Modification;
import org.openspml.v2.msg.spml.ModificationMode;
import org.openspml.v2.msg.spml.ModifyRequest;
import org.openspml.v2.msg.spml.ModifyResponse;
import org.openspml.v2.msg.spml.PSO;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.openspml.v2.msg.spml.Request;
import org.openspml.v2.msg.spml.Response;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spml.Schema;
import org.openspml.v2.msg.spml.SchemaEntityRef;
import org.openspml.v2.msg.spml.StatusCode;
import org.openspml.v2.msg.spmlbatch.OnError;
import org.openspml.v2.msg.spmlref.HasReference;
import org.openspml.v2.msg.spmlref.Reference;
import org.openspml.v2.msg.spmlref.ReferenceDefinition;
import org.openspml.v2.msg.spmlsearch.Query;
import org.openspml.v2.msg.spmlsearch.Scope;
import org.openspml.v2.msg.spmlsearch.SearchRequest;
import org.openspml.v2.msg.spmlsearch.SearchResponse;
import org.openspml.v2.profiles.DSMLProfileRegistrar;
import org.openspml.v2.profiles.dsml.DSMLAttr;
import org.openspml.v2.profiles.dsml.DSMLModification;
import org.openspml.v2.profiles.dsml.DSMLProfileException;
import org.openspml.v2.profiles.dsml.DSMLUnmarshaller;
import org.openspml.v2.profiles.dsml.DSMLValue;
import org.openspml.v2.profiles.dsml.EqualityMatch;
import org.openspml.v2.profiles.dsml.Filter;
import org.openspml.v2.profiles.spmldsml.AttributeDefinition;
import org.openspml.v2.profiles.spmldsml.AttributeDefinitionReference;
import org.openspml.v2.profiles.spmldsml.AttributeDefinitionReferences;
import org.openspml.v2.profiles.spmldsml.DSMLSchema;
import org.openspml.v2.profiles.spmldsml.ObjectClassDefinition;
import org.openspml.v2.util.Spml2Exception;
import org.openspml.v2.util.xml.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.psp.spml.config.Pso;
import edu.internet2.middleware.psp.spml.config.PsoAttribute;
import edu.internet2.middleware.psp.spml.config.PsoIdentifyingAttribute;
import edu.internet2.middleware.psp.spml.config.PsoReference;
import edu.internet2.middleware.psp.spml.config.PsoReferences;
import edu.internet2.middleware.psp.spml.provider.BaseSpmlProvider;
import edu.internet2.middleware.psp.spml.provider.SpmlProvider;
import edu.internet2.middleware.psp.spml.provider.SpmlTarget;
import edu.internet2.middleware.psp.spml.request.AlternateIdentifier;
import edu.internet2.middleware.psp.spml.request.BulkCalcRequest;
import edu.internet2.middleware.psp.spml.request.BulkCalcResponse;
import edu.internet2.middleware.psp.spml.request.BulkDiffRequest;
import edu.internet2.middleware.psp.spml.request.BulkDiffResponse;
import edu.internet2.middleware.psp.spml.request.BulkProvisioningRequest;
import edu.internet2.middleware.psp.spml.request.BulkSyncRequest;
import edu.internet2.middleware.psp.spml.request.BulkSyncResponse;
import edu.internet2.middleware.psp.spml.request.CalcRequest;
import edu.internet2.middleware.psp.spml.request.CalcResponse;
import edu.internet2.middleware.psp.spml.request.DiffRequest;
import edu.internet2.middleware.psp.spml.request.DiffResponse;
import edu.internet2.middleware.psp.spml.request.ProvisioningRequest;
import edu.internet2.middleware.psp.spml.request.ProvisioningResponse;
import edu.internet2.middleware.psp.spml.request.PspMarshallableCreator;
import edu.internet2.middleware.psp.spml.request.SearchRequestWithQueryClauseNamespaces;
import edu.internet2.middleware.psp.spml.request.SyncRequest;
import edu.internet2.middleware.psp.spml.request.SyncResponse;
import edu.internet2.middleware.psp.spml.request.SynchronizedResponse;
import edu.internet2.middleware.psp.util.AttributeModifier;
import edu.internet2.middleware.psp.util.MDCHelper;
import edu.internet2.middleware.psp.util.PSPUtil;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeRequestException;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.profile.provider.BaseSAMLProfileRequestContext;
import edu.internet2.middleware.shibboleth.common.service.ServiceException;
import javax.script.ScriptException;
import org.springframework.core.JdkVersion;

/**
 * Represents an (incomplete) spmlv2 provisioning service provider supporting calc, diff, and sync operations whose data
 * is calculated by a shibboleth attribute resolver.
 */
public class Psp extends BaseSpmlProvider implements SpmlProvider {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(Psp.class);

    /** Required bootstrap configuration files. */
    private static String[] CONFIG_FILES = {"psp-internal.xml", "psp-services.xml",};

    /** Configuration xml element name. */
    public static final String BEAN_NAME = "psp";

    /** The Shibboleth attribute authority. */
    private AttributeAuthority attributeAuthority;

    /** Spring identifier. */
    private String id;

    /** Map whose keys are target IDs and values are provisioned object definitions. */
    private Map<String, List<Pso>> objects = Collections.EMPTY_MAP;

    /** Runtime configuration. */
    private PspOptions pspOptions;

    /** Map whose keys are target IDs and values are targets. */
    private Map<String, SpmlTarget> targets = Collections.EMPTY_MAP;

    /** Constructor. */
    public Psp() {
    }

    /**
     * Get a {@link Psp} with default options.
     * 
     * @return the provisioning service provider
     * @throws ResourceException if a configuration error occurs
     */
    public static Psp getPSP() throws ResourceException {
        return getPSP(new PspOptions());
    }

    /**
     * Get a {@link Psp}.
     * 
     * @param pspOptions the psp options
     * @return the provisioning service provider
     * @throws ResourceException if a configuration error occurs
     */
    public static Psp getPSP(PspOptions pspOptions) throws ResourceException {
        String confDir = pspOptions != null ? pspOptions.getConfDir() : null;
        LOG.info("Loading psp from configuration directory '{}'", confDir);
        ApplicationContext context = PSPUtil.createSpringContext(PSPUtil.getResources(confDir, CONFIG_FILES));
        Psp psp = (Psp) context.getBean(BEAN_NAME);
        psp.setPspOptions(pspOptions);
        return psp;
    }

    /**
     * Return an spmlv2 add request.
     * 
     * @param pso the provisioning service object
     * @param returnData the return data
     * @return the spmlv2 add request
     */
    public AddRequest createAddRequest(PSO pso, ReturnData returnData) {

        AddRequest addRequest = new AddRequest();
        addRequest.setRequestID(PSPUtil.uniqueRequestId());
        addRequest.setReturnData(returnData);

        String entityName = pso.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);
        if (entityName != null) {
            addRequest.addOpenContentAttr(Pso.ENTITY_NAME_ATTRIBUTE, entityName);
        }

        // identifier
        addRequest.setPsoID(pso.getPsoID());
        addRequest.setTargetId(pso.getPsoID().getTargetID());
        if (pso.getPsoID().getContainerID() != null) {
            addRequest.setContainerID(pso.getPsoID().getContainerID());
        }

        // data
        if (returnData.equals(ReturnData.DATA) || returnData.equals(ReturnData.EVERYTHING)) {
            addRequest.setData(pso.getData());
        }

        // everything
        if (returnData.equals(ReturnData.EVERYTHING)) {
            for (CapabilityData capabilityData : pso.getCapabilityData()) {
                addRequest.addCapabilityData(capabilityData);
            }
        }

        return addRequest;
    }

    /**
     * Diff the current and correct provisioning service objects with the same identifier and return the modification
     * requests necessary to transform the current object to be identical to the correct object.
     * 
     * @param correctPso the representation of the object as it should be
     * @param currentPso the representation of the object as it currently is
     * @param returnData whether to compare identifiers and/or data and/or references
     * @return the modify requests necessary for synchronization
     * @throws PspException if the objects do not have the same identifier or schema entity name
     * @throws Spml2Exception if an spml error occurs
     */
    public List<ModifyRequest> diff(PSO correctPso, PSO currentPso, ReturnData returnData) throws PspException,
            Spml2Exception {

        List<ModifyRequest> modifyRequests = new ArrayList<ModifyRequest>();

        // entityName
        String correctEntityName = correctPso.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);
        String currentEntityName = currentPso.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);
        if (!correctEntityName.equals(currentEntityName)) {
            LOG.error("Unable to diff objects with different entityNames : '{}' and '{}'", correctEntityName,
                    currentEntityName);
            throw new PspException("Unable to diff objects with different entityNames.");
        }

        List<Modification> dataMods = Collections.EMPTY_LIST;
        List<Modification> referenceMods = Collections.EMPTY_LIST;

        if (returnData.equals(ReturnData.DATA) || returnData.equals(ReturnData.EVERYTHING)) {
            dataMods = diffData(correctPso, currentPso);
        }

        if (returnData.equals(ReturnData.EVERYTHING)) {
            referenceMods = diffReferences(correctPso, currentPso);
        }

        if (dataMods.isEmpty() && referenceMods.isEmpty()) {
            return modifyRequests;
        }

        if (getTarget(correctPso.getPsoID().getTargetID()).isBundleModifications()) {
            ModifyRequest modifyRequest = new ModifyRequest();
            modifyRequest.setRequestID(PSPUtil.uniqueRequestId());
            modifyRequest.setPsoID(correctPso.getPsoID());
            if (correctEntityName != null) {
                modifyRequest.addOpenContentAttr(Pso.ENTITY_NAME_ATTRIBUTE, correctEntityName);
            }
            for (Modification modification : dataMods) {
                modifyRequest.addModification(modification);
            }
            for (Modification modification : referenceMods) {
                modifyRequest.addModification(modification);
            }
            modifyRequests.add(modifyRequest);
        } else {
            modifyRequests.addAll(unbundleDataModifications(dataMods, correctPso.getPsoID(), correctEntityName));
            modifyRequests.addAll(unbundleReferenceModifications(referenceMods, correctPso.getPsoID(),
                    correctEntityName));
        }

        return modifyRequests;
    }

    /**
     * Diff the data of two Provisioning Service Objects. @see #diff(PSO, PSO)
     * 
     * @param correctPSO the representation of the PSO as it should be
     * @param currentPSO the representation of the PSO as it is
     * @return the <code>ModifyRequests</code> which would make the currentPSO identical to the correctPSO
     * @throws DSMLProfileException if an error occurs determining the <code>ModifyRequest</code>s
     * @throws PspException if the Provisioning Service Objects do not have an
     *             <code>PSODefinition.ENTITY_NAME_ATTRIBUTE</code>
     */
    public List<Modification> diffData(PSO correctPSO, PSO currentPSO) throws DSMLProfileException, PspException {
        List<Modification> modifications = new ArrayList<Modification>();

        Map<String, DSMLAttr> currentDsmlAttrs = PSPUtil.getDSMLAttrMap(currentPSO.getData());
        Map<String, DSMLAttr> correctDsmlAttrs = PSPUtil.getDSMLAttrMap(correctPSO.getData());

        Set<String> attrNames = new LinkedHashSet<String>();
        attrNames.addAll(correctDsmlAttrs.keySet());
        attrNames.addAll(currentDsmlAttrs.keySet());

        // determine the schema entity, assume pso IDs are the same for each pso, where do we
        // check this ?
        String targetId = currentPSO.getPsoID().getTargetID();
        String entityName = currentPSO.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);

        Pso psoDefinition = getPso(targetId, entityName);
        if (psoDefinition == null) {
            LOG.error("Unknown pso for target id '" + targetId + "' entity name '" + entityName + "'");
            throw new PspException("Unknown pso for target id '" + targetId + "' entity name '" + entityName + "'");
        }

        for (String attrName : attrNames) {
            PsoAttribute psoAttributeDefinition = psoDefinition.getPsoAttribute(attrName);
            if (psoAttributeDefinition == null) {
                LOG.error("Unknown pso attribute '" + attrName + "'");
                throw new PspException("Unknown pso attribute '" + attrName + "'");
            }
        }

        for (String attrName : attrNames) {
            DSMLAttr currentDsmlAttr = currentDsmlAttrs.get(attrName);
            DSMLAttr correctDsmlAttr = correctDsmlAttrs.get(attrName);

            AttributeModifier attributeModifier = new AttributeModifier(attrName, true);

            if (currentDsmlAttr != null) {
                attributeModifier.initDSML(currentDsmlAttr.getValues());

                PsoAttribute psoAttributeDefinition = psoDefinition.getPsoAttribute(attrName);
                if (psoAttributeDefinition.isRetainAll()) {
                    attributeModifier.retainAll();
                }

                if (psoAttributeDefinition.isReplaceValues()) {
                    attributeModifier.setReplaceValues(true);
                }
            }

            if (correctDsmlAttr != null) {
                attributeModifier.store(correctDsmlAttr.getValues());
            }

            modifications.addAll(attributeModifier.getDSMLModification());
        }

        return modifications;
    }

    /**
     * Diff the reference capability data of two Provisioning Service Objects. @see #diff(PSO, PSO)
     * 
     * @param correctPSO the representation of the PSO as it should be
     * @param currentPSO the representation of the PSO as it is
     * @return the <code>ModifyRequests</code> which would make the currentPSO identical to the correctPSO
     * @throws Spml2Exception if an error occurs determining the <code>ModifyRequest</code>s
     * @throws PspException if the reference capability can not be handled
     */
    public List<Modification> diffReferences(PSO correctPSO, PSO currentPSO) throws Spml2Exception, PspException {
        List<Modification> modifications = new ArrayList<Modification>();

        Map<String, List<Reference>> correctReferenceMap = PSPUtil.getReferences(correctPSO.getCapabilityData());
        Map<String, List<Reference>> currentReferenceMap = PSPUtil.getReferences(currentPSO.getCapabilityData());

        Set<String> typeOfReferences = new LinkedHashSet<String>();
        typeOfReferences.addAll(correctReferenceMap.keySet());
        typeOfReferences.addAll(currentReferenceMap.keySet());

        // determine the schema entity
        String targetId = correctPSO.getPsoID().getTargetID();
        String entityName = correctPSO.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);
        Pso psoDefinition = getPso(targetId, entityName);
        if (psoDefinition == null) {
            LOG.error("Unknown pso for target id '" + targetId + "' entity name '" + entityName + "'");
            throw new PspException("Unknown pso for target id '" + targetId + "' entity name '" + entityName + "'");
        }

        for (String typeOfReference : typeOfReferences) {
            List<Reference> currentReferences = currentReferenceMap.get(typeOfReference);
            List<Reference> correctReferences = correctReferenceMap.get(typeOfReference);

            PsoReferences psoReferences = psoDefinition.getReferences(typeOfReference);
            if (psoReferences == null) {
                LOG.error("Unknown pso references type '" + typeOfReference + "'");
                throw new PspException("Unknown pso references type '" + typeOfReference + "'");
            }

            AttributeModifier attributeModifier =
                    new AttributeModifier(typeOfReference, psoReferences.isCaseSensitive());

            if (currentReferences != null) {
                attributeModifier.initReference(currentReferences);
            }
            if (correctReferences != null) {
                attributeModifier.store(correctReferences);
            }

            modifications.addAll(attributeModifier.getReferenceModification());
        }

        return modifications;
    }

    /**
     * Lookup an identifier and return true if the lookup is successful and the identifier exists. Return false if the
     * lookup is successful and the identifier does not exist. Throw exception if the attempt to lookup the identifier
     * does not succeed.
     * 
     * @param psoID the provisioned object identifier
     * @return true if the identifier exists, false if the identifier does not exist
     * @throws PspException if an error occurs during when attempting to lookup the identifier
     */
    public boolean doesIdentifierExist(PSOIdentifier psoID) throws PspException {

        LookupRequest lookupRequest = new LookupRequest();
        lookupRequest.setPsoID(psoID);
        lookupRequest.setRequestID(PSPUtil.uniqueRequestId());
        lookupRequest.setReturnData(ReturnData.IDENTIFIER);

        LookupResponse lookupResponse = execute(lookupRequest);

        return doesIdentifierExist(lookupResponse);
    }

    /**
     * Returns true if the lookup response is successful and the identifier exists. Returns false if the lookup response
     * is successful and the identifier does not exist. Throw exception otherwise.
     * 
     * @param lookupResponse the lookup response
     * @return true if the identifier exists, false if the identifier does not exist
     * @throws PspException if the lookup response is not successful
     */
    public static boolean doesIdentifierExist(LookupResponse lookupResponse) throws PspException {

        if (lookupResponse.getStatus().equals(StatusCode.SUCCESS)) {
            return true;
        }

        if (lookupResponse.getStatus().equals(StatusCode.FAILURE)
                && lookupResponse.getError().equals(ErrorCode.NO_SUCH_IDENTIFIER)) {
            return false;
        }

        LOG.error("The lookup response is not a success '{}'", PSPUtil.toString(lookupResponse));
        throw new PspException("The lookup response is not a success '" + PSPUtil.toString(lookupResponse) + "'");
    }

    /** {@inheritDoc} */
    public void execute(AddRequest addRequest, AddResponse addResponse) {

        // Get the target definition.
        SpmlTarget target = targets.get(addRequest.getPsoID().getTargetID());

        // Execute the request on the target provider.
        Response targetResponse = target.execute(addRequest);

        // Fail if the response is not the correct class.
        if (!(targetResponse instanceof AddResponse)) {
            fail(addResponse, ErrorCode.CUSTOM_ERROR, "Target did not return an AddResponse.");
            return;
        }

        // If successful, set the PSO.
        if (targetResponse.getStatus().equals(StatusCode.SUCCESS)) {
            addResponse.setPso(((AddResponse) targetResponse).getPso());
        } else {
            // If not successful, fail the response.
            fail(addResponse, targetResponse.getError(), targetResponse.getErrorMessages());
        }
    }

    /**
     * Execute a calc request for every source identifier.
     * 
     * @param bulkCalcRequest the request
     * @return the response containing a calc response for every source identifier
     */
    public BulkCalcResponse execute(BulkCalcRequest bulkCalcRequest) {

        // Start MDC logging.
        MDCHelper mdc = new MDCHelper(bulkCalcRequest).start();

        // Log the request.
        LOG.info("Psp '{}' - BulkCalc {}", getId(), PSPUtil.toString(bulkCalcRequest));

        // Log the request as SPML.
        if (isLogSpml()) {
            LOG.info("Psp '{}' - BulkCalc SPML:\n{}", getId(), toXML(bulkCalcRequest));
        }

        // Potentially write the request.
        writeRequest(bulkCalcRequest);

        // Create a new response.
        BulkCalcResponse bulkCalcResponse = new BulkCalcResponse();

        // Be optimistic regarding success.
        bulkCalcResponse.setStatus(StatusCode.SUCCESS);

        // The response requestID should be the same as the request.
        bulkCalcResponse.setRequestID(getOrGenerateRequestID(bulkCalcRequest));

        // Validate the request.
        validate(bulkCalcRequest, bulkCalcResponse);

        // If the validation was successful, execute the request.
        if (bulkCalcResponse.getStatus().equals(StatusCode.SUCCESS)) {
            execute(bulkCalcRequest, bulkCalcResponse);
        }

        // If the response is a success, log to INFO.
        if (bulkCalcResponse.getStatus().equals(StatusCode.SUCCESS)) {
            LOG.info("Psp '{}' - BulkCalc {}", getId(), PSPUtil.toString(bulkCalcResponse));
            if (isLogSpml()) {
                LOG.info("Psp '{}' - BulkCalc SPML:\n{}", getId(), toXML(bulkCalcResponse));
            }
            // If the response is not a success, log to ERROR.
        } else {
            LOG.error("Psp '{}' - BulkCalc {}", getId(), PSPUtil.toString(bulkCalcResponse));
            if (isLogSpml()) {
                LOG.error("Psp '{}' - BulkCalc SPML:\n{}", getId(), toXML(bulkCalcResponse));
            }
        }

        // Potentially write the response.
        writeResponse(bulkCalcResponse);

        // Stop MDC logging.
        mdc.stop();

        // Return the response.
        return bulkCalcResponse;
    }

    /**
     * Execute an {@link BulkCalcRequest} and update the {@link BulkCalcResponse}.
     * 
     * @param bulkCalcRequest the SPML bulk calc request
     * @param bulkCalcResponse the SPML bulk calc response
     */
    public void execute(BulkCalcRequest bulkCalcRequest, BulkCalcResponse bulkCalcResponse) {

        // get all identifiers
        Map<String, List<SchemaEntityRef>> identifiers = null;
        try {
            identifiers = getAllSourceIdentifiers(bulkCalcRequest);
        } catch (AttributeRequestException e) {
            fail(bulkCalcResponse, ErrorCode.CUSTOM_ERROR, e);
            return;
        } catch (PspException e) {
            fail(bulkCalcResponse, ErrorCode.CUSTOM_ERROR, e);
            return;
        }
        if (identifiers == null) {
            fail(bulkCalcResponse, ErrorCode.CUSTOM_ERROR, "Unable to resolve source identifiers.");
            return;
        }

        // by creating the psp context here, references will be cached
        PspContext pspContext = new PspContext();
        pspContext.setCalcRequestMap(new HashMap<CalcRequest, CalcResponse>(identifiers.size()));

        // new CalcRequest for each identifier
        for (String identifier : identifiers.keySet()) {
            CalcRequest calcRequest = new CalcRequest();
            calcRequest.setId(identifier);
            calcRequest.setRequestID(PSPUtil.uniqueRequestId());
            calcRequest.setReturnData(bulkCalcRequest.getReturnData());
            calcRequest.setSchemaEntities(identifiers.get(identifier));

            CalcResponse calcResponse = execute(calcRequest, pspContext);
            bulkCalcResponse.addResponse(calcResponse);

            // first failure encountered, stop processing if OnError.EXIT
            if (calcResponse.getStatus() != StatusCode.SUCCESS && bulkCalcResponse.getStatus() != StatusCode.FAILURE) {
                bulkCalcResponse.setStatus(StatusCode.FAILURE);
                if (bulkCalcRequest.getOnError().equals(OnError.EXIT)) {
                    return;
                }
            }
        }

    }

    /**
     * Execute a diff request for every source identifier.
     * 
     * @param bulkDiffRequest the request
     * @return the response containing a diff response for every source identifier
     */
    public BulkDiffResponse execute(BulkDiffRequest bulkDiffRequest) {

        // Start MDC logging.
        MDCHelper mdc = new MDCHelper(bulkDiffRequest).start();

        // Log the request.
        LOG.info("Psp '{}' - BulkDiff {}", getId(), PSPUtil.toString(bulkDiffRequest));

        // Log the request as SPML.
        if (isLogSpml()) {
            LOG.info("Psp '{}' - BulkDiff SPML:\n{}", getId(), toXML(bulkDiffRequest));
        }

        // Potentially write the request.
        writeRequest(bulkDiffRequest);

        // Create a new response.
        BulkDiffResponse bulkDiffResponse = new BulkDiffResponse();

        // Be optimistic regarding success.
        bulkDiffResponse.setStatus(StatusCode.SUCCESS);

        // The response requestID should be the same as the request.
        bulkDiffResponse.setRequestID(getOrGenerateRequestID(bulkDiffRequest));

        // Validate the request.
        validate(bulkDiffRequest, bulkDiffResponse);

        // If the validation was successful, execute the request.
        if (bulkDiffResponse.getStatus().equals(StatusCode.SUCCESS)) {
            execute(bulkDiffRequest, bulkDiffResponse);
        }

        // If the response is a success, log to INFO.
        if (bulkDiffResponse.getStatus().equals(StatusCode.SUCCESS)) {
            LOG.info("Psp '{}' - BulkDiff {}", getId(), PSPUtil.toString(bulkDiffResponse));
            if (isLogSpml()) {
                LOG.info("Psp '{}' - BulkDiff SPML:\n{}", getId(), toXML(bulkDiffResponse));
            }
            // If the response is not a success, log to ERROR.
        } else {
            LOG.error("Psp '{}' - BulkDiff {}", getId(), PSPUtil.toString(bulkDiffResponse));
            if (isLogSpml()) {
                LOG.error("Psp '{}' - BulkDiff SPML:\n{}", getId(), toXML(bulkDiffResponse));
            }
        }

        // Potentially write the response.
        writeResponse(bulkDiffResponse);

        // Stop MDC logging.
        mdc.stop();

        // Return the response.
        return bulkDiffResponse;
    }

    /**
     * Execute an {@link BulkDiffRequest} and update the {@link BulkDiffResponse}.
     * 
     * @param bulkDiffRequest the SPML bulk diff request
     * @param bulkDiffResponse the SPML bulk diff response
     */
    public void execute(BulkDiffRequest bulkDiffRequest, BulkDiffResponse bulkDiffResponse) {

        try {
            // get all source identifiers
            Map<String, List<SchemaEntityRef>> identifiers = null;
            try {
                identifiers = getAllSourceIdentifiers(bulkDiffRequest);
            } catch (AttributeRequestException e) {
                fail(bulkDiffResponse, ErrorCode.CUSTOM_ERROR, e);
                return;
            } catch (PspException e) {
                fail(bulkDiffResponse, ErrorCode.CUSTOM_ERROR, e);
                return;
            }
            if (identifiers == null) {
                fail(bulkDiffResponse, ErrorCode.CUSTOM_ERROR, "Unable to resolve source identifiers.");
                return;
            }

            // get target identifiers which currently exist
            Set<PSOIdentifier> currentPsoIds = getAllTargetIdentifiers(bulkDiffRequest, bulkDiffResponse);
            if (currentPsoIds == null) {
                return;
            }

            // PSOIdentifiers that should exist
            Set<PSOIdentifier> correctPsoIds = new LinkedHashSet<PSOIdentifier>();

            // PSOIdentifiers to be deleted
            Set<PSOIdentifier> psoIdsToBeDeleted = new LinkedHashSet<PSOIdentifier>();

            // by creating the psp context here, references will be cached
            PspContext pspContext = new PspContext();
            pspContext.setCalcRequestMap(new HashMap<CalcRequest, CalcResponse>(identifiers.size()));

            // diff each identifier
            for (String identifier : identifiers.keySet()) {

                // new diff request
                DiffRequest diffRequest = new DiffRequest();
                diffRequest.setId(identifier);
                diffRequest.setRequestID(PSPUtil.uniqueRequestId());
                diffRequest.setReturnData(bulkDiffRequest.getReturnData());
                diffRequest.setSchemaEntities(identifiers.get(identifier));

                // execute diff request
                DiffResponse diffResponse = execute(diffRequest, pspContext);

                // add diff response to bulk response ?
                boolean addToBulkResponse = false;
                if (bulkDiffRequest.returnDiffResponses()) {
                    if (!diffResponse.getRequests().isEmpty()) {
                        addToBulkResponse = true;
                    }
                }
                if (bulkDiffRequest.returnSyncResponses()) {
                    if (!diffResponse.getSynchronizedResponses().isEmpty()) {
                        addToBulkResponse = true;
                    }
                }
                if (addToBulkResponse) {
                    bulkDiffResponse.addResponse(diffResponse);
                }

                // store correct ids and ids to be deleted for reconciliation
                for (AddRequest addRequest : diffResponse.getAddRequests()) {
                    correctPsoIds.add(addRequest.getPsoID());
                }
                for (ModifyRequest modifyRequest : diffResponse.getModifyRequests()) {
                    correctPsoIds.add(modifyRequest.getPsoID());
                }
                for (DeleteRequest deleteRequest : diffResponse.getDeleteRequests()) {
                    psoIdsToBeDeleted.add(deleteRequest.getPsoID());
                }
                for (SynchronizedResponse synchronizedResponse : diffResponse.getSynchronizedResponses()) {
                    correctPsoIds.add(synchronizedResponse.getPsoID());
                }

                // first failure encountered, stop processing if OnError.EXIT
                if (diffResponse.getStatus() != StatusCode.SUCCESS
                        && bulkDiffResponse.getStatus() != StatusCode.FAILURE) {
                    bulkDiffResponse.setStatus(StatusCode.FAILURE);
                    if (bulkDiffRequest.getOnError().equals(OnError.EXIT)) {
                        return;
                    }
                }
            }

            // DeleteRequests for identifiers which exist but shouldn't, and which are not already being deleted
            for (PSOIdentifier psoId : currentPsoIds) {
                if (!correctPsoIds.contains(psoId) && !psoIdsToBeDeleted.contains(psoId)) {
                    if (bulkDiffRequest.returnDiffResponses()) {
                        DeleteRequest deleteRequest = new DeleteRequest();
                        deleteRequest.setPsoID(psoId);
                        deleteRequest.setRequestID(PSPUtil.uniqueRequestId());
                        DiffResponse diffResponse = new DiffResponse();
                        diffResponse.setId(psoId.getID());
                        diffResponse.addRequest(deleteRequest);
                        bulkDiffResponse.addResponse(diffResponse);
                    }
                }
            }
        } catch (PspException e) {
            fail(bulkDiffResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (Spml2Exception e) {
            fail(bulkDiffResponse, ErrorCode.CUSTOM_ERROR, e);
        }
    }

    /**
     * Execute a sync request for every source identifier.
     * 
     * @param bulkSyncRequest the request
     * @return the response containing a sync response for every source identifier
     */
    public BulkSyncResponse execute(BulkSyncRequest bulkSyncRequest) {

        // Start MDC logging.
        MDCHelper mdc = new MDCHelper(bulkSyncRequest).start();

        // Log the request.
        LOG.info("Psp '{}' - BulkSync {}", getId(), PSPUtil.toString(bulkSyncRequest));

        // Log the request as SPML.
        if (isLogSpml()) {
            LOG.info("Psp '{}' - BulkSync SPML:\n{}", getId(), toXML(bulkSyncRequest));
        }

        // Potentially write the request.
        writeRequest(bulkSyncRequest);

        // Create a new response.
        BulkSyncResponse bulkSyncResponse = new BulkSyncResponse();

        // Be optimistic regarding success.
        bulkSyncResponse.setStatus(StatusCode.SUCCESS);

        // The response requestID should be the same as the request.
        bulkSyncResponse.setRequestID(getOrGenerateRequestID(bulkSyncRequest));

        // Validate the request.
        validate(bulkSyncRequest, bulkSyncResponse);

        // If the validation was successful, execute the request.
        if (bulkSyncResponse.getStatus().equals(StatusCode.SUCCESS)) {
            execute(bulkSyncRequest, bulkSyncResponse);
        }

        // If the response is a success, log to INFO.
        if (bulkSyncResponse.getStatus().equals(StatusCode.SUCCESS)) {
            LOG.info("Psp '{}' - BulkSync {}", getId(), PSPUtil.toString(bulkSyncResponse));
            if (isLogSpml()) {
                LOG.info("Psp '{}' - BulkSync SPML:\n{}", getId(), toXML(bulkSyncResponse));
            }
            // If the response is not a success, log to ERROR.
        } else {
            LOG.error("Psp '{}' - BulkSync {}", getId(), PSPUtil.toString(bulkSyncResponse));
            if (isLogSpml()) {
                LOG.error("Psp '{}' -  BulkSync SPML:\n{}", getId(), toXML(bulkSyncResponse));
            }
        }

        // Potentially write the response.
        writeResponse(bulkSyncResponse);

        // Stop MDC logging.
        mdc.stop();

        // Return the response.
        return bulkSyncResponse;
    }

    /**
     * Execute an {@link BulkSyncRequest} and update the {@link BulkSyncResponse}.
     * 
     * @param bulkSyncRequest the SPML bulk sync request
     * @param bulkSyncResponse the SPML bulk sync response
     */
    public void execute(BulkSyncRequest bulkSyncRequest, BulkSyncResponse bulkSyncResponse) {

        try {
            // get all source identifiers
            Map<String, List<SchemaEntityRef>> identifiers = null;
            try {
                identifiers = getAllSourceIdentifiers(bulkSyncRequest);
            } catch (AttributeRequestException e) {
                fail(bulkSyncResponse, ErrorCode.CUSTOM_ERROR, e);
                return;
            } catch (PspException e) {
                fail(bulkSyncResponse, ErrorCode.CUSTOM_ERROR, e);
                return;
            }
            if (identifiers == null) {
                fail(bulkSyncResponse, ErrorCode.CUSTOM_ERROR, "Unable to resolve source identifiers.");
                return;
            }

            // get target identifiers which currently exist
            Set<PSOIdentifier> currentPsoIds = getAllTargetIdentifiers(bulkSyncRequest, bulkSyncResponse);
            if (currentPsoIds == null) {
                return;
            }

            // PSOIdentifiers that should exist
            Set<PSOIdentifier> correctPsoIds = new LinkedHashSet<PSOIdentifier>();

            // PSOIdentifiers to be deleted
            Set<PSOIdentifier> psoIdsToBeDeleted = new LinkedHashSet<PSOIdentifier>();

            // by creating the psp context here, references will be cached
            PspContext pspContext = new PspContext();
            pspContext.setCalcRequestMap(new HashMap<CalcRequest, CalcResponse>(identifiers.size()));

            // sync each identifier
            for (String identifier : identifiers.keySet()) {

                // new sync request
                SyncRequest syncRequest = new SyncRequest();
                syncRequest.setId(identifier);
                syncRequest.setRequestID(PSPUtil.uniqueRequestId());
                syncRequest.setReturnData(bulkSyncRequest.getReturnData());
                syncRequest.setSchemaEntities(identifiers.get(identifier));

                // execute sync request
                SyncResponse syncResponse = execute(syncRequest, pspContext);

                // add sync response to bulk response ?
                boolean addToBulkResponse = false;
                if (bulkSyncRequest.returnDiffResponses()) {
                    if (!syncResponse.getAddDeleteModifyResponses().isEmpty()) {
                        addToBulkResponse = true;
                    }
                }
                if (bulkSyncRequest.returnSyncResponses()) {
                    if (!syncResponse.getSynchronizedResponses().isEmpty()) {
                        addToBulkResponse = true;
                    }
                }
                if (addToBulkResponse) {
                    bulkSyncResponse.addResponse(syncResponse);
                }

                // store correct ids and ids to be deleted for reconciliation
                DiffResponse diffResponse = syncResponse.getDiffResponse();
                for (AddRequest addRequest : diffResponse.getAddRequests()) {
                    correctPsoIds.add(addRequest.getPsoID());
                }
                for (ModifyRequest modifyRequest : diffResponse.getModifyRequests()) {
                    correctPsoIds.add(modifyRequest.getPsoID());
                }
                for (DeleteRequest deleteRequest : diffResponse.getDeleteRequests()) {
                    psoIdsToBeDeleted.add(deleteRequest.getPsoID());
                }
                for (SynchronizedResponse synchronizedResponse : diffResponse.getSynchronizedResponses()) {
                    correctPsoIds.add(synchronizedResponse.getPsoID());
                }

                // first failure encountered, stop processing if OnError.EXIT
                if (syncResponse.getStatus() != StatusCode.SUCCESS
                        && bulkSyncResponse.getStatus() != StatusCode.FAILURE) {
                    bulkSyncResponse.setStatus(StatusCode.FAILURE);
                    if (bulkSyncRequest.getOnError().equals(OnError.EXIT)) {
                        return;
                    }
                }
            }

            // DeleteRequests for identifiers which exist but shouldn't, and which are not already being deleted
            for (PSOIdentifier psoId : currentPsoIds) {
                if (!correctPsoIds.contains(psoId) && !psoIdsToBeDeleted.contains(psoId)) {
                    DeleteRequest deleteRequest = new DeleteRequest();
                    deleteRequest.setPsoID(psoId);
                    deleteRequest.setRequestID(PSPUtil.uniqueRequestId());

                    DeleteResponse deleteResponse = execute(deleteRequest);

                    SyncResponse syncResponse = new SyncResponse();
                    syncResponse.setId(psoId.getID());
                    syncResponse.addResponse(deleteResponse);
                    syncResponse.setRequestID(deleteResponse.getRequestID());
                    syncResponse.setStatus(deleteResponse.getStatus());

                    if (deleteResponse.getStatus().equals(StatusCode.FAILURE)) {
                        fail(syncResponse, deleteResponse.getError(), deleteResponse.getErrorMessages());
                    }

                    if (bulkSyncRequest.returnDiffResponses()) {
                        bulkSyncResponse.addResponse(syncResponse);
                    }

                    // first failure encountered, stop processing if OnError.EXIT
                    if (syncResponse.getStatus() != StatusCode.SUCCESS
                            && bulkSyncResponse.getStatus() != StatusCode.FAILURE) {
                        bulkSyncResponse.setStatus(StatusCode.FAILURE);
                        if (bulkSyncRequest.getOnError().equals(OnError.EXIT)) {
                            return;
                        }
                    }
                }
            }
        } catch (PspException e) {
            fail(bulkSyncResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (Spml2Exception e) {
            fail(bulkSyncResponse, ErrorCode.CUSTOM_ERROR, e);
        }
    }

    /** {@inheritDoc} */
    public CalcResponse execute(CalcRequest calcRequest) {
        return execute(calcRequest, new PspContext());
    }

    /**
     * {@inheritDoc}
     * 
     * The psp context argument allows for the caching of references during bulk requests.
     * 
     * @param pspContext the psp context
     */
    public CalcResponse execute(CalcRequest calcRequest, PspContext pspContext) {

        // Start MDC logging.
        MDCHelper mdc = new MDCHelper(calcRequest).start();

        // Log the request.
        LOG.info("Psp '{}' - Calc {}", getId(), PSPUtil.toString(calcRequest));

        // Log the request as SPML.
        if (isLogSpml()) {
            LOG.info("Psp '{}' - Calc XML:\n{}", getId(), toXML(calcRequest));
        }

        // Potentially write the request.
        writeRequest(calcRequest);

        // Create a new response.
        CalcResponse calcResponse = new CalcResponse();

        // Be optimistic regarding success.
        calcResponse.setStatus(StatusCode.SUCCESS);

        // The response requestID should be the same as the request.
        calcResponse.setRequestID(getOrGenerateRequestID(calcRequest));

        // Validate the request.
        validate(calcRequest, calcResponse);

        // If the validation was successful, execute the request.
        if (calcResponse.getStatus().equals(StatusCode.SUCCESS)) {
            execute(calcRequest, calcResponse, pspContext);
        }

        // If the response is a success, log to INFO.
        if (calcResponse.getStatus().equals(StatusCode.SUCCESS)) {
            LOG.info("Psp '{}' - Calc {}", getId(), PSPUtil.toString(calcResponse));
            if (isLogSpml()) {
                LOG.info("Psp '{}' - Calc XML:\n{}", getId(), toXML(calcResponse));
            }
            // If the response is not a success, log to ERROR.
        } else {
            String errorMsg = PSPUtil.toString(calcResponse);
            // Special case the Unable to calculate provisioned object error. 
            // this typically happens because the action to the object happens
            // outside of an area the PSP has knowledge of.  GRP-811 
            if(errorMsg.contains("Unable to calculate provisioned object.")){
                LOG.info("Psp '{}' - Calc {}", getId(), errorMsg);
                if (isLogSpml()) {
                    LOG.info("Psp '{}' - Calc XML:\n{}", getId(), toXML(calcResponse));
                }
            }
            else{
                LOG.error("Psp '{}' - Calc {}", getId(), errorMsg);
                if (isLogSpml()) {
                    LOG.error("Psp '{}' - Calc XML:\n{}", getId(), toXML(calcResponse));
                }
            }
        }

        // Potentially write the response.
        writeResponse(calcResponse);

        // Stop MDC logging.
        mdc.stop();

        // Return the response.
        return calcResponse;
    }

    /**
     * Execute an {@link CalcRequest} and update the {@link CalcResponse}.
     * 
     * The psp context argument allows for the caching of references during bulk requests.
     * 
     * @param calcRequest the SPML calc request
     * @param calcResponse the SPML calc response
     * @param pspContext the psp context
     */
    public void execute(CalcRequest calcRequest, CalcResponse calcResponse, PspContext pspContext) {

        try {
            // Set the response id.
            calcResponse.setId(calcRequest.getId());

            // provisioning context
            // PspContext pspContext = new PspContext();
            pspContext.setProvisioningServiceProvider(this);
            pspContext.setProvisioningRequest(calcRequest);
            pspContext.setAttributes(null);

            // attribute request context
            BaseSAMLProfileRequestContext attributeRequestContext = new BaseSAMLProfileRequestContext();
            attributeRequestContext.setPrincipalName(calcRequest.getId());

            // get targets specified in request before building the context
            Map<String, List<Pso>> map = getTargetAndObjectDefinitions(calcRequest);

            // determine attribute resolver requested attributes
            LinkedHashSet<String> attributeIds = new LinkedHashSet<String>();
            for (String targetId : map.keySet()) {
                for (Pso psoDefinition : map.get(targetId)) {
                    attributeIds.addAll(psoDefinition.getSourceIds(calcRequest.getReturnData()));
                }
            }
            attributeRequestContext.setRequestedAttributes(attributeIds);

            // resolve attributes
            LOG.debug("PSP '{}' - Calc {} Resolving attributes '{}'.",
                    new Object[] {getId(), calcRequest, attributeIds});
            Map<String, BaseAttribute<?>> attributes = null;
			try{
				attributes = getAttributeAuthority().getAttributes(attributeRequestContext);
			}catch(AttributeResolutionException are){
				if(are.getCause() instanceof ScriptException){
					if(System.getProperty("java.version").contains("1.8.")){
						LOG.error("PSP '{}' - You are using Java {}.  The PSP does NOT support Java versions greater than 1.7. Information on how to work-around this issue: https://wiki.shibboleth.net/confluence/display/SHIB2/IdPJava1.8", new Object[] {getId(),System.getProperty("java.version")});

					}
				}
				throw are; //re-throw this exception 
			}
            LOG.debug("PSP '{}' - Calc {} Resolved attributes '{}'.",
                    new Object[] {getId(), calcRequest, attributes.keySet()});
            pspContext.setAttributes(attributes);

            // get PSOs based on attributes in psp context
            for (String targetId : map.keySet()) {
                for (Pso psoDefinition : map.get(targetId)) {
                    for (PSO pso : psoDefinition.getPSO(pspContext)) {
                        calcResponse.addPSO(pso);
                    }
                }
            }

            if (calcResponse.getPSOs().isEmpty()) {
                fail(calcResponse, ErrorCode.NO_SUCH_IDENTIFIER, "Unable to calculate provisioned object.");
                return;
            }

        } catch (PspException e) {
            fail(calcResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (AttributeRequestException e) {
            fail(calcResponse, ErrorCode.CUSTOM_ERROR, e);
        } catch (Spml2Exception e) {
            fail(calcResponse, ErrorCode.CUSTOM_ERROR, e);
        }
    }

    /** {@inheritDoc} */
    public void execute(DeleteRequest deleteRequest, DeleteResponse deleteResponse) {

        // Get the target definition.
        SpmlTarget target = targets.get(deleteRequest.getPsoID().getTargetID());

        // Execute the request on the target provider.
        Response targetResponse = target.execute(deleteRequest);

        // Fail if the response is not the correct class.
        if (!(targetResponse instanceof DeleteResponse)) {
            fail(deleteResponse, ErrorCode.CUSTOM_ERROR, "Target did not return a DeleteResponse.");
            return;
        }

        if (targetResponse.getStatus().equals(StatusCode.SUCCESS)) {
            // Do nothing.
        } else {
            // If not successful, fail the response.
            fail(deleteResponse, targetResponse.getError(), targetResponse.getErrorMessages());
        }
    }

    /** {@inheritDoc} */
    public DiffResponse execute(DiffRequest diffRequest) {
        return execute(diffRequest, new PspContext());
    }

    /**
     * {@inheritDoc}
     * 
     * The psp context argument allows for the caching of references during bulk requests.
     * 
     * @param pspContext the psp context
     */
    public DiffResponse execute(DiffRequest diffRequest, PspContext pspContext) {

        // Start MDC logging.
        MDCHelper mdc = new MDCHelper(diffRequest).start();

        // Log the request.
        LOG.info("Psp '{}' - Diff {}", getId(), PSPUtil.toString(diffRequest));

        // Log the request as SPML.
        if (isLogSpml()) {
            LOG.info("Psp '{}' - Diff XML:\n{}", getId(), toXML(diffRequest));
        }

        // Potentially write the request.
        writeRequest(diffRequest);

        // Create a new response.
        DiffResponse diffResponse = new DiffResponse();

        // Be optimistic regarding success.
        diffResponse.setStatus(StatusCode.SUCCESS);

        // The response requestID should be the same as the request.
        diffResponse.setRequestID(getOrGenerateRequestID(diffRequest));

        // Validate the request.
        validate(diffRequest, diffResponse);

        // If the validation was successful, execute the request.
        if (diffResponse.getStatus().equals(StatusCode.SUCCESS)) {
            execute(diffRequest, diffResponse, pspContext);
        }

        // If the response is a success, log to INFO.
        if (diffResponse.getStatus().equals(StatusCode.SUCCESS)) {
            LOG.info("Psp '{}' - Diff {}", getId(), PSPUtil.toString(diffResponse));
            if (isLogSpml()) {
                LOG.info("Psp '{}' - Diff XML:\n{}", getId(), toXML(diffResponse));
            }
            // If the response is not a success, log to ERROR.
        } else {
            LOG.error("Psp '{}' - Diff {}", getId(), PSPUtil.toString(diffResponse));
            if (isLogSpml()) {
                LOG.error("Psp '{}' - Diff XML:\n{}", getId(), toXML(diffResponse));
            }
        }

        // Potentially write the response.
        writeResponse(diffResponse);

        // Stop MDC logging.
        mdc.stop();

        // Return the response.
        return diffResponse;
    }

    /**
     * Execute an {@link DiffRequest} and update the {@link DiffResponse}.
     * 
     * The psp context argument allows for the caching of references during bulk requests.
     * 
     * @param diffRequest the SPML diff request
     * @param diffResponse the SPML diff response
     * @param pspContext the psp context
     */
    public void execute(DiffRequest diffRequest, DiffResponse diffResponse, PspContext pspContext) {

        diffResponse.setId(diffRequest.getId());

        // Calculate how the id should be provisioned.
        CalcRequest calcRequest = new CalcRequest();
        calcRequest.setId(diffRequest.getId());
        calcRequest.setRequestID(PSPUtil.uniqueRequestId());
        calcRequest.setReturnData(diffRequest.getReturnData());
        calcRequest.setSchemaEntities(diffRequest.getSchemaEntities());

        // Execute the calc request.
        CalcResponse calcResponse = execute(calcRequest, pspContext);

        if (calcResponse.getStatus().equals(StatusCode.FAILURE)) {
            fail(diffResponse, calcResponse.getError(), calcResponse.getErrorMessages());
            return;
        }

        for (PSO correctPSO : calcResponse.getPSOs()) {

            // Lookup a PSO Identifier to see how it is provisioned.
            LookupRequest lookupRequest = new LookupRequest();
            lookupRequest.setPsoID(correctPSO.getPsoID());
            lookupRequest.setRequestID(PSPUtil.uniqueRequestId());
            lookupRequest.setReturnData(diffRequest.getReturnData());

            LookupResponse lookupResponse = execute(lookupRequest);

            try {
                if (Psp.doesIdentifierExist(lookupResponse)) {
                    // if identifier exists, diff
                    PSO currentPSO = lookupResponse.getPso();

                    List<ModifyRequest> modifyRequests = diff(correctPSO, currentPSO, diffRequest.getReturnData());

                    if (modifyRequests.isEmpty()) {

                        SynchronizedResponse synchronizedResponse = new SynchronizedResponse();
                        synchronizedResponse.setPsoID(currentPSO.getPsoID());
                        diffResponse.addResponse(synchronizedResponse);

                    } else {

                        for (ModifyRequest modifyRequest : modifyRequests) {
                            modifyRequest.setReturnData(diffRequest.getReturnData());
                            diffResponse.addRequest(modifyRequest);
                        }

                    }

                } else {
                    // if identifier does not exist, do we need to rename ?
                    ModifyRequest modifyRequest = renameRequest(correctPSO);

                    // if modify request is not null, rename
                    if (modifyRequest != null) {
                        diffResponse.addRequest(modifyRequest);
                    } else {
                        // if not renaming, add
                        AddRequest addRequest = createAddRequest(correctPSO, diffRequest.getReturnData());
                        diffResponse.addRequest(addRequest);
                    }
                }
            } catch (Spml2Exception e) {
                fail(diffResponse, ErrorCode.CUSTOM_ERROR, e);
            } catch (PspException e) {
                fail(diffResponse, ErrorCode.CUSTOM_ERROR, e);
            }
        }
    }

    /**
     * Execute a list targets request and return the response.
     * 
     * @param listTargetsRequest the list targets request
     * @return the list targets response
     */
    public ListTargetsResponse execute(ListTargetsRequest listTargetsRequest) {

        MDCHelper mdc = new MDCHelper(listTargetsRequest).start();
        LOG.info("Psp '{}' - ListTargets {}", getId(), listTargetsRequest);
        if (isLogSpml()) {
            LOG.info("Psp '{}' - ListTargets XML\n{}", getId(), toXML(listTargetsRequest));
        }
        writeRequest(listTargetsRequest);

        ListTargetsResponse listTargetsResponse = new ListTargetsResponse();
        listTargetsResponse.setStatus(StatusCode.SUCCESS);
        listTargetsResponse.setRequestID(getOrGenerateRequestID(listTargetsRequest));

        try {
            for (String targetId : objects.keySet()) {
                listTargetsResponse.addTarget(getSpmlTarget(targetId));
            }
        } catch (Spml2Exception e) {
            // FUTURE UNSUPPORTED_PROFILE instead of CUSTOM_ERROR as appropriate
            fail(listTargetsResponse, ErrorCode.CUSTOM_ERROR, e);
        }

        if (listTargetsResponse.getStatus().equals(StatusCode.SUCCESS)) {
            LOG.info("Psp '{}' - ListTargets {}", getId(), PSPUtil.toString(listTargetsResponse));
        } else {
            LOG.error("Psp '{}' - ListTargets {}", getId(), PSPUtil.toString(listTargetsResponse));
        }
        if (isLogSpml()) {
            LOG.info("Psp '{}' - ListTargets XML\n{}", getId(), toXML(listTargetsResponse));
        }
        mdc.stop();
        writeResponse(listTargetsResponse);
        return listTargetsResponse;
    }

    /** {@inheritDoc} */
    public void execute(LookupRequest lookupRequest, LookupResponse lookupResponse) {

        // Get the target definition.
        SpmlTarget target = targets.get(lookupRequest.getPsoID().getTargetID());

        // Execute the request on the target provider.
        Response targetResponse = target.execute(lookupRequest);

        // Fail if the response is not the correct class.
        if (!(targetResponse instanceof LookupResponse)) {
            fail(lookupResponse, ErrorCode.CUSTOM_ERROR, "Target did not return a LookupResponse.");
            return;
        }

        // If successful, set the PSO.
        if (targetResponse.getStatus().equals(StatusCode.SUCCESS)) {
            lookupResponse.setPso(((LookupResponse) targetResponse).getPso());
        } else {
            // If not successful, fail the response.
            fail(lookupResponse, targetResponse.getError(), targetResponse.getErrorMessages());
        }
    }

    /** {@inheritDoc} */
    public void execute(ModifyRequest modifyRequest, ModifyResponse modifyResponse) {

        // Get the target definition.
        SpmlTarget target = targets.get(modifyRequest.getPsoID().getTargetID());

        // Execute the request on the target provider.
        Response targetResponse = target.execute(modifyRequest);

        // Fail if the response is not the correct class.
        if (!(targetResponse instanceof ModifyResponse)) {
            fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Target did not return a ModifyResponse.");
            return;
        }

        // If successful, set the PSO.
        if (targetResponse.getStatus().equals(StatusCode.SUCCESS)) {
            modifyResponse.setPso(((ModifyResponse) targetResponse).getPso());
        } else {
            // If not successful, fail the response.
            fail(modifyResponse, targetResponse.getError(), targetResponse.getErrorMessages());
        }
    }

    /** {@inheritDoc} */
    public void execute(SearchRequest searchRequest, SearchResponse searchResponse) {

        // Get the target definition.
        SpmlTarget target = targets.get(searchRequest.getQuery().getTargetID());

        // Execute the request on the target provider.
        Response targetResponse = target.execute(searchRequest);

        // Fail if the response is not the correct class.
        if (!(targetResponse instanceof SearchResponse)) {
            fail(searchResponse, ErrorCode.CUSTOM_ERROR, "Target did not return a SearchResponse.");
            return;
        }

        // If successful, add the PSOs.
        if (targetResponse.getStatus().equals(StatusCode.SUCCESS)) {
            for (PSO pso : ((SearchResponse) targetResponse).getPSOs()) {
                searchResponse.addPSO(pso);
            }
        } else {
            // If not successful, fail the response.
            fail(searchResponse, targetResponse.getError(), targetResponse.getErrorMessages());
        }
    }

    /** {@inheritDoc} */
    public SyncResponse execute(SyncRequest syncRequest) {
        return execute(syncRequest, new PspContext());
    }

    /**
     * {@inheritDoc}
     * 
     * The psp context argument allows for the caching of references during bulk requests.
     * 
     * @param pspContext the psp context
     */
    public SyncResponse execute(SyncRequest syncRequest, PspContext pspContext) {

        // Start MDC logging.
        MDCHelper mdc = new MDCHelper(syncRequest).start();

        // Log the request.
        LOG.info("Psp '{}' - Sync {}", getId(), PSPUtil.toString(syncRequest));

        // Log the request as SPML.
        if (isLogSpml()) {
            LOG.info("Psp '{}' - Sync SPML:\n{}", getId(), toXML(syncRequest));
        }

        // Potentially write the request.
        writeRequest(syncRequest);

        // Create a new response.
        SyncResponse syncResponse = new SyncResponse();

        // Be optimistic regarding success.
        syncResponse.setStatus(StatusCode.SUCCESS);

        // The response requestID should be the same as the request.
        syncResponse.setRequestID(getOrGenerateRequestID(syncRequest));

        // Validate the request.
        validate(syncRequest, syncResponse);

        // If the validation was successful, execute the request.
        if (syncResponse.getStatus().equals(StatusCode.SUCCESS)) {
            execute(syncRequest, syncResponse, pspContext);
        }

        // If the response is a success, log to INFO.
        if (syncResponse.getStatus().equals(StatusCode.SUCCESS)) {
            LOG.info("Psp '{}' - Sync {}", getId(), PSPUtil.toString(syncResponse));
            if (isLogSpml()) {
                LOG.info("Psp '{}' - Sync SPML:\n{}", getId(), toXML(syncResponse));
            }
            // If the response is not a success, log to ERROR.
        } else {
            LOG.error("Psp '{}' - Sync {}", getId(), PSPUtil.toString(syncResponse));
            if (isLogSpml()) {
                LOG.error("Psp '{}' - Sync SPML:\n{}", getId(), toXML(syncResponse));
            }
        }

        // Potentially write the response.
        writeResponse(syncResponse);

        // Stop MDC logging.
        mdc.stop();

        // Return the response.
        return syncResponse;
    }

    /**
     * Execute an {@link SyncRequest} and update the {@link SyncResponse}.
     * 
     * @param syncRequest the SPML sync request
     * @param syncResponse the SPML sync response
     */
    public void execute(SyncRequest syncRequest, SyncResponse syncResponse, PspContext pspContext) {

        // Set the response id.
        syncResponse.setId(syncRequest.getId());

        // Create a diff request.
        DiffRequest diffRequest = new DiffRequest();
        diffRequest.setId(syncRequest.getId());
        diffRequest.setRequestID(PSPUtil.uniqueRequestId());
        diffRequest.setReturnData(syncRequest.getReturnData());
        diffRequest.setSchemaEntities(syncRequest.getSchemaEntities());

        // Execute the diff request.
        DiffResponse diffResponse = execute(diffRequest, pspContext);

        // Store the diff response.
        syncResponse.setDiffResponse(diffResponse);

        // Return if the diff response is not successful.
        if (!diffResponse.getStatus().equals(StatusCode.SUCCESS)) {
            fail(syncResponse, diffResponse.getError(), diffResponse.getErrorMessages());
            return;
        }

        try {
            // Execute the requests in the diff response.
            for (Request request : diffResponse.getRequests()) {

                Response response = execute(request);

                syncResponse.addResponse(response);

                if (request instanceof DeleteRequest) {
                    syncResponse.setId(((DeleteRequest) request).getPsoID().getID());
                }

                if (response.getStatus().equals(StatusCode.FAILURE)) {
                    fail(syncResponse, response.getError(), response.getErrorMessages());
                    return;
                }
            }

            for (SynchronizedResponse synchronizedResponse : diffResponse.getSynchronizedResponses()) {
                syncResponse.addResponse(synchronizedResponse);
            }

        } catch (PspException e) {
            fail(syncResponse, ErrorCode.CUSTOM_ERROR, e);
            return;
        } catch (Spml2Exception e) {
            fail(syncResponse, ErrorCode.CUSTOM_ERROR, e);
            return;
        }
    }

    /**
     * This method returns all source object identifiers. The map keys are the identifiers, and the map values are the
     * {@link SchemaEntityRef}s applicable for each identifier.
     * 
     * The identifiers are returned by the attribute resolver via a {@link BulkCalcRequest} whose return data is
     * "identifier".
     * 
     * @param bulkProvisioningRequest the bulk provisioning request
     * @return a possibly empty map consisting of all source identifiers and their corresponding provisioned objects
     * @throws PspException
     * @throws AttributeRequestException
     */
    public Map<String, List<SchemaEntityRef>> getAllSourceIdentifiers(BulkProvisioningRequest bulkProvisioningRequest)
            throws PspException, AttributeRequestException {

        BulkCalcRequest bulkCalcRequest = new BulkCalcRequest();
        bulkCalcRequest.setSchemaEntities(bulkProvisioningRequest.getSchemaEntities());
        bulkCalcRequest.setReturnData(ReturnData.IDENTIFIER);
        bulkCalcRequest.setId(BulkProvisioningRequest.BULK_REQUEST_ID);

        // provisioning context
        PspContext pspContext = new PspContext();
        pspContext.setProvisioningServiceProvider(this);
        pspContext.setProvisioningRequest(bulkCalcRequest);

        // attribute request context
        BaseSAMLProfileRequestContext attributeRequestContext = new BaseSAMLProfileRequestContext();
        attributeRequestContext.setPrincipalName(bulkCalcRequest.getId());

        // get targets specified in request before building the context
        Map<String, List<Pso>> map = getTargetAndObjectDefinitions(bulkCalcRequest);

        // determine attribute resolver requested attributes
        LinkedHashSet<String> attributeIds = new LinkedHashSet<String>();
        for (String psoTargetDefinition : map.keySet()) {
            for (Pso psoDefinition : map.get(psoTargetDefinition)) {
                if (!DatatypeHelper.isEmpty(psoDefinition.getAllSourceIdentifiersRef())) {
                    attributeIds.add(DatatypeHelper.safeTrim(psoDefinition.getAllSourceIdentifiersRef()));
                }
            }
        }
        attributeRequestContext.setRequestedAttributes(attributeIds);

        // return null if there are no attribute ids to resovle
        if (attributeIds.isEmpty()) {
            LOG.debug("PSP '{}' - No source identifier refs are configured.");
            return null;
        }

        // resolve attributes
        LOG.debug("PSP '{}' - Calc {} Resolving attributes '{}'.",
                new Object[] {getId(), bulkCalcRequest, attributeIds});
        Map<String, BaseAttribute<?>> attributes = getAttributeAuthority().getAttributes(attributeRequestContext);
        LOG.debug("PSP '{}' - Calc {} Resolved attributes '{}'.", new Object[] {getId(), bulkCalcRequest, attributeIds});
        pspContext.setAttributes(attributes);

        Map<String, List<SchemaEntityRef>> identifierMap = new LinkedHashMap<String, List<SchemaEntityRef>>();

        for (String targetId : map.keySet()) {
            for (Pso psoDefinition : map.get(targetId)) {
                String allSourceIdentifiersRef = psoDefinition.getAllSourceIdentifiersRef();
                if (DatatypeHelper.isEmpty(allSourceIdentifiersRef)) {
                    continue;
                }

                BaseAttribute attribute = attributes.get(allSourceIdentifiersRef);
                if (attribute == null) {
                    LOG.warn("PSP '{}' - Unable to resolve attribute '{}'", getId(), allSourceIdentifiersRef);
                    continue;
                }

                for (Object value : attribute.getValues()) {
                    if (value == null) {
                        throw new PspException("TODO null value");
                    }

                    String id = null;
                    if (value instanceof PSOIdentifier) {
                        id = ((PSOIdentifier) value).getID();
                    } else {
                        id = value.toString();
                    }

                    if (!identifierMap.containsKey(id)) {
                        identifierMap.put(id, new ArrayList<SchemaEntityRef>());
                    }

                    SchemaEntityRef entity = new SchemaEntityRef();
                    entity.setEntityName(psoDefinition.getId());
                    entity.setTargetID(targetId);

                    identifierMap.get(id).add(entity);
                }
            }
        }

        return identifierMap;
    }

    /**
     * Search for all known identifiers of a target during a bulk diff request. The search request is created using the
     * filter returned from the identifying attributes. The identifiers are returned in an order suitable for deletion.
     * If the pso is not authoritative it is omitted. If a container id is included in the search request, it is omitted
     * from the returned identifiers.
     * 
     * @param bulkProvisioningRequest the bulk request
     * @param provisioningResponse the bulk response
     * @return the target pso identifiers in order suitable for deletion
     * @throws PspException
     * @throws DSMLProfileException
     */
    public Set<PSOIdentifier> getAllTargetIdentifiers(BulkProvisioningRequest bulkProvisioningRequest,
            ProvisioningResponse provisioningResponse) throws PspException, DSMLProfileException {

        // the pso ids which currently exist
        Set<PSOIdentifier> currentPsoIds = new LinkedHashSet<PSOIdentifier>();

        // the targets and objects applicable to the request
        Map<String, List<Pso>> map = getTargetAndObjectDefinitions(bulkProvisioningRequest);

        // for every target id
        for (String targetId : map.keySet()) {

            // the pso ids which currently exist for each target
            Set<PSOIdentifier> targetPsoIds = new LinkedHashSet<PSOIdentifier>();

            // get the target
            SpmlTarget target = targets.get(targetId);

            // for every pso
            for (Pso psoDefinition : map.get(targetId)) {

                // if authoritative, get the PSOIdentifiers from the target provider
                if (!psoDefinition.isAuthoritative()) {
                    continue;
                }

                // if no identifying attribute nor query, skip the pso
                if (psoDefinition.getPsoIdentifyingAttribute() == null
                        || psoDefinition.getAllTargetIdentifiersQuery() == null) {
                    continue;
                }

                // create a search request from the identifying attribute
                SearchRequest searchRequest = new SearchRequest();
                searchRequest.setRequestID(PSPUtil.uniqueRequestId());
                searchRequest.setReturnData(ReturnData.IDENTIFIER);
                Query query = psoDefinition.getAllTargetIdentifiersQuery();
                searchRequest.setQuery(query);

                // execute the search request
                Response searchResponse = target.execute(searchRequest);

                if (!(searchResponse instanceof SearchResponse)) {
                    fail(provisioningResponse, ErrorCode.CUSTOM_ERROR, "Target did not return a SearchResponse.");
                    return null;
                }
                if (!searchResponse.getStatus().equals(StatusCode.SUCCESS)) {
                    // If not successful, fail the response.
                    fail(provisioningResponse, searchResponse.getError(), searchResponse.getErrorMessages());
                    return null;
                }

                // gather the pso identifiers
                for (PSO pso : ((SearchResponse) searchResponse).getPSOs()) {
                    // do not return the container id if it is specified
                    if (psoDefinition.getPsoIdentifier().getContainerId() == null) {
                        targetPsoIds.add(pso.getPsoID());
                    } else if (!psoDefinition.getPsoIdentifier().getContainerId().equals(pso.getPsoID().getID())) {
                        targetPsoIds.add(pso.getPsoID());
                    }
                }
            }

            // order for deletion
            currentPsoIds.addAll(target.orderForDeletion(targetPsoIds));
        }

        return currentPsoIds;
    }

    /**
     * Get the attribute authority used to calculate provisioned objects.
     * 
     * @return the attribute authority
     */
    public AttributeAuthority getAttributeAuthority() {
        return attributeAuthority;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /**
     * Return the names of all target attributes, references, identifiers, etc. for the given target id and return data.
     * 
     * If return data is identifier, the names of identifier definitions and alternate identifier definitions are
     * returned.
     * 
     * If return data is data, the names of identifiers as well as attributes are returned.
     * 
     * If return data is everything, the names of identifiers, attributes, and references are returned.
     * 
     * @param targetId the target id
     * @param returnData return data
     * @return possibly empty set of attribute, reference, and identifier names
     */
    public Set<String> getNames(String targetId, ReturnData returnData) {

        Set<String> names = new LinkedHashSet<String>();

        List<Pso> psoDefinitions = objects.get(targetId);
        if (psoDefinitions != null) {
            for (Pso psoDefinition : psoDefinitions) {
                PsoIdentifyingAttribute ia = psoDefinition.getPsoIdentifyingAttribute();
                if (ia != null) {
                    names.add(ia.getName());
                }
                if (returnData.equals(ReturnData.DATA) || returnData.equals(ReturnData.EVERYTHING)) {
                    names.addAll(psoDefinition.getAttributeNames());
                }
                if (returnData.equals(ReturnData.EVERYTHING)) {
                    names.addAll(psoDefinition.getReferenceNames());
                }
            }
        }

        return names;
    }

    /**
     * Return the pso definition with the given target id and object id (entity name) or null.
     * 
     * @param targetId the target id
     * @param objectId the object id (entity name)
     * @return the pso definition or null
     */
    public Pso getPso(String targetId, String objectId) {

        if (targetId == null || objectId == null) {
            return null;
        }

        if (!objects.containsKey(targetId)) {
            return null;
        }

        Pso psoDefinitionToReturn = null;

        for (Pso psoDefinition : objects.get(targetId)) {
            if (psoDefinition.getId().equals(objectId)) {
                psoDefinitionToReturn = psoDefinition;
            }
        }

        return psoDefinitionToReturn;
    }

    /**
     * Returns a list of pso definitions for the given target id. Returns null if the target id is null or the target id
     * is unknown.
     * 
     * @param targetId the target id
     * @return list of pso definitions for the given target id or null
     */
    public List<Pso> getPsos(String targetId) {
        if (targetId == null) {
            return null;
        }

        if (!objects.containsKey(targetId)) {
            return null;
        }

        return Collections.unmodifiableList(objects.get(targetId));
    }

    /**
     * @return Returns the pspOptions.
     */
    public PspOptions getPspOptions() {
        return pspOptions;
    }

    /**
     * Return the SPMLv2 target object for the given target id suitable for inclusion in a list targets request.
     * 
     * @param targetId the target id
     * @return the SPMLv2 target object or null if the target id is unknown.
     * @throws Spml2Exception if an SPMLv2 toolkit error occursF
     */
    public org.openspml.v2.msg.spml.Target getSpmlTarget(String targetId) throws Spml2Exception {

        if (!objects.containsKey(targetId)) {
            return null;
        }

        org.openspml.v2.msg.spml.Target target = new org.openspml.v2.msg.spml.Target();
        target.setTargetID(targetId);
        // FUTURE support XSD ?
        target.setProfile(new DSMLProfileRegistrar().getProfileURI());

        Schema schema = new Schema();

        // FUTURE support other schemas ?
        DSMLSchema dsmlSchema = new DSMLSchema();

        CapabilitiesList cl = new CapabilitiesList();

        LinkedHashMap<String, SchemaEntityRef> schemaEntityRefMap = new LinkedHashMap<String, SchemaEntityRef>();

        for (Pso psoDefinition : objects.get(targetId)) {
            SchemaEntityRef entity = new SchemaEntityRef();
            entity.setEntityName(psoDefinition.getId());
            entity.setTargetID(getId());
            schemaEntityRefMap.put(entity.getEntityName(), entity);

            schema.addSupportedSchemaEntity(entity);

            ObjectClassDefinition objectClassDef = new ObjectClassDefinition();
            objectClassDef.setName(psoDefinition.getId());

            AttributeDefinitionReferences attrRefs = new AttributeDefinitionReferences();

            for (PsoAttribute psoAttributeDefinition : psoDefinition.getPsoAttributes()) {
                AttributeDefinition attrDef = new AttributeDefinition();
                attrDef.setName(psoAttributeDefinition.getName());
                dsmlSchema.addAttributeDefinition(attrDef);

                AttributeDefinitionReference attrDefRef = new AttributeDefinitionReference();
                attrDefRef.setName(psoAttributeDefinition.getName());
                // FUTURE attrRef.setRequired(required);
                attrRefs.addAttributeDefinitionReference(attrDefRef);
            }

            objectClassDef.setMemberAttributes(attrRefs);
            dsmlSchema.addObjectClassDefinition(objectClassDef);
        }

        for (Pso psoDefinition : objects.get(targetId)) {
            for (PsoReferences psoReferencesDefinition : psoDefinition.getReferences()) {
                for (PsoReference psoReferenceDefinition : psoReferencesDefinition.getPsoReferences()) {
                    SchemaEntityRef fromEntity = schemaEntityRefMap.get(psoDefinition.getId());
                    SchemaEntityRef toEntity = schemaEntityRefMap.get(psoReferenceDefinition.getToObject().getId());

                    Capability capability = new Capability();
                    capability.setNamespaceURI(PsoReferences.REFERENCE_URI);
                    capability.addAppliesTo(fromEntity);
                    cl.addCapability(capability);

                    ReferenceDefinition rd = new ReferenceDefinition();
                    rd.setTypeOfReference(psoReferencesDefinition.getName());
                    rd.setSchemaEntity(fromEntity);
                    rd.addCanReferTo(toEntity);

                    OCEtoMarshallableAdapter oce = new OCEtoMarshallableAdapter(rd);
                    capability.addOpenContentElement(oce);
                }
            }
        }

        target.setCapabilities(cl);
        schema.addOpenContentElement(dsmlSchema);
        target.addSchema(schema);

        return target;
    }

    /**
     * Return the {@link SpmlTarget} with the given target id or null.
     * 
     * @param targetId the target id
     * @return the {@link SpmlTarget} or null.
     */
    public SpmlTarget getTarget(String targetId) {
        return targets.get(targetId);
    }

    /**
     * Return a map whose keys are target IDs and whose values are lists of PSO definitions applicable to the schema
     * entity of the supplied provisioning request.
     * 
     * @param request the provisioning request
     * @return map whose keys are target IDs and values are lists of PSO definitions
     * @throws PspException if the schema entity target id or entity name is unknown
     */
    public Map<String, List<Pso>> getTargetAndObjectDefinitions(ProvisioningRequest request) throws PspException {

        Map<String, List<Pso>> map = new LinkedHashMap<String, List<Pso>>();

        if (request.getSchemaEntities().isEmpty()) {

            map.putAll(getTargetAndObjectDefinitions(new SchemaEntityRef()));

        } else {

            for (SchemaEntityRef schemaEntityRef : request.getSchemaEntities()) {
                map.putAll(getTargetAndObjectDefinitions(schemaEntityRef));
            }
        }
        return map;
    }

    /**
     * Return a map whose keys are target IDs and whose values are lists of PSO definitions applicable to the given
     * schema entity.
     * 
     * @param schemaEntityRef the schema entity
     * @return map whose keys are target IDs and values are lists of PSO definitions
     * @throws PspException if the schema entity target id or entity name is unknown
     */
    public Map<String, List<Pso>> getTargetAndObjectDefinitions(SchemaEntityRef schemaEntityRef) throws PspException {

        LOG.trace("PSP '{}' - Get pso definitions for schema entity '{}'", getId(), PSPUtil.toString(schemaEntityRef));

        Map<String, List<Pso>> map = new LinkedHashMap<String, List<Pso>>();

        if (DatatypeHelper.isEmpty(schemaEntityRef.getTargetID())
                && DatatypeHelper.isEmpty(schemaEntityRef.getEntityName())) {

            map = Collections.unmodifiableMap(objects);

        } else if (DatatypeHelper.isEmpty(schemaEntityRef.getTargetID())) {

            for (String targetId : objects.keySet()) {
                for (Pso psoDefinition : objects.get(targetId)) {
                    if (psoDefinition.getId().equals(schemaEntityRef.getEntityName())) {
                        map.put(targetId, new ArrayList<Pso>());
                        map.get(targetId).add(psoDefinition);
                    }
                }
            }

            if (map.isEmpty()) {
                LOG.error("Unknown pso id '" + schemaEntityRef.getEntityName() + "'");
                throw new PspException("Unknown pso id '" + schemaEntityRef.getEntityName() + "'");
            }

        } else if (DatatypeHelper.isEmpty(schemaEntityRef.getEntityName())) {

            if (!objects.containsKey(schemaEntityRef.getTargetID())) {
                LOG.error("Unknown target id '" + schemaEntityRef.getTargetID() + "'");
                throw new PspException("Unknown target id '" + schemaEntityRef.getTargetID() + "'");
            }

            map.put(schemaEntityRef.getTargetID(), objects.get(schemaEntityRef.getTargetID()));

        } else {

            if (!objects.containsKey(schemaEntityRef.getTargetID())) {
                LOG.error("Unknown target id '" + schemaEntityRef.getTargetID() + "'");
                throw new PspException("Unknown target id '" + schemaEntityRef.getTargetID() + "'");
            }

            for (Pso psoDefinition : objects.get(schemaEntityRef.getTargetID())) {
                if (psoDefinition.getId().equals(schemaEntityRef.getEntityName())) {
                    map.put(schemaEntityRef.getTargetID(), new ArrayList<Pso>());
                    map.get(schemaEntityRef.getTargetID()).add(psoDefinition);
                }
            }

            if (map.isEmpty()) {
                LOG.error("Unknown pso id '" + schemaEntityRef.getEntityName() + "'");
                throw new PspException("Unknown pso id '" + schemaEntityRef.getEntityName() + "'");
            }
        }

        LOG.trace("PSP '{}' - Get pso definitions for schema entity '{}' found {}",
                new Object[] {getId(), PSPUtil.toString(schemaEntityRef), map});
        return map;
    }

    /**
     * Return true if the given {@link PSOIdentifier} has an attribute with the given name and value.
     * 
     * @param psoID the pso identifier
     * @param attributeName the attribute name
     * @param attributeValue the attribute value
     * @return true if the pso identifier has the attribute, false otherwise
     * @throws PspException if the spml search fails
     * @throws DSMLProfileException if a dsml error occurs
     * @throws PspNoSuchIdentifierException if the psoID can not be found
     */
    public boolean hasAttribute(PSOIdentifier psoID, String attributeName, String attributeValue) throws PspException,
            DSMLProfileException, PspNoSuchIdentifierException {

        LOG.debug("Psp '{}' - Has attribute '{}' name '{}' value '{}'", new Object[] {getId(), PSPUtil.toString(psoID),
                attributeName, attributeValue,});

        EqualityMatch equalityMatch = new EqualityMatch(attributeName, attributeValue);
        Filter filter = new Filter();
        filter.setItem(equalityMatch);

        Query query = new Query();
        query.setBasePsoID(psoID);
        query.setTargetID(psoID.getTargetID());
        query.addQueryClause(filter);
        query.setScope(Scope.PSO);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setReturnData(ReturnData.IDENTIFIER);
        searchRequest.setQuery(query);
        searchRequest.setRequestID(PSPUtil.uniqueRequestId());

        SearchResponse response = execute(searchRequest);

        if (!response.getStatus().equals(StatusCode.SUCCESS)) {
            String errorMessage = PSPUtil.toString(response);
            LOG.error(errorMessage);

            if (response.getStatus().equals(StatusCode.FAILURE)
                    && response.getError().equals(ErrorCode.NO_SUCH_IDENTIFIER)) {
                throw new PspNoSuchIdentifierException(errorMessage);
            }

            throw new PspException(errorMessage);
        }

        if (response.getPSOs().length > 0) {
            LOG.debug("Psp '{}' - Has attribute true '{}' name '{}' value '{}'",
                    new Object[] {getId(), PSPUtil.toString(psoID), attributeName, attributeValue,});
            return true;
        }

        LOG.debug("Psp '{}' - Has attribute false '{}' name '{}' value '{}'",
                new Object[] {getId(), PSPUtil.toString(psoID), attributeName, attributeValue,});
        return false;
    }

    /**
     * Return true if the given {@link PSOIdentifier} has the given {@Reference}.
     * 
     * @param psoID the pso identifier
     * @param reference the reference
     * @return true if the pso identifier has the reference, false otherwise
     * @throws PspException if the spml search fails
     * @throws PspNoSuchIdentifierException if the psoID can not be found
     */
    public boolean hasReference(PSOIdentifier psoID, Reference reference) throws PspException,
            PspNoSuchIdentifierException {

        LOG.debug("Psp '{}' - Has reference from '{}' to '{}'",
                new Object[] {getId(), PSPUtil.toString(psoID), PSPUtil.toString(reference),});

        HasReference hasReference = new HasReference();
        hasReference.setToPsoID(reference.getToPsoID());
        hasReference.setTypeOfReference(reference.getTypeOfReference());
        hasReference.setReferenceData(reference.getReferenceData());

        Query query = new Query();
        query.setBasePsoID(psoID);
        query.setTargetID(psoID.getTargetID());
        query.addQueryClause(hasReference);
        query.setScope(Scope.PSO);

        // SPML library namespace marshalling issue
        // SearchRequest searchRequest = new SearchRequest();
        SearchRequest searchRequest = new SearchRequestWithQueryClauseNamespaces();

        searchRequest.setReturnData(ReturnData.IDENTIFIER);
        searchRequest.setQuery(query);
        searchRequest.setRequestID(PSPUtil.uniqueRequestId());

        SearchResponse response = execute(searchRequest);

        if (!response.getStatus().equals(StatusCode.SUCCESS)) {
            String errorMessage =
                    "Psp '" + getId() + "' - Has reference from '" + PSPUtil.toString(psoID) + "' to '"
                            + PSPUtil.toString(reference) + "' " + PSPUtil.toString(response);
            LOG.error(errorMessage);

            if (response.getStatus().equals(StatusCode.FAILURE)
                    && response.getError().equals(ErrorCode.NO_SUCH_IDENTIFIER)) {
                throw new PspNoSuchIdentifierException(errorMessage);
            }

            throw new PspException(errorMessage);
        }

        if (response.getPSOs().length > 0) {
            LOG.debug("Psp '{}' - Has reference true from '{}' to '{}'", new Object[] {getId(),
                    PSPUtil.toString(psoID), PSPUtil.toString(reference),});
            return true;
        }

        LOG.debug("Psp '{}' - Has reference false from '{}' to '{}'", new Object[] {getId(), PSPUtil.toString(psoID),
                PSPUtil.toString(reference),});
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * Initialize the SPMLv2 toolkit marshallers/unmarshallers.
     */
    public void initialize() throws ServiceException {

        super.initialize();

        this.initializeSpmlToolkit();
    }

    /**
     * Initialize the SPMLv2 toolkit marshallers/unmarshallers.
     * 
     * {@link PspMarshallableCreator} {@link DSMLUnmarshaller}
     */
    public void initializeSpmlToolkit() {

        ObjectFactory.getInstance().addCreator(new PspMarshallableCreator());
        ObjectFactory.getInstance().addOCEUnmarshaller(new DSMLUnmarshaller());
    }

    /**
     * {@inheritDoc}
     * 
     * Initialize the "objects" map whose keys are target ids and values are {@link Pso}s. Initialize the "targets" map
     * whose keys are target ids and values are {@link SpmlTarget}s.
     */
    protected void onNewContextCreated(ApplicationContext newServiceContext) throws ServiceException {

        Map<String, List<Pso>> oldPsoDefinitions = objects;
        Map<String, SpmlTarget> oldTargets = targets;

        try {
            String[] psoBeanNames = newServiceContext.getBeanNamesForType(Pso.class);
            LOG.debug("PSP '{}' - Loading {} PSO definitions", getId(), Arrays.asList(psoBeanNames));
            objects = new LinkedHashMap<String, List<Pso>>(psoBeanNames.length);
            for (String beanName : psoBeanNames) {
                Pso psoDefinition = (Pso) newServiceContext.getBean(beanName);
                String targetId = psoDefinition.getPsoIdentifier().getTargetId();
                if (!objects.containsKey(targetId)) {
                    objects.put(targetId, new ArrayList<Pso>());
                }
                objects.get(targetId).add(psoDefinition);
            }
            targets = new LinkedHashMap<String, SpmlTarget>(objects.keySet().size());
            for (String targetId : objects.keySet()) {
                Object target = newServiceContext.getBean(targetId, SpmlTarget.class);
                ((SpmlTarget) target).setPSP(this);
                targets.put(targetId, (SpmlTarget) target);
            }
        } catch (Exception e) {
            objects = oldPsoDefinitions;
            targets = oldTargets;
            LOG.error("PSP '" + getId() + "' - Configuration is not valid, retaining old configuration", e);
            throw new ServiceException("PSP '" + getId()
                    + "' - Configuration is not valid, retaining old configuration", e);
        }
    }

    /**
     * Returns a {@link ModifyRequest} suitable for renaming the {@PSOIdentifier} of the given
     * {@link PSO} . Returns null if the object can not or should not be renamed.
     * 
     * The modify request must contain one and only one {@link AlternateIdentifier}. This alternate identifier is the
     * "old" or "current" identifier.
     * 
     * The "new" identifier is the pso identifier of the given pso.
     * 
     * If the "new" identifier already exists, or the "old" identifier does not exist, then return null.
     * 
     * @param pso the provisioned object
     * @return the modify request or null if the object should not or can not be renamed
     * @throws PspException
     */
    public ModifyRequest renameRequest(PSO pso) throws PspException {

        LOG.debug("PSP '{}' - Rename {}", getId(), PSPUtil.toString(pso));

        // get alternate identifiers from correct pso
        List<AlternateIdentifier> oldIDs = pso.getOpenContentElements(AlternateIdentifier.class);

        // do not rename if there are no alternate identifiers
        if (oldIDs.isEmpty()) {
            LOG.debug("PSP '{}' - Rename {} Will not rename. One alternate identifier is required.", getId(),
                    PSPUtil.toString(pso));
            return null;
        }

        // throw exception if more than one alternate identifier
        if (oldIDs.size() > 1) {
            LOG.warn("PSP '{}' - Rename {} Will not rename. Multiple alternate identifiers are not supported.",
                    getId(), PSPUtil.toString(pso));
            return null;
        }

        PSOIdentifier newPSOID = pso.getPsoID();

        // Do nothing if identifier exists.
        if (doesIdentifierExist(newPSOID)) {
            LOG.warn("PSP '{}' - Rename {} Will not rename. New identifier already exists.", getId(),
                    PSPUtil.toString(pso));
            return null;
        }

        // Lookup alternate identifiers to see if they exist.
        List<AlternateIdentifier> foundOldIDs = new ArrayList<AlternateIdentifier>();

        for (AlternateIdentifier oldID : oldIDs) {
            if (doesIdentifierExist(oldID.getPSOIdentifier())) {
                foundOldIDs.add(oldID);
            }
        }

        // Do nothing if old identifiers do not exist.
        if (foundOldIDs.isEmpty()) {
            LOG.warn("PSP '{}' - Rename {} Will not rename. Alternate identifier must exist on target.", getId(),
                    PSPUtil.toString(pso));
            return null;
        }

        // Throw exception if more than one old identifier is found.
        if (foundOldIDs.size() > 1) {
            LOG.warn("PSP '{}' - Rename {} Will not rename. Multiple alternate identifiers exist on target.", getId(),
                    PSPUtil.toString(pso));
            return null;
        }

        PSOIdentifier oldPSOID = foundOldIDs.get(0).getPSOIdentifier();

        // rename
        ModifyRequest modifyRequest = new ModifyRequest();
        modifyRequest.setPsoID(oldPSOID);
        modifyRequest.setRequestID(PSPUtil.uniqueRequestId());

        Modification modification = new Modification();
        AlternateIdentifier newPsoID = new AlternateIdentifier();
        newPsoID.setPSOIdentifier(newPSOID);
        modification.addOpenContentElement(newPsoID);
        modification.setModificationMode(ModificationMode.REPLACE);
        modifyRequest.addModification(modification);

        LOG.warn("PSP '{}' - Rename {} Will rename with modify request {}",
                new Object[] {getId(), PSPUtil.toString(pso), PSPUtil.toString(modifyRequest)});

        return modifyRequest;
    }

    /**
     * Set the attribute authority
     * 
     * @param attributeAuthority the attribute authority
     */
    public void setAttributeAuthority(AttributeAuthority attributeAuthority) {
        this.attributeAuthority = attributeAuthority;
    }

    /** {@inheritDoc} */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param pspOptions The pspOptions to set.
     */
    public void setPspOptions(PspOptions pspOptions) {
        this.pspOptions = pspOptions;
    }

    /**
     * Return a <code>ModifyRequest</code> for every data <code>Modification</code>.
     * 
     * @param dataMods the <code>Modification</code>s
     * @param psoID the PSO Identifier
     * @param entityName the schema entity name
     * @return the <code>ModifyRequest</code>s
     * @throws Spml2Exception if an error occurs creating the <code>DSMLModification</code>s
     * 
     */
    public List<ModifyRequest> unbundleDataModifications(List<Modification> dataMods, PSOIdentifier psoID,
            String entityName) throws Spml2Exception {
        List<ModifyRequest> unbundledModifyRequests = new ArrayList<ModifyRequest>();

        for (Modification modification : dataMods) {

            for (Object object : modification.getOpenContentElements(DSMLModification.class)) {
                DSMLModification dsmlModification = (DSMLModification) object;
                DSMLValue[] dsmlValues = dsmlModification.getValues();
                for (DSMLValue dsmlValue : dsmlValues) {
                    ModifyRequest unbundledModifyRequest = new ModifyRequest();
                    unbundledModifyRequest.setRequestID(PSPUtil.uniqueRequestId());
                    unbundledModifyRequest.setPsoID(psoID);
                    if (entityName != null) {
                        unbundledModifyRequest.addOpenContentAttr(Pso.ENTITY_NAME_ATTRIBUTE, entityName);
                    }
                    DSMLModification dsmlMod =
                            new DSMLModification(dsmlModification.getName(), new DSMLValue[] {dsmlValue},
                                    dsmlModification.getOperation());
                    Modification unbundledModification = new Modification();
                    unbundledModification.setModificationMode(modification.getModificationMode());
                    unbundledModification.addOpenContentElement(dsmlMod);
                    unbundledModifyRequest.addModification(unbundledModification);
                    unbundledModifyRequests.add(unbundledModifyRequest);
                }
            }
        }

        return unbundledModifyRequests;
    }

    /**
     * Return a <code>ModifyRequest</code> for every reference capability data <code>Modification</code>.
     * 
     * @param referenceMods the <code>Modification</code>s
     * @param psoID the PSO Identifier
     * @param entityName the schema entity name
     * @return the <code>ModifyRequest</code>s
     * @throws Spml2Exception if an error occurs creating the dsml modifications
     * @throws PspException if the reference capability can not be handled
     */
    public List<ModifyRequest> unbundleReferenceModifications(List<Modification> referenceMods, PSOIdentifier psoID,
            String entityName) throws Spml2Exception, PspException {
        List<ModifyRequest> unbundledModifyRequests = new ArrayList<ModifyRequest>();

        for (Modification modification : referenceMods) {
            Map<String, List<Reference>> references = PSPUtil.getReferences(modification.getCapabilityData());
            for (String typeOfReference : references.keySet()) {
                for (Reference reference : references.get(typeOfReference)) {
                    ModifyRequest unbundledModifyRequest = new ModifyRequest();
                    unbundledModifyRequest.setRequestID(PSPUtil.uniqueRequestId());
                    unbundledModifyRequest.setPsoID(psoID);
                    if (entityName != null) {
                        unbundledModifyRequest.addOpenContentAttr(Pso.ENTITY_NAME_ATTRIBUTE, entityName);
                    }
                    CapabilityData capabilityData = PSPUtil.fromReferences(Arrays.asList(new Reference[] {reference}));
                    Modification unbundledModification = new Modification();
                    unbundledModification.addCapabilityData(capabilityData);
                    unbundledModification.setModificationMode(modification.getModificationMode());
                    unbundledModifyRequest.addModification(unbundledModification);
                    unbundledModifyRequests.add(unbundledModifyRequest);
                }
            }
        }

        return unbundledModifyRequests;
    }

    /**
     * {@inheritDoc}
     * 
     * The target of the request must be a configured target of this psp. The entity name of the request must be a
     * configured pso of this psp. The dsml attributes of the request must be configured pso attributes of this psp.
     */
    public void validate(AddRequest addRequest, AddResponse addResponse) {

        super.validate(addRequest, addResponse);

        if (addResponse.getStatus() != null && !addResponse.getStatus().equals(StatusCode.SUCCESS)) {
            return;
        }

        SpmlTarget target = targets.get(addRequest.getPsoID().getTargetID());
        if (target == null) {
            fail(addResponse, ErrorCode.INVALID_IDENTIFIER);
        }

        // Not null nor empty via super.validate();
        String entityName = addRequest.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);

        Pso psoDefinition = null;
        for (Pso psoDef : objects.get(addRequest.getPsoID().getTargetID())) {
            if (psoDef.getId().equals(entityName)) {
                psoDefinition = psoDef;
            }
        }
        if (psoDefinition == null) {
            fail(addResponse, ErrorCode.MALFORMED_REQUEST, "Invalid entity name.");
            return;
        }

        Map<String, DSMLAttr> dsmlAttrs = PSPUtil.getDSMLAttrMap(addRequest.getData());
        for (String attrName : dsmlAttrs.keySet()) {
            if (psoDefinition.getPsoAttribute(attrName) == null) {
                fail(addResponse, ErrorCode.MALFORMED_REQUEST, "Unknown attribute.");
                return;
            }
        }
    }

    /**
     * Validate that the targets and objects of the request are known to this psp.
     * 
     * @param provisioningRequest the bulk provisioning request
     * @param provisioningResponse the bulk provisioning response
     */
    public void validate(BulkProvisioningRequest provisioningRequest, ProvisioningResponse provisioningResponse) {

        try {
            getTargetAndObjectDefinitions(provisioningRequest);
        } catch (PspException e) {
            fail(provisioningResponse, ErrorCode.NO_SUCH_IDENTIFIER, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * The target of the request must be a configured target of this PSP.
     */
    public void validate(DeleteRequest deleteRequest, DeleteResponse deleteResponse) {

        super.validate(deleteRequest, deleteResponse);

        if (deleteResponse.getStatus() != null && !deleteResponse.getStatus().equals(StatusCode.SUCCESS)) {
            return;
        }

        SpmlTarget target = targets.get(deleteRequest.getPsoID().getTargetID());

        if (target == null) {
            fail(deleteResponse, ErrorCode.INVALID_IDENTIFIER);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * The target of the request must be a configured target of this PSP.
     */
    public void validate(LookupRequest lookupRequest, LookupResponse lookupResponse) {

        super.validate(lookupRequest, lookupResponse);

        if (lookupResponse.getStatus() != null && !lookupResponse.getStatus().equals(StatusCode.SUCCESS)) {
            return;
        }

        SpmlTarget target = targets.get(lookupRequest.getPsoID().getTargetID());

        if (target == null) {
            fail(lookupResponse, ErrorCode.INVALID_IDENTIFIER);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * The target of the modify request must be a configured target of this psp. The modify request must have at least
     * one modification whose mode is add, delete, or replace. If the modify request contains an alternate identifier,
     * only one alternate identifier may exist.
     */
    public void validate(ModifyRequest modifyRequest, ModifyResponse modifyResponse) {

        super.validate(modifyRequest, modifyResponse);

        if (modifyResponse.getStatus() != null && !modifyResponse.getStatus().equals(StatusCode.SUCCESS)) {
            return;
        }

        SpmlTarget target = targets.get(modifyRequest.getPsoID().getTargetID());

        if (target == null) {
            fail(modifyResponse, ErrorCode.INVALID_IDENTIFIER);
        }

        if (modifyRequest.getModifications().length == 0) {
            fail(modifyResponse, ErrorCode.MALFORMED_REQUEST, "A modification is required.");
            return;
        }

        for (Modification modification : modifyRequest.getModifications()) {
            if (modification.getModificationMode() == null) {
                fail(modifyResponse, ErrorCode.MALFORMED_REQUEST, "A modification mode is required.");
                return;
            }
            if (!(modification.getModificationMode().equals(ModificationMode.ADD)
                    || modification.getModificationMode().equals(ModificationMode.DELETE) || modification
                    .getModificationMode().equals(ModificationMode.REPLACE))) {
                fail(modifyResponse, ErrorCode.MALFORMED_REQUEST, "Unsupported modification mode.");
                return;
            }
        }

        for (Modification modification : modifyRequest.getModifications()) {
            List<AlternateIdentifier> alternateIdentifiers =
                    modification.getOpenContentElements(AlternateIdentifier.class);
            if (alternateIdentifiers.size() > 1) {
                fail(modifyResponse, ErrorCode.CUSTOM_ERROR, "Only one alternate identifier is supported.");
            }
            if (alternateIdentifiers.size() == 1) {
                AlternateIdentifier alternateIdentifier = alternateIdentifiers.get(0);
                if (alternateIdentifier.getID() == null || alternateIdentifier.getID().length() == 0) {
                    fail(modifyResponse, ErrorCode.MALFORMED_REQUEST);
                }
                if (alternateIdentifier.getTargetID() == null || alternateIdentifier.getTargetID().length() == 0) {
                    fail(modifyResponse, ErrorCode.MALFORMED_REQUEST);
                }
            }
        }

    }

    /**
     * The provisioning request must have an id and the target id and entity name must be known to this psp.
     * 
     * @param provisioningRequest the provisioning request
     * @param provisioningResponse the provisioning response
     */
    public void validate(ProvisioningRequest provisioningRequest, ProvisioningResponse provisioningResponse) {

        if (DatatypeHelper.isEmpty(provisioningRequest.getId())) {
            fail(provisioningResponse, ErrorCode.MALFORMED_REQUEST);
        }

        try {
            getTargetAndObjectDefinitions(provisioningRequest);
        } catch (PspException e) {
            fail(provisioningResponse, ErrorCode.NO_SUCH_IDENTIFIER, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * The target of the request must be a configured target of this PSP.
     */
    public void validate(SearchRequest searchRequest, SearchResponse searchResponse) {

        super.validate(searchRequest, searchResponse);
        if (searchResponse.getStatus() != null && !searchResponse.getStatus().equals(StatusCode.SUCCESS)) {
            return;
        }

        SpmlTarget target = targets.get(searchRequest.getQuery().getTargetID());
        if (target == null) {
            fail(searchResponse, ErrorCode.NO_SUCH_IDENTIFIER);
            return;
        }
    }
}

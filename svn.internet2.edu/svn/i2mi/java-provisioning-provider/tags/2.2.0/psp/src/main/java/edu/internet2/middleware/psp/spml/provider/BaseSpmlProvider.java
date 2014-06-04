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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.opensaml.xml.util.DatatypeHelper;
import org.openspml.v2.msg.Marshallable;
import org.openspml.v2.msg.XMLMarshaller;
import org.openspml.v2.msg.XMLUnmarshaller;
import org.openspml.v2.msg.spml.AddRequest;
import org.openspml.v2.msg.spml.AddResponse;
import org.openspml.v2.msg.spml.DeleteRequest;
import org.openspml.v2.msg.spml.DeleteResponse;
import org.openspml.v2.msg.spml.ErrorCode;
import org.openspml.v2.msg.spml.ExecutionMode;
import org.openspml.v2.msg.spml.LookupRequest;
import org.openspml.v2.msg.spml.LookupResponse;
import org.openspml.v2.msg.spml.ModifyRequest;
import org.openspml.v2.msg.spml.ModifyResponse;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.openspml.v2.msg.spml.Request;
import org.openspml.v2.msg.spml.Response;
import org.openspml.v2.msg.spml.StatusCode;
import org.openspml.v2.msg.spmlsearch.Query;
import org.openspml.v2.msg.spmlsearch.SearchRequest;
import org.openspml.v2.msg.spmlsearch.SearchResponse;
import org.openspml.v2.util.Spml2Exception;
import org.openspml.v2.util.xml.ReflectiveDOMXMLUnmarshaller;
import org.openspml.v2.util.xml.ReflectiveXMLMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.psp.spml.config.Pso;
import edu.internet2.middleware.psp.spml.request.SearchRequestWithQueryClauseNamespaces;
import edu.internet2.middleware.psp.util.MDCHelper;
import edu.internet2.middleware.psp.util.PSPUtil;
import edu.internet2.middleware.shibboleth.common.config.BaseReloadableService;

/**
 * Base class for SPMLv2 Provisioning Service Providers. Handling of requests is provided by subclasses. Extends
 * Shibboleth's {@link BaseReloadableService}.
 */
public abstract class BaseSpmlProvider extends BaseReloadableService implements SpmlProvider {

  /** Logger. */
  private static final Logger LOG = LoggerFactory.getLogger(BaseSpmlProvider.class);

  /** The Spring identifier. */
  private String id;

  /** Method name to look for in subclasses. */
  public static final String methodName = "execute";

  /** SPML toolkit XML marshaller. */
  private XMLMarshaller xmlMarshaller;

  /** SPML toolkit XML unmarshaller. */
  private XMLUnmarshaller xmlUnmarshaller;

  /** Whether or not to log spml messages. */
  private boolean logSpml;

  /** Path to output file. */
  private String pathToOutputFile;

  /** Where output is written to. */
  private BufferedWriter writer;

  /** If true will write requests. False by default. */
  private boolean writeRequests;

  /** If true will write responses. False by default. */
  private boolean writeResponses;

  /**
   * {@inheritDoc}
   * 
   * Only the synchronous execution mode is supported.
   */
  public Response execute(Request request) {

    // a generic Response returned under error conditions
    Response response = new Response();
    response.setRequestID(this.getOrGenerateRequestID(request));

    try {
      // FUTURE handle asynchronous requests
      if (request.getExecutionMode() == ExecutionMode.ASYNCHRONOUS) {
        fail(response, ErrorCode.UNSUPPORTED_EXECUTION_MODE);
        LOG.error(PSPUtil.toString(response));
        LOG.trace("response:\n{}", this.toXML(response));
      } else {
        // determine the appropriate method
        // TODO a terrible kludge to work around the OpenSPML2 toolkit bug
        Method method = null;
        if (request instanceof SearchRequestWithQueryClauseNamespaces) {
          method = this.getClass().getMethod(methodName, new Class[] { request.getClass().getSuperclass() });
        } else {
          method = this.getClass().getMethod(methodName, new Class[] { request.getClass() });
        }
        // execute the request
        response = (Response) method.invoke(this, new Object[] { request });
        if (response.getRequestID() == null) {
          response.setRequestID(this.getOrGenerateRequestID(request));
        }
      }

    } catch (NoSuchMethodException e) {
      fail(response, ErrorCode.UNSUPPORTED_OPERATION);
      LOG.error(PSPUtil.toString(response), e);
      LOG.trace("response:\n{}", this.toXML(response));
    } catch (IllegalAccessException e) {
      fail(response, ErrorCode.UNSUPPORTED_OPERATION, e);
      LOG.error(PSPUtil.toString(response), e);
      LOG.trace("response:\n{}", this.toXML(response));
    } catch (InvocationTargetException e) {
      fail(response, ErrorCode.UNSUPPORTED_OPERATION, e);
      LOG.error(PSPUtil.toString(response), e);
      LOG.trace("response:\n{}", this.toXML(response));
    }

    return response;
  }

  /** {@inheritDoc} */
  public AddResponse execute(AddRequest addRequest) {

    // Start MDC logging.
    MDCHelper mdc = new MDCHelper(addRequest).start();

    // Log the request.
    LOG.info("Target '{}' - Add {}", getId(), PSPUtil.toString(addRequest));

    // Log the request as SPML.
    if (isLogSpml()) {
      LOG.info("Target '{}' - Add XML:\n{}", getId(), toXML(addRequest));
    }

    // Potentially write the request.
    writeRequest(addRequest);

    // Create a new response.
    AddResponse addResponse = new AddResponse();

    // Be optimistic regarding success.
    addResponse.setStatus(StatusCode.SUCCESS);

    // The response requestID should be the same as the request.
    addResponse.setRequestID(getOrGenerateRequestID(addRequest));

    // Validate the request.
    validate(addRequest, addResponse);

    // If the validation was successful, execute the request.
    if (addResponse.getStatus().equals(StatusCode.SUCCESS)) {
      execute(addRequest, addResponse);
    }

    // If the response is a success, log to INFO.
    if (addResponse.getStatus().equals(StatusCode.SUCCESS)) {
      LOG.info("Target '{}' - Add {}", getId(), PSPUtil.toString(addResponse));
      if (isLogSpml()) {
        LOG.info("Target '{}' - Add XML:\n{}", getId(), toXML(addResponse));
      }
      // If the response is not a success, log to ERROR.
    } else {
      LOG.error("Target '{}' - Add {}", getId(), PSPUtil.toString(addResponse));
      if (isLogSpml()) {
        LOG.error("Target '{}' - Add XML:\n{}", getId(), toXML(addResponse));
      }
    }

    // Potentially write the response.
    writeResponse(addResponse);

    // Stop MDC logging.
    mdc.stop();

    // Return the response.
    return addResponse;
  }

  /**
   * Execute an {@link AddRequest} and update the {@link AddResponse}.
   * 
   * @param addRequest the SPML add request
   * @param addResponse the SPML add response
   */
  public abstract void execute(AddRequest addRequest, AddResponse addResponse);

  /** {@inheritDoc} */
  public DeleteResponse execute(DeleteRequest deleteRequest) {

    // Start MDC logging.
    MDCHelper mdc = new MDCHelper(deleteRequest).start();

    // Log the request.
    LOG.info("Target '{}' - Delete {}", getId(), PSPUtil.toString(deleteRequest));

    // Log the request as SPML.
    if (isLogSpml()) {
      LOG.info("Target '{}' - Delete XML:\n{}", getId(), toXML(deleteRequest));
    }

    // Potentially write the request.
    writeRequest(deleteRequest);

    // Create a new response.
    DeleteResponse deleteResponse = new DeleteResponse();

    // Be optimistic regarding success.
    deleteResponse.setStatus(StatusCode.SUCCESS);

    // The response requestID should be the same as the request.
    deleteResponse.setRequestID(getOrGenerateRequestID(deleteRequest));

    // Validate the request.
    validate(deleteRequest, deleteResponse);

    // If the validation was successful, execute the request.
    if (deleteResponse.getStatus().equals(StatusCode.SUCCESS)) {
      execute(deleteRequest, deleteResponse);
    }

    // If the response is a success, log to INFO.
    if (deleteResponse.getStatus().equals(StatusCode.SUCCESS)) {
      LOG.info("Target '{}' - Delete {}", getId(), PSPUtil.toString(deleteResponse));
      if (isLogSpml()) {
        LOG.info("Target '{}' - Delete XML:\n{}", getId(), toXML(deleteResponse));
      }
      // If the response is not a success, log to ERROR.
    } else {
      LOG.error("Target '{}' - Delete {}", getId(), PSPUtil.toString(deleteResponse));
      if (isLogSpml()) {
        LOG.error("Target '{}' - Delete XML:\n{}", getId(), toXML(deleteResponse));
      }
    }

    // Potentially write the response.
    writeResponse(deleteResponse);

    // Stop MDC logging.
    mdc.stop();

    // Return the response.
    return deleteResponse;
  }

  /**
   * Execute an {@link DeleteRequest} and update the {@link DeleteResponse}.
   * 
   * @param deleteRequest the SPML delete request
   * @param deleteResponse the SPML delete response
   */
  public abstract void execute(DeleteRequest deleteRequest, DeleteResponse deleteResponse);

  /** {@inheritDoc} */
  public LookupResponse execute(LookupRequest lookupRequest) {

    // Start MDC logging.
    MDCHelper mdc = new MDCHelper(lookupRequest).start();

    // Log the request.
    LOG.info("Target '{}' - Lookup {}", getId(), PSPUtil.toString(lookupRequest));

    // Log the request as SPML.
    if (isLogSpml()) {
      LOG.info("Target '{}' - Lookup XML:\n{}", getId(), toXML(lookupRequest));
    }

    // Potentially write the request.
    writeRequest(lookupRequest);

    // Create a new response.
    LookupResponse lookupResponse = new LookupResponse();

    // Be optimistic regarding success.
    lookupResponse.setStatus(StatusCode.SUCCESS);

    // The response requestID should be the same as the request.
    lookupResponse.setRequestID(getOrGenerateRequestID(lookupRequest));

    // Validate the request.
    validate(lookupRequest, lookupResponse);

    // If the validation was successful, execute the request.
    if (lookupResponse.getStatus().equals(StatusCode.SUCCESS)) {
      execute(lookupRequest, lookupResponse);
    }

    // If the response is a success, log to INFO.
    if (lookupResponse.getStatus().equals(StatusCode.SUCCESS)) {
      LOG.info("Target '{}' - Lookup {}", getId(), PSPUtil.toString(lookupResponse));
      if (isLogSpml()) {
        LOG.info("Target '{}' - Lookup XML:\n{}", getId(), toXML(lookupResponse));
      }
      // If the response is not a success, log to ERROR.
    } else {
      LOG.error("Target '{}' - Lookup {}", getId(), PSPUtil.toString(lookupResponse));
      if (isLogSpml()) {
        LOG.error("Target '{}' - Lookup XML:\n{}", getId(), toXML(lookupResponse));
      }
    }

    // Potentially write the response.
    writeResponse(lookupResponse);

    // Stop MDC logging.
    mdc.stop();

    // Return the response.
    return lookupResponse;
  }

  /**
   * Execute an {@link LookupRequest} and update the {@link LookupResponse}.
   * 
   * @param lookupRequest the SPML delete request
   * @param lookupResponse the SPML delete response
   */
  public abstract void execute(LookupRequest lookupRequest, LookupResponse lookupResponse);

  /** {@inheritDoc} */
  public ModifyResponse execute(ModifyRequest modifyRequest) {

    // Start MDC logging.
    MDCHelper mdc = new MDCHelper(modifyRequest).start();

    // Log the request.
    LOG.info("Target '{}' - Modify {}", getId(), PSPUtil.toString(modifyRequest));

    // Log the request as SPML.
    if (isLogSpml()) {
      LOG.info("Target '{}' - Modify XML:\n{}", getId(), toXML(modifyRequest));
    }

    // Potentially write the request.
    writeRequest(modifyRequest);

    // Create a new response.
    ModifyResponse modifyResponse = new ModifyResponse();

    // Be optimistic regarding success.
    modifyResponse.setStatus(StatusCode.SUCCESS);

    // The response requestID should be the same as the request.
    modifyResponse.setRequestID(getOrGenerateRequestID(modifyRequest));

    // Validate the request.
    validate(modifyRequest, modifyResponse);

    // If the validation was successful, execute the request.
    if (modifyResponse.getStatus().equals(StatusCode.SUCCESS)) {
      execute(modifyRequest, modifyResponse);
    }

    // If the response is a success, log to INFO.
    if (modifyResponse.getStatus().equals(StatusCode.SUCCESS)) {
      LOG.info("Target '{}' - Modify {}", getId(), PSPUtil.toString(modifyResponse));
      if (isLogSpml()) {
        LOG.info("Target '{}' - Modify XML:\n{}", getId(), toXML(modifyResponse));
      }
      // If the response is not a success, log to ERROR.
    } else {
      LOG.error("Target '{}' - Modify {}", getId(), PSPUtil.toString(modifyResponse));
      if (isLogSpml()) {
        LOG.error("Target '{}' - Modify XML:\n{}", getId(), toXML(modifyResponse));
      }
    }

    // Potentially write the response.
    writeResponse(modifyResponse);

    // Stop MDC logging.
    mdc.stop();

    // Return the response.
    return modifyResponse;
  }

  /**
   * Execute an {@link ModifyRequest} and update the {@link ModifyResponse}.
   * 
   * @param modifyRequest the SPML modify request
   * @param modifyResponse the SPML modify response
   */
  public abstract void execute(ModifyRequest modifyRequest, ModifyResponse modifyResponse);

  /** {@inheritDoc} */
  public SearchResponse execute(SearchRequest searchRequest) {

    // Start MDC logging.
    MDCHelper mdc = new MDCHelper(searchRequest).start();

    // Log the request.
    LOG.info("Target '{}' - Search {}", getId(), PSPUtil.toString(searchRequest));

    // Log the request as SPML.
    if (isLogSpml()) {
      LOG.info("Target '{}' - Search XML:\n{}", getId(), toXML(searchRequest));
    }

    // Potentially write the request.
    writeRequest(searchRequest);

    // Create a new response.
    SearchResponse searchResponse = new SearchResponse();

    // Be optimistic regarding success.
    searchResponse.setStatus(StatusCode.SUCCESS);

    // The response requestID should be the same as the request.
    searchResponse.setRequestID(getOrGenerateRequestID(searchRequest));

    // Validate the request.
    validate(searchRequest, searchResponse);

    // If the validation was successful, execute the request.
    if (searchResponse.getStatus().equals(StatusCode.SUCCESS)) {
      execute(searchRequest, searchResponse);
    }

    // If the response is a success, log to INFO.
    if (searchResponse.getStatus().equals(StatusCode.SUCCESS)) {
      LOG.info("Target '{}' - Search {}", getId(), PSPUtil.toString(searchResponse));
      if (isLogSpml()) {
        LOG.info("Target '{}' - Search XML:\n{}", getId(), toXML(searchResponse));
      }
      // If the response is not a success, log to ERROR.
    } else {
      LOG.error("Target '{}' - Search {}", getId(), PSPUtil.toString(searchResponse));
      if (isLogSpml()) {
        LOG.error("Target '{}' - Search XML:\n{}", getId(), toXML(searchResponse));
      }
    }

    // Potentially write the response.
    writeResponse(searchResponse);

    // Stop MDC logging.
    mdc.stop();

    // Return the response.
    return searchResponse;
  }

  /**
   * Execute an {@link SearchRequest} and update the {@link SearchResponse}.
   * 
   * @param searchRequest the SPML search request
   * @param searchResponse the SPML search response
   */
  public abstract void execute(SearchRequest searchRequest, SearchResponse searchResponse);

  /**
   * Determine if the {@link AddRequest} satisfies minimum requirements.
   * 
   * The request must have a PSO identifier containing an ID and target ID.
   * 
   * The request must have data.
   * 
   * The request must have an {@link Pso.ENTITY_NAME_ATTRIBUTE} attribute.
   * 
   * TODO
   * 
   * @param addRequest the SPML add request
   * @param addResponse the SPML add response
   */
  public void validate(AddRequest addRequest, AddResponse addResponse) {

    if (addRequest.getPsoID() == null) {
      fail(addResponse, ErrorCode.INVALID_IDENTIFIER);
      return;
    }

    if (DatatypeHelper.isEmpty(addRequest.getPsoID().getID())) {
      fail(addResponse, ErrorCode.INVALID_IDENTIFIER);
      return;
    }

    if (DatatypeHelper.isEmpty(addRequest.getPsoID().getTargetID())) {
      fail(addResponse, ErrorCode.INVALID_IDENTIFIER);
      return;
    }

    if (addRequest.getData() == null) {
      fail(addResponse, ErrorCode.MALFORMED_REQUEST, "Data is required.");
      return;
    }

    String entityName = addRequest.findOpenContentAttrValueByName(Pso.ENTITY_NAME_ATTRIBUTE);
    if (DatatypeHelper.isEmpty(entityName)) {
      fail(addResponse, ErrorCode.MALFORMED_REQUEST, "Invalid entity name.");
      return;
    }
  }

  /**
   * Determine if the {@link DeleteRequest} satisfies minimum requirements.
   * 
   * The request must have a valid PSO identifier.
   * 
   * @param deleteRequest the SPML delete request
   * @param deleteResponse the SPML delete response
   */
  public void validate(DeleteRequest deleteRequest, DeleteResponse deleteResponse) {

    if (!isValid(deleteRequest.getPsoID())) {
      fail(deleteResponse, ErrorCode.INVALID_IDENTIFIER);
    }
  }

  /**
   * Determine if the {@link LookupRequest} satisfies minimum requirements.
   * 
   * The request must have a PSO identifier containing an ID and target ID.
   * 
   * @param lookupRequest the SPML lookup request
   * @param lookupResponse the SPML lookup response
   */
  public void validate(LookupRequest lookupRequest, LookupResponse lookupResponse) {

    if (lookupRequest.getPsoID() == null) {
      fail(lookupResponse, ErrorCode.MALFORMED_REQUEST);
      return;
    }

    if (DatatypeHelper.isEmpty(lookupRequest.getPsoID().getID())) {
      fail(lookupResponse, ErrorCode.MALFORMED_REQUEST);
      return;
    }

    if (DatatypeHelper.isEmpty(lookupRequest.getPsoID().getTargetID())) {
      fail(lookupResponse, ErrorCode.MALFORMED_REQUEST);
      return;
    }
  }

  /**
   * Determine if the {@link LookupRequest} satisfies minimum requirements.
   * 
   * The request must have a valid PSO identifier.
   * 
   * @param modifyRequest the SPML modify request
   * @param modifyResponse the SPML modify response
   */
  public void validate(ModifyRequest modifyRequest, ModifyResponse modifyResponse) {

    if (!isValid(modifyRequest.getPsoID())) {
      fail(modifyResponse, ErrorCode.INVALID_IDENTIFIER);
    }
  }

  /**
   * Determine if the {@link SearchRequest} satisfies minimum requirements.
   * 
   * The request must have a {@link Query} and a target ID.
   * 
   * @param searchRequest the SPML search request
   * @param searchResponse the SPML search response
   */
  public void validate(SearchRequest searchRequest, SearchResponse searchResponse) {

    Query query = searchRequest.getQuery();
    if (query == null) {
      fail(searchResponse, ErrorCode.MALFORMED_REQUEST);
      return;
    }

    if (DatatypeHelper.isEmpty(query.getTargetID())) {
      fail(searchResponse, ErrorCode.MALFORMED_REQUEST);
    }
  }

  /**
   * Return true if the PSO identifier is valid, return false otherwise.
   * 
   * A valid PSO identifier has an ID and a target ID.
   * 
   * @param psoIdentifier the SPML PSO identifier
   * @return true if the identifier is valid, false otherwise.
   */
  public boolean isValid(PSOIdentifier psoIdentifier) {

    if (psoIdentifier == null) {
      return false;
    }

    if (DatatypeHelper.isEmpty(psoIdentifier.getID())) {
      return false;
    }

    if (DatatypeHelper.isEmpty(psoIdentifier.getTargetID())) {
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id.
   * 
   * @param id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Return the XML marshaller, which will be instantiated once and reused per instance of this class.
   * 
   * @return the {@link XMLMarshaller}
   */
  public XMLMarshaller getXMLMarshaller() {
    if (this.xmlMarshaller == null) {
      this.xmlMarshaller = new ReflectiveXMLMarshaller();
    }
    return this.xmlMarshaller;
  }

  /**
   * Set the XML marshaller
   * 
   * @param xmlMarshaller the {@link XMLMarshaller}
   */
  public void setXMLMarshaller(XMLMarshaller xmlMarshaller) {
    this.xmlMarshaller = xmlMarshaller;
  }

  /**
   * Return the XML unmarshaller, which will be instantiated once and reused per instance of this class.
   * 
   * @return the {@link XMLUnmarshaller}
   */
  public XMLUnmarshaller getXmlUnmarshaller() {
    if (this.xmlUnmarshaller == null) {
      this.xmlUnmarshaller = new ReflectiveDOMXMLUnmarshaller();
    }
    return xmlUnmarshaller;
  }

  /**
   * Set the XML unmarshaller
   * 
   * @param xmlUnmarshaller the {@link XMLUnmarshaller}
   */
  public void setXmlUnmarshaller(XMLUnmarshaller xmlUnmarshaller) {
    this.xmlUnmarshaller = xmlUnmarshaller;
  }

  /**
   * @return Returns the pathToOutputFile.
   */
  public String getPathToOutputFile() {
    return pathToOutputFile;
  }

  /**
   * @param pathToOutputFile The pathToOutputFile to set.
   */
  public void setPathToOutputFile(String pathToOutputFile) {
    this.pathToOutputFile = pathToOutputFile;
  }

  /**
   * @return Returns the writeRequests.
   */
  public boolean isWriteRequests() {
    return writeRequests;
  }

  /**
   * @param writeRequests The writeRequests to set.
   */
  public void setWriteRequests(boolean writeRequests) {
    this.writeRequests = writeRequests;
  }

  /**
   * @return Returns the writeResponses.
   */
  public boolean isWriteResponses() {
    return writeResponses;
  }

  /**
   * @param writeResponses The writeResponses to set.
   */
  public void setWriteResponses(boolean writeResponses) {
    this.writeResponses = writeResponses;
  }

  /**
   * @return Returns the writer.
   */
  public BufferedWriter getWriter() {
    return writer;
  }

  /**
   * @param writer The writer to set.
   */
  public void setWriter(BufferedWriter writer) {
    this.writer = writer;
  }

  /**
   * See {@link #fail(Response, ErrorCode, String...)}
   * 
   * The messages from the given exception are added to the response.
   * 
   * @param response the {@link Response}
   * @param errorCode the {@link ErrorCode}
   * @param e the exception
   * @return the updated {@link Response}
   */
  public Response fail(Response response, ErrorCode errorCode, Exception e) {
    return fail(response, errorCode, e.getMessage());
  }

  /**
   * Set the status code of the given response to failure and set the error code and messages.
   * 
   * Kludge : the 0x0 unicode character is replaced with an underscore to avoid exceptions when handling Active
   * Directory error messages.
   * 
   * @param response the {@link Response}
   * @param errorCode the {@link ErrorCode}
   * @param messages error text
   * @return the updated {@link Response}
   */
  public Response fail(Response response, ErrorCode errorCode, String... messages) {
    response.setStatus(StatusCode.FAILURE);
    response.setError(errorCode);
    if (messages != null) {
      for (String message : messages) {
        if (message != null) {
          // TODO for Active Directory, find a better way
          message = message.replace((char) 0x0, '_');
          response.addErrorMessage(message);
        }
      }
    }
    return response;
  }

  /**
   * Return the request ID of the given request or generate a new request ID.
   * 
   * @param request
   * @return the request ID
   */
  public String getOrGenerateRequestID(Request request) {
    if (request.getRequestID() != null) {
      return request.getRequestID();
    }
    return PSPUtil.uniqueRequestId();
  }

  /**
   * Return the XML representation of the given object. Return null if an exception occurs.
   * 
   * @param marshallable the {@link Marshallable} object
   * @return the XML representation
   */
  public String toXML(Marshallable marshallable) {
    try {
      return marshallable.toXML(this.getXMLMarshaller());
    } catch (Spml2Exception e) {
      LOG.error("Unable to marshal xml", e);
      return null;
    }
  }

  /**
   * Write an spml request or response to the configured output file or STDOUT.
   * 
   * @param marshallable the spml request or response
   * @throws IOException if the output file cannot be written to
   */
  public void write(Marshallable marshallable) throws IOException {
    if (writer == null) {
      if (DatatypeHelper.isEmpty(pathToOutputFile)) {
        writer = new BufferedWriter(new OutputStreamWriter(System.out));
      } else {
        writer = new BufferedWriter(new FileWriter(pathToOutputFile));
      }
    }

    writer.write(toXML(marshallable));
    writer.write(System.getProperty("line.separator"));
    writer.flush();
  }

  /**
   * If configured to write requests, write SPML requests to the configured file or STDOUT.
   * 
   * @param request the SPML request to write
   */
  public void writeRequest(Request request) {
    if (isWriteRequests()) {
      try {
        write(request);
      } catch (IOException e) {
        LOG.error("Unable to write request : " + e.getMessage(), e);
      }
    }
  }

  /**
   * If configured to write responses, write SPML responses to the configured file or STDOUT.
   * 
   * @param response the SPML response to write
   */
  public void writeResponse(Response response) {
    if (isWriteResponses()) {
      try {
        write(response);
      } catch (IOException e) {
        LOG.error("Unable to write request : " + e.getMessage(), e);
      }
    }
  }

  /**
   * If true will log spml messages.
   * 
   * @return Returns whether or not to log spml messages.
   */
  public boolean isLogSpml() {
    return logSpml;
  }

  /**
   * If set to true will log spml messages.
   * 
   * @param logSpml whether or not to log spml messages.
   */
  public void setLogSpml(boolean logSpml) {
    this.logSpml = logSpml;
  }
}

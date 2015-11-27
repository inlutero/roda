/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.api.controllers;

import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.TransformerException;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.roda.core.common.UserUtility;
import org.roda.core.data.adapter.facet.Facets;
import org.roda.core.data.adapter.filter.Filter;
import org.roda.core.data.adapter.sort.Sorter;
import org.roda.core.data.adapter.sublist.Sublist;
import org.roda.core.data.common.AuthorizationDeniedException;
import org.roda.core.data.common.NotFoundException;
import org.roda.core.data.common.RODAException;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.IndexResult;
import org.roda.core.data.v2.RodaUser;
import org.roda.core.data.v2.SimpleDescriptionObject;
import org.roda.core.data.v2.TransferredResource;
import org.roda.core.model.AIP;
import org.roda.core.model.DescriptiveMetadata;
import org.roda.core.model.ValidationException;
import org.roda.core.storage.Binary;
import org.roda.wui.api.exceptions.AlreadyExistsException;
import org.roda.wui.api.exceptions.ApiException;
import org.roda.wui.api.exceptions.RequestNotValidException;
import org.roda.wui.api.v1.utils.StreamResponse;
import org.roda.wui.client.browse.BrowseItemBundle;
import org.roda.wui.client.browse.DescriptiveMetadataEditBundle;
import org.roda.wui.common.RodaCoreService;
import org.roda.wui.common.client.GenericException;

/**
 * FIXME 1) verify all checkObject*Permissions (because now also a permission
 * for insert is available)
 */
public class Browser extends RodaCoreService {

  private static final String SDO_PARAM = "sdo";
  private static final String FILTER_PARAM = "filter";
  private static final String SORTER_PARAM = "sorter";
  private static final String SUBLIST_PARAM = "sublist";

  private static final String BROWSER_COMPONENT = "Browser";
  private static final String ADMINISTRATION_METADATA_EDITOR_ROLE = "administration.metadata_editor";
  private static final String INGEST_TRANSFER_ROLE = "ingest.transfer";
  private static final String BROWSE_ROLE = "browse";

  private static final String TRANSFERRED_RESOURCE_ID_PARAM = "transferredResourceId";

  private static final String PARENT_PARAM = "parent";
  private static final String FOLDERNAME_PARAM = "folderName";
  private static final String PARENT_PATH = "path";
  private static final String FILENAME_PARAM = "filename";
  private static final String PATH_PARAM = "path";
  private static final Object CLASSIFICATION_PLAN_TYPE_PARAMETER = "classificationPlanType";

  private Browser() {
    super();
  }

  public static BrowseItemBundle getItemBundle(RodaUser user, String aipId, Locale locale)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date startDate = new Date();

    // check user permissions
    UserUtility.checkRoles(user, BROWSE_ROLE);

    // delegate
    BrowseItemBundle itemBundle = BrowserHelper.getItemBundle(aipId, locale);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "getItemBundle", aipId, duration, RodaConstants.API_PATH_PARAM_AIP_ID,
      aipId);

    return itemBundle;
  }

  public static DescriptiveMetadataEditBundle getDescriptiveMetadataEditBundle(RodaUser user, String aipId,
    String metadataId) throws AuthorizationDeniedException, GenericException {
    Date startDate = new Date();

    // check user permissions
    UserUtility.checkRoles(user, BROWSE_ROLE);

    // delegate
    DescriptiveMetadataEditBundle bundle = BrowserHelper.getDescriptiveMetadataEditBundle(aipId, metadataId);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "getDescriptiveMetadataEditBundle", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_METADATA_ID, metadataId);

    return bundle;
  }

  public static IndexResult<SimpleDescriptionObject> findDescriptiveMetadata(RodaUser user, Filter filter,
    Sorter sorter, Sublist sublist, Facets facets) throws RODAException {
    Date startDate = new Date();

    // check user permissions
    UserUtility.checkRoles(user, BROWSE_ROLE);

    // delegate
    IndexResult<SimpleDescriptionObject> descriptiveMetadata = BrowserHelper.findDescriptiveMetadata(filter, sorter,
      sublist, facets);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "findDescriptiveMetadata", null, duration, FILTER_PARAM, filter,
      SORTER_PARAM, sorter, SUBLIST_PARAM, sublist);

    return descriptiveMetadata;
  }

  public static Long countDescriptiveMetadata(RodaUser user, Filter filter)
    throws AuthorizationDeniedException, GenericException {
    Date startDate = new Date();

    // check user permissions
    UserUtility.checkRoles(user, BROWSE_ROLE);

    // delegate
    Long count = BrowserHelper.countDescriptiveMetadata(filter);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "countDescriptiveMetadata", null, duration, FILTER_PARAM,
      filter.toString());

    return count;
  }

  public static SimpleDescriptionObject getSimpleDescriptionObject(RodaUser user, String aipId)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date startDate = new Date();

    // check user permissions
    UserUtility.checkRoles(user, BROWSE_ROLE);

    // delegate
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "getSimpleDescriptionObject", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId);

    return sdo;
  }

  public static List<SimpleDescriptionObject> getAncestors(RodaUser user, SimpleDescriptionObject sdo)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date startDate = new Date();

    // check user permissions
    UserUtility.checkRoles(user, BROWSE_ROLE);

    // delegate
    List<SimpleDescriptionObject> ancestors = BrowserHelper.getAncestors(sdo);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "getAncestors", sdo.getId(), duration, SDO_PARAM, sdo.toString());

    return ancestors;
  }

  /*
   * ---------------------------------------------------------------------------
   * ---------------- REST related methods - start -----------------------------
   * ---------------------------------------------------------------------------
   */
  public static StreamResponse getAipRepresentation(RodaUser user, String aipId, String representationId,
    String acceptFormat)
      throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
    Date startDate = new Date();

    // validate input
    BrowserHelper.validateGetAipRepresentationParams(acceptFormat);

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectReadPermissions(user, sdo);

    // delegate
    StreamResponse aipRepresentation = BrowserHelper.getAipRepresentation(aipId, representationId, acceptFormat);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "getAipRepresentation", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_REPRESENTATION_ID, representationId);

    return aipRepresentation;
  }

  public static StreamResponse listAipDescriptiveMetadata(RodaUser user, String aipId, String start, String limit,
    String acceptFormat)
      throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
    Date startDate = new Date();

    // validate input
    BrowserHelper.validateListAipDescriptiveMetadataParams(acceptFormat);

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectReadPermissions(user, sdo);

    // delegate
    StreamResponse aipDescriptiveMetadataList = BrowserHelper.listAipDescriptiveMetadata(aipId, start, limit);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "listAipDescriptiveMetadata", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_QUERY_KEY_START, start,
      RodaConstants.API_QUERY_KEY_LIMIT, limit);

    return aipDescriptiveMetadataList;
  }

  public static StreamResponse getAipDescritiveMetadata(RodaUser user, String aipId, String metadataId,
    String acceptFormat, String language) throws AuthorizationDeniedException, GenericException, TransformerException,
      NotFoundException, RequestNotValidException {
    Date startDate = new Date();

    // validate input
    BrowserHelper.validateGetAipDescritiveMetadataParams(acceptFormat);

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectModifyPermissions(user, sdo);

    // delegate
    StreamResponse aipDescritiveMetadata = BrowserHelper.getAipDescritiveMetadata(aipId, metadataId, acceptFormat,
      language);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "getAipDescritiveMetadata", aipId, duration,
      RodaConstants.API_PATH_PARAM_METADATA_ID, metadataId);

    return aipDescritiveMetadata;

  }

  public static StreamResponse listAipPreservationMetadata(RodaUser user, String aipId, String start, String limit,
    String acceptFormat)
      throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
    Date startDate = new Date();

    // validate input
    BrowserHelper.validateListAipPreservationMetadataParams(acceptFormat);

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectReadPermissions(user, sdo);

    // delegate
    StreamResponse aipPreservationMetadataList = BrowserHelper.aipsAipIdPreservationMetadataGet(aipId, start, limit);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "listAipPreservationMetadata", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_QUERY_KEY_START, start,
      RodaConstants.API_QUERY_KEY_LIMIT, limit);

    return aipPreservationMetadataList;
  }

  public static StreamResponse getAipRepresentationPreservationMetadata(RodaUser user, String aipId,
    String representationId, String startAgent, String limitAgent, String startEvent, String limitEvent,
    String startFile, String limitFile, String acceptFormat, String language) throws AuthorizationDeniedException,
      GenericException, TransformerException, NotFoundException, RequestNotValidException {
    Date startDate = new Date();

    // validate input
    BrowserHelper.validateGetAipRepresentationPreservationMetadataParams(acceptFormat, language);

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectReadPermissions(user, sdo);

    // delegate
    StreamResponse aipRepresentationPreservationMetadata = BrowserHelper.getAipRepresentationPreservationMetadata(aipId,
      representationId, startAgent, limitAgent, startEvent, limitEvent, startFile, limitFile, acceptFormat, language);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "getAipRepresentationPreservationMetadata", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, "startAgent", startAgent, "limitAgent", limitAgent, "startEvent",
      startEvent, "limitEvent", limitEvent, "startFile", startFile, "limitFile", limitFile);

    return aipRepresentationPreservationMetadata;

  }

  public static StreamResponse getAipRepresentationPreservationMetadataFile(RodaUser user, String aipId,
    String representationId, String fileId) throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date startDate = new Date();

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectReadPermissions(user, sdo);

    // delegate
    StreamResponse aipRepresentationPreservationMetadataFile = BrowserHelper
      .getAipRepresentationPreservationMetadataFile(aipId, representationId, fileId);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "getAipRepresentationPreservationMetadataFile", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_REPRESENTATION_ID, representationId,
      RodaConstants.API_PATH_PARAM_FILE_ID, fileId);

    return aipRepresentationPreservationMetadataFile;
  }

  public static void postAipRepresentationPreservationMetadataFile(RodaUser user, String aipId, String representationId,
    InputStream is, FormDataContentDisposition fileDetail)
      throws AuthorizationDeniedException, GenericException, NotFoundException {

    Date startDate = new Date();

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectInsertPermissions(user, sdo);

    // delegate
    BrowserHelper.createOrUpdateAipRepresentationPreservationMetadataFile(aipId, representationId, is, fileDetail,
      true);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "postAipRepresentationPreservationMetadataFile", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_REPRESENTATION_ID, representationId);

  }

  public static void putAipRepresentationPreservationMetadataFile(RodaUser user, String aipId, String representationId,
    InputStream is, FormDataContentDisposition fileDetail)
      throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date startDate = new Date();

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectInsertPermissions(user, sdo);

    // delegate
    BrowserHelper.createOrUpdateAipRepresentationPreservationMetadataFile(aipId, representationId, is, fileDetail,
      false);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "aipsAipIdPreservationMetadataRepresentationIdFileIdPut", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_REPRESENTATION_ID, representationId);

  }

  public static void aipsAipIdPreservationMetadataRepresentationIdFileIdDelete(RodaUser user, String aipId,
    String representationId, String fileId) throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date startDate = new Date();

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectRemovePermissions(user, sdo);

    // delegate
    BrowserHelper.aipsAipIdPreservationMetadataRepresentationIdFileIdDelete(aipId, representationId, fileId);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "aipsAipIdPreservationMetadataRepresentationIdFileIdDelete", aipId,
      duration, RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_REPRESENTATION_ID,
      representationId, RodaConstants.API_PATH_PARAM_FILE_ID, fileId);

  }

  /*
   * ---------------------------------------------------------------------------
   * ---------------- REST related methods - end -------------------------------
   * ---------------------------------------------------------------------------
   */

  public static SimpleDescriptionObject moveInHierarchy(RodaUser user, String aipId, String parentId)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date startDate = new Date();

    // check user permissions
    UserUtility.checkRoles(user, ADMINISTRATION_METADATA_EDITOR_ROLE);
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectModifyPermissions(user, sdo);
    sdo = BrowserHelper.getSimpleDescriptionObject(parentId);
    UserUtility.checkObjectModifyPermissions(user, sdo);

    // delegate
    sdo = BrowserHelper.moveInHierarchy(aipId, parentId);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "moveInHierarchy", sdo.getId(), duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, "toParent", parentId);

    return sdo;

  }

  public static AIP createAIP(RodaUser user, String parentId)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, ADMINISTRATION_METADATA_EDITOR_ROLE);
    if (parentId != null) {
      SimpleDescriptionObject parentSDO = BrowserHelper.getSimpleDescriptionObject(parentId);
      UserUtility.checkObjectModifyPermissions(user, parentSDO);
    } else {
      // TODO check user role to create top-level AIPs
    }

    // delegate
    AIP aip = BrowserHelper.createAIP(parentId);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, BROWSER_COMPONENT, "createAIP", aip.getId(), duration, "parentId", parentId);

    return aip;
  }

  public static void removeAIP(RodaUser user, String aipId)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, ADMINISTRATION_METADATA_EDITOR_ROLE);
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectModifyPermissions(user, sdo);

    // delegate
    BrowserHelper.removeAIP(aipId);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, BROWSER_COMPONENT, "removeAIP", aipId, duration);
  }

  public static DescriptiveMetadata createDescriptiveMetadataFile(RodaUser user, String aipId, String metadataId,
    String metadataType, Binary metadataBinary)
      throws AuthorizationDeniedException, GenericException, ValidationException, NotFoundException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, ADMINISTRATION_METADATA_EDITOR_ROLE);
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectModifyPermissions(user, sdo);

    // delegate
    DescriptiveMetadata ret = BrowserHelper.createDescriptiveMetadataFile(aipId, metadataId, metadataType,
      metadataBinary);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, BROWSER_COMPONENT, "createDescriptiveMetadataFile", sdo.getId(), duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_METADATA_ID, metadataId);

    return ret;
  }

  public static DescriptiveMetadata updateDescriptiveMetadataFile(RodaUser user, String aipId, String metadataId,
    String metadataType, Binary metadataBinary)
      throws AuthorizationDeniedException, GenericException, ValidationException, NotFoundException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, ADMINISTRATION_METADATA_EDITOR_ROLE);
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectModifyPermissions(user, sdo);

    // delegate
    DescriptiveMetadata ret = BrowserHelper.updateDescriptiveMetadataFile(aipId, metadataId, metadataType,
      metadataBinary);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, BROWSER_COMPONENT, "editDescriptiveMetadataFile", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_METADATA_ID, metadataId);

    return ret;
  }

  public static void removeDescriptiveMetadataFile(RodaUser user, String aipId, String metadataId)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, ADMINISTRATION_METADATA_EDITOR_ROLE);
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectModifyPermissions(user, sdo);

    // delegate
    BrowserHelper.removeDescriptiveMetadataFile(aipId, metadataId);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, BROWSER_COMPONENT, "removeMetadataFile", sdo.getId(), duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_METADATA_ID, metadataId);
  }

  public static DescriptiveMetadata retrieveMetadataFile(RodaUser user, String aipId, String metadataId)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, ADMINISTRATION_METADATA_EDITOR_ROLE);
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectModifyPermissions(user, sdo);

    // delegate
    DescriptiveMetadata dm = BrowserHelper.retrieveMetadataFile(aipId, metadataId);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, BROWSER_COMPONENT, "retrieveMetadataFile", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_METADATA_ID, metadataId);

    return dm;
  }

  public static void removeRepresentation(RodaUser user, String aipId, String representationId)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, ADMINISTRATION_METADATA_EDITOR_ROLE);
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectModifyPermissions(user, sdo);

    // delegate
    BrowserHelper.removeRepresentation(aipId, representationId);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, BROWSER_COMPONENT, "removeRepresentation", sdo.getId(), duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_REPRESENTATION_ID, representationId);

  }

  public static void removeRepresentationFile(RodaUser user, String aipId, String representationId, String fileId)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date start = new Date();

    // check user permissions
    UserUtility.checkRoles(user, ADMINISTRATION_METADATA_EDITOR_ROLE);
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectModifyPermissions(user, sdo);

    // delegate
    BrowserHelper.removeRepresentationFile(aipId, representationId, fileId);

    // register action
    long duration = new Date().getTime() - start.getTime();
    registerAction(user, BROWSER_COMPONENT, "removeRepresentationFile", sdo.getId(), duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_REPRESENTATION_ID, representationId,
      RodaConstants.API_PATH_PARAM_FILE_ID, fileId);
  }

  public static StreamResponse getAipRepresentationFile(RodaUser user, String aipId, String representationId,
    String fileId, String acceptFormat)
      throws GenericException, AuthorizationDeniedException, NotFoundException, RequestNotValidException {
    Date startDate = new Date();

    // validate input
    BrowserHelper.validateGetAipRepresentationFileParams(acceptFormat);

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectModifyPermissions(user, sdo);

    // delegate
    StreamResponse aipRepresentationFile = BrowserHelper.getAipRepresentationFile(aipId, representationId, fileId,
      acceptFormat);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "getAipRepresentationFile", aipId, duration,
      RodaConstants.API_PATH_PARAM_REPRESENTATION_ID, representationId, RodaConstants.API_PATH_PARAM_FILE_ID, fileId);

    return aipRepresentationFile;
  }

  public static void putDescriptiveMetadataFile(RodaUser user, String aipId, String metadataId, String metadataType,
    InputStream is, FormDataContentDisposition fileDetail)
      throws GenericException, AuthorizationDeniedException, NotFoundException {
    Date startDate = new Date();

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectInsertPermissions(user, sdo);

    // delegate
    BrowserHelper.createOrUpdateAipDescriptiveMetadataFile(aipId, metadataId, metadataType, is, fileDetail, true);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "putDescriptiveMetadataFile", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_METADATA_ID, metadataId);

  }

  public static void postDescriptiveMetadataFile(RodaUser user, String aipId, String metadataId, String metadataType,
    InputStream is, FormDataContentDisposition fileDetail)
      throws GenericException, AuthorizationDeniedException, NotFoundException {
    Date startDate = new Date();

    // check user permissions
    SimpleDescriptionObject sdo = BrowserHelper.getSimpleDescriptionObject(aipId);
    UserUtility.checkObjectInsertPermissions(user, sdo);

    // delegate
    BrowserHelper.createOrUpdateAipDescriptiveMetadataFile(aipId, metadataId, metadataType, is, fileDetail, true);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "postDescriptiveMetadataFile", aipId, duration,
      RodaConstants.API_PATH_PARAM_AIP_ID, aipId, RodaConstants.API_PATH_PARAM_METADATA_ID, metadataId);

  }

  public static IndexResult<TransferredResource> findTransferredResources(RodaUser user, Filter filter, Sorter sorter,
    Sublist sublist, Facets facets) throws GenericException, AuthorizationDeniedException {
    Date startDate = new Date();

    // check user permissions
    UserUtility.checkRoles(user, INGEST_TRANSFER_ROLE);

    // TODO if not admin, add to filter a constraint for the resource to belong
    // to this user

    // delegate
    IndexResult<TransferredResource> resources = BrowserHelper.findTransferredResources(filter, sorter, sublist,
      facets);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "findTransferredResources", null, duration, FILTER_PARAM, filter,
      SORTER_PARAM, sorter, SUBLIST_PARAM, sublist);

    return resources;
  }

  public static TransferredResource retrieveTransferredResource(RodaUser user, String transferredResourceId)
    throws GenericException, AuthorizationDeniedException, NotFoundException {
    Date startDate = new Date();
    // check user permissions
    UserUtility.checkRoles(user, INGEST_TRANSFER_ROLE);

    // TODO if not admin, add to filter a constraint for the resource to belong
    // to this user

    // delegate
    TransferredResource resource = BrowserHelper.retrieveTransferredResource(transferredResourceId);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "retrieveTransferredResource", null, duration,
      TRANSFERRED_RESOURCE_ID_PARAM, transferredResourceId);

    return resource;
  }

  public static String createTransferredResourcesFolder(RodaUser user, String parent, String folderName)
    throws AuthorizationDeniedException, GenericException {
    Date startDate = new Date();
    // check user permissions
    UserUtility.checkRoles(user, INGEST_TRANSFER_ROLE);

    UserUtility.checkTransferredResourceAccess(user, Arrays.asList(parent));

    // fix parent
    if (parent == null) {
      parent = user.getName();
    }

    // delegate
    String id = BrowserHelper.createTransferredResourcesFolder(parent, folderName);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "createTransferredResourcesFolder", null, duration, PARENT_PARAM, parent,
      FOLDERNAME_PARAM, folderName);
    return id;
  }

  public static void removeTransferredResources(RodaUser user, List<String> ids)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Date startDate = new Date();

    // check user permissions
    UserUtility.checkRoles(user, INGEST_TRANSFER_ROLE);

    UserUtility.checkTransferredResourceAccess(user, ids);

    // delegate
    BrowserHelper.removeTransferredResources(ids);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "removeTransferredResources", null, duration, PATH_PARAM, ids);
  }

  public static void createTransferredResourceFile(RodaUser user, String path, String fileName, InputStream inputStream)
    throws AuthorizationDeniedException, GenericException, FileAlreadyExistsException {
    Date startDate = new Date();

    // check user permissions
    UserUtility.checkRoles(user, INGEST_TRANSFER_ROLE);

    UserUtility.checkTransferredResourceAccess(user, Arrays.asList(path));

    // delegate
    BrowserHelper.createTransferredResourceFile(path, fileName, inputStream);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "createTransferredResourceFile", null, duration, PATH_PARAM, path,
      FILENAME_PARAM, fileName);

  }

  public static StreamResponse getClassificationPlan(RodaUser user, String type) throws GenericException {
    Date startDate = new Date();

    // delegate
    StreamResponse classificationPlan = BrowserHelper.getClassificationPlan(type, user);

    // register action
    long duration = new Date().getTime() - startDate.getTime();
    registerAction(user, BROWSER_COMPONENT, "getClassificationPlan", null, duration, CLASSIFICATION_PLAN_TYPE_PARAMETER,
      type);

    return classificationPlan;
  }

  public static void createTransferredResource(RodaUser user, String parentId, String fileName, InputStream inputStream,
    String name) throws AuthorizationDeniedException, GenericException, AlreadyExistsException {
    if (name == null) {
      try {
        Browser.createTransferredResourceFile(user, parentId, fileName, inputStream);
      } catch (FileAlreadyExistsException e) {
        throw new AlreadyExistsException(ApiException.RESOURCE_ALREADY_EXISTS,
          "File '" + fileName + "' already exists.");
      }
    } else {
      Browser.createTransferredResourcesFolder(user, parentId, name);
    }
  }

  public static boolean isTransferFullyInitialized(RodaUser user) {
    return BrowserHelper.isTransferFullyInitialized();
  }

}
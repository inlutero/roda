/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.model.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.roda.core.common.RodaUtils;
import org.roda.core.data.adapter.filter.BasicSearchFilterParameter;
import org.roda.core.data.adapter.filter.Filter;
import org.roda.core.data.adapter.sublist.Sublist;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.eadc.DescriptionLevel;
import org.roda.core.data.v2.IndexResult;
import org.roda.core.data.v2.LogEntry;
import org.roda.core.data.v2.LogEntryParameter;
import org.roda.core.data.v2.RepresentationState;
import org.roda.core.data.v2.SIPReport;
import org.roda.core.data.v2.SimpleDescriptionObject;
import org.roda.core.index.IndexService;
import org.roda.core.index.IndexServiceException;
import org.roda.core.metadata.v2.premis.PremisAgentHelper;
import org.roda.core.metadata.v2.premis.PremisEventHelper;
import org.roda.core.metadata.v2.premis.PremisFileObjectHelper;
import org.roda.core.metadata.v2.premis.PremisMetadataException;
import org.roda.core.metadata.v2.premis.PremisRepresentationObjectHelper;
import org.roda.core.model.FileFormat;
import org.roda.core.model.ModelServiceException;
import org.roda.core.storage.Binary;
import org.roda.core.storage.ClosableIterable;
import org.roda.core.storage.DefaultStoragePath;
import org.roda.core.storage.Resource;
import org.roda.core.storage.StoragePath;
import org.roda.core.storage.StorageService;
import org.roda.core.storage.StorageServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lc.xmlns.premisV2.AgentComplexType;
import lc.xmlns.premisV2.EventComplexType;
import lc.xmlns.premisV2.File;
import lc.xmlns.premisV2.LinkingAgentIdentifierComplexType;
import lc.xmlns.premisV2.Representation;

/**
 * Model related utility class
 * 
 * @author Hélder Silva <hsilva@keep.pt>
 */
public final class ModelUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(ModelUtils.class);

  public enum PREMIS_TYPE {
    OBJECT, EVENT, AGENT, UNKNOWN
  }

  /**
   * Private empty constructor
   */
  private ModelUtils() {

  }

  /**
   * Builds, from metadata, a {@code FileFormat} object
   * 
   * @param metadata
   *          metadata
   * @throws ModelServiceException
   */
  public static FileFormat getFileFormat(Map<String, Set<String>> metadata) throws ModelServiceException {
    String mimetype = getString(metadata, RodaConstants.STORAGE_META_FORMAT_MIME);
    String version = getString(metadata, RodaConstants.STORAGE_META_FORMAT_VERSION);
    // FIXME how to load format registries if any
    Map<String, String> formatRegistries = new HashMap<String, String>();

    return new FileFormat(mimetype, version, formatRegistries);
  }

  /**
   * Builds, from metadata, a {@code Set<RepresentationState>} object
   * 
   * @param metadata
   *          metadata
   */
  public static Set<RepresentationState> getStatuses(Map<String, Set<String>> metadata) {
    Set<RepresentationState> statuses = new TreeSet<RepresentationState>();
    Set<String> statusesInString = metadata.get(RodaConstants.STORAGE_META_REPRESENTATION_STATUSES);
    for (String statusString : statusesInString) {
      statuses.add(RepresentationState.valueOf(statusString.toUpperCase()));
    }
    return statuses;
  }

  public static <T> T getAs(Map<String, Set<String>> metadata, String key, Class<T> type) throws ModelServiceException {
    T ret;
    Set<String> set = metadata.get(key);
    if (set == null || set.isEmpty()) {
      ret = null;
    } else if (set.size() == 1) {
      String value = set.iterator().next();
      if (type.equals(Date.class)) {
        try {
          ret = type.cast(RodaUtils.parseDate(set.iterator().next()));
        } catch (ParseException e) {
          throw new ModelServiceException("Could not parse date: " + value, ModelServiceException.INTERNAL_SERVER_ERROR,
            e);
        }
      } else if (type.equals(Boolean.class)) {
        ret = type.cast(Boolean.valueOf(value));
      } else if (type.equals(Long.class)) {
        ret = type.cast(Long.valueOf(value));
      } else {
        throw new ModelServiceException(
          "Could not parse date because metadata field has not a single value class is not supported" + type,
          ModelServiceException.INTERNAL_SERVER_ERROR);
      }
    } else {
      throw new ModelServiceException("Could not parse date because metadata field has not a single value, set=" + set,
        ModelServiceException.INTERNAL_SERVER_ERROR);
    }

    return ret;
  }

  public static <T> void setAs(Map<String, Set<String>> metadata, String key, T value) throws ModelServiceException {
    if (value instanceof Date) {
      Date dateValue = (Date) value;
      metadata.put(key, new HashSet<>(Arrays.asList(RodaUtils.dateToString(dateValue))));
    } else if (value instanceof Boolean) {
      Boolean booleanValue = (Boolean) value;
      metadata.put(key, new HashSet<>(Arrays.asList(booleanValue.toString())));
    } else if (value instanceof Long) {
      Long longValue = (Long) value;
      metadata.put(key, new HashSet<>(Arrays.asList(longValue.toString())));
    } else {
      throw new ModelServiceException(
        "Could not set data because value class is not supported" + value.getClass().getName(),
        ModelServiceException.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Reads, from metadata and for a metadata key, an ISO8601 date if it exists
   * 
   * @param metadata
   *          metadata
   * @param key
   *          metadata key
   * @throws ModelServiceException
   */
  public static Date getDate(Map<String, Set<String>> metadata, String key) throws ModelServiceException {
    return getAs(metadata, key, Date.class);
  }

  /**
   * Reads, from metadata and for a metadata key, a boolean if it exists
   * 
   * @param metadata
   *          metadata
   * @param key
   *          metadata key
   * @throws ModelServiceException
   */
  public static Boolean getBoolean(Map<String, Set<String>> metadata, String key) throws ModelServiceException {
    return getAs(metadata, key, Boolean.class);
  }

  /**
   * Reads, from metadata and for a metadata key, a boolean if it exists
   * 
   * @param metadata
   *          metadata
   * @param key
   *          metadata key
   * @throws ModelServiceException
   */
  public static Long getLong(Map<String, Set<String>> metadata, String key) throws ModelServiceException {
    return getAs(metadata, key, Long.class);
  }

  /**
   * Reads, from metadata and for a metadata key, a string if it exists
   * 
   * @param metadata
   *          metadata
   * @param key
   *          metadata key
   * @throws ModelServiceException
   */
  public static String getString(Map<String, Set<String>> metadata, String key) throws ModelServiceException {
    String ret;
    Set<String> set = metadata.get(key);
    if (set == null || set.isEmpty()) {
      ret = null;
    } else if (set.size() == 1) {
      ret = set.iterator().next();
    } else {
      throw new ModelServiceException("Could not parse date because metadata field has multiple values, set=" + set,
        ModelServiceException.INTERNAL_SERVER_ERROR);
    }

    return ret;
  }

  /**
   * Returns a list of ids from the children of a certain resource
   * 
   * @param storage
   *          the storage service containing the parent resource
   * @param path
   *          the storage path for the parent resource
   * @throws ModelServiceException
   */
  public static List<String> getChildIds(StorageService storage, StoragePath path, boolean failIfParentDoesNotExist)
    throws ModelServiceException {
    List<String> ids = new ArrayList<String>();
    ClosableIterable<Resource> iterable = null;
    try {
      iterable = storage.listResourcesUnderDirectory(path);
      Iterator<Resource> it = iterable.iterator();
      while (it.hasNext()) {
        Resource next = it.next();
        if (next != null) {
          StoragePath storagePath = next.getStoragePath();
          ids.add(storagePath.getName());
        } else {
          LOGGER.error("Error while getting IDs for path " + path.asString());
        }
      }
    } catch (StorageServiceException e) {
      if (e.getCode() != StorageServiceException.NOT_FOUND || failIfParentDoesNotExist) {
        throw new ModelServiceException("Could not get ids", e.getCode(), e);
      }
    }

    if (iterable != null) {
      try {
        iterable.close();
      } catch (IOException e) {
        LOGGER.warn("Error closing iterator on getIds()", e);
      }
    }

    return ids;
  }

  // /**
  // * Returns a list of ids from the children of a certain resources, starting
  // * with the prefix defined
  // *
  // * @param storage
  // * the storage service containing the parent resource
  // * @param path
  // * the storage paths for the parent resources
  // * @param prefix
  // * the prefix of the children
  // * @throws StorageServiceException
  // */
  // public static List<String> getIds(StorageService storage, List<StoragePath>
  // paths, String prefix)
  // throws StorageServiceException {
  // List<String> ids = new ArrayList<String>();
  // for (StoragePath path : paths) {
  // if (path.getName().startsWith(prefix)) {
  // ids.add(path.getName());
  // }
  // }
  // return ids;
  //
  // }

  public static StoragePath getAIPcontainerPath() throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP);
  }

  public static StoragePath getAIPpath(String aipId) throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipId);
  }

  public static StoragePath getDescriptiveMetadataPath(String aipId) throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipId,
      RodaConstants.STORAGE_DIRECTORY_METADATA, RodaConstants.STORAGE_DIRECTORY_DESCRIPTIVE);
  }

  public static StoragePath getDescriptiveMetadataPath(String aipId, String descriptiveMetadataBinaryId)
    throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipId,
      RodaConstants.STORAGE_DIRECTORY_METADATA, RodaConstants.STORAGE_DIRECTORY_DESCRIPTIVE,
      descriptiveMetadataBinaryId);
  }

  public static StoragePath getRepresentationsPath(String aipId) throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipId, RodaConstants.STORAGE_DIRECTORY_DATA);
  }

  public static StoragePath getRepresentationPath(String aipId, String representationId)
    throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipId, RodaConstants.STORAGE_DIRECTORY_DATA,
      representationId);
  }

  public static StoragePath getRepresentationFilePath(String aipId, String representationId, String fileId)
    throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipId, RodaConstants.STORAGE_DIRECTORY_DATA,
      representationId, fileId);
  }

  public static String getAIPidFromStoragePath(StoragePath path) {
    return path.getDirectoryPath().get(0);
  }

  public static String getRepresentationIdFromStoragePath(StoragePath path) throws ModelServiceException {
    if (path.getDirectoryPath().size() >= 3) {
      return path.getDirectoryPath().get(2);
    } else {
      throw new ModelServiceException(
        "Error while trying to obtain representation id from storage path (length is not 3 or above)",
        ModelServiceException.INTERNAL_SERVER_ERROR);
    }
  }

  public static StoragePath getPreservationPath(String aipId, String representationID) throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipId,
      RodaConstants.STORAGE_DIRECTORY_METADATA, RodaConstants.STORAGE_DIRECTORY_PRESERVATION, representationID);

  }

  public static StoragePath getPreservationPath(String aipId) throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipId,
      RodaConstants.STORAGE_DIRECTORY_METADATA, RodaConstants.STORAGE_DIRECTORY_PRESERVATION);

  }

  public static StoragePath getPreservationFilePath(String aipId, String representationID, String fileID)
    throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipId,
      RodaConstants.STORAGE_DIRECTORY_METADATA, RodaConstants.STORAGE_DIRECTORY_PRESERVATION, representationID, fileID);

  }

  public static Representation getPreservationRepresentationObject(Binary preservationBinary) {
    Representation representation = null;
    InputStream binaryInputStream = null;
    try {
      binaryInputStream = preservationBinary.getContent().createInputStream();
      representation = PremisRepresentationObjectHelper.newInstance(binaryInputStream).getRepresentation();
    } catch (PremisMetadataException | IOException | ClassCastException e) {
      representation = null;
    } finally {
      if (binaryInputStream != null) {
        try {
          binaryInputStream.close();
        } catch (IOException e1) {
          LOGGER.warn("Cannot close file inputstream", e1);
        }
      }
    }
    return representation;
  }

  public static EventComplexType getPreservationEvent(Binary preservationBinary) {
    EventComplexType event = null;
    InputStream binaryInputStream = null;
    try {
      binaryInputStream = preservationBinary.getContent().createInputStream();
      event = PremisEventHelper.newInstance(binaryInputStream).getEvent();
    } catch (PremisMetadataException | IOException | ClassCastException e) {
      event = null;
    } finally {
      if (binaryInputStream != null) {
        try {
          binaryInputStream.close();
        } catch (IOException e1) {
          LOGGER.warn("Cannot close file inputstream", e1);
        }
      }
    }
    return event;
  }

  public static File getPreservationFileObject(Binary preservationBinary) {
    File file = null;
    InputStream binaryInputStream = null;
    try {
      binaryInputStream = preservationBinary.getContent().createInputStream();
      file = PremisFileObjectHelper.newInstance(binaryInputStream).getFile();
    } catch (PremisMetadataException | IOException | ClassCastException e) {
      file = null;
    } finally {
      if (binaryInputStream != null) {
        try {
          binaryInputStream.close();
        } catch (IOException e1) {
          LOGGER.warn("Cannot close file inputstream", e1);
        }
      }
    }
    return file;
  }

  public static AgentComplexType getPreservationAgentObject(Binary preservationBinary) {
    AgentComplexType agent = null;
    InputStream binaryInputStream = null;
    try {
      binaryInputStream = preservationBinary.getContent().createInputStream();
      agent = PremisAgentHelper.newInstance(binaryInputStream).getAgent();
    } catch (PremisMetadataException | IOException | ClassCastException e) {
      agent = null;
    } finally {
      if (binaryInputStream != null) {
        try {
          binaryInputStream.close();
        } catch (IOException e1) {
          LOGGER.warn("Cannot close file inputstream", e1);
        }
      }
    }
    return agent;
  }

  public static StoragePath getPreservationAgentPath(String agentID) throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_PRESERVATION,
      RodaConstants.STORAGE_DIRECTORY_AGENTS, agentID);
  }

  public static StoragePath getLogPath(String logFile) throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_ACTIONLOG, logFile);
  }

  // @Deprecated
  // public static StoragePath getLogPath(Date d) throws StorageServiceException
  // {
  // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  // String logFile = sdf.format(d) + ".log";
  // return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_ACTIONLOG,
  // logFile);
  // }

  public static void writeLogEntryToFile(LogEntry logEntry, Path logFile) throws ModelServiceException {
    try {
      String entryJSON = ModelUtils.getJsonFromObject(logEntry) + "\n";
      Files.write(logFile, entryJSON.getBytes(), StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new ModelServiceException("Error writing log entry to file", ModelServiceException.INTERNAL_SERVER_ERROR,
        e);
    }
  }

  public static void writeObjectToFile(Object object, Path file) throws ModelServiceException {
    try {
      String json = ModelUtils.getJsonFromObject(object) + "\n";
      Files.write(file, json.getBytes(), StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new ModelServiceException("Error writing object, as json, to file",
        ModelServiceException.INTERNAL_SERVER_ERROR, e);
    }
  }

  public static Map<String, String> getMapFromJson(String json) {
    Map<String, String> ret = new HashMap<String, String>();
    try {
      JsonFactory factory = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      ret = mapper.readValue(json, new TypeReference<Map<String, String>>() {
      });
    } catch (IOException e) {
      LOGGER.error("Error transforming json string to log entry parameters", e);
    }
    return ret;
  }

  public static String getJsonFromObject(Object object) {
    String ret = null;
    try {
      JsonFactory factory = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      ret = mapper.writeValueAsString(object);
    } catch (IOException e) {
      LOGGER.error("Error transforming object '" + object + "' to json string", e);
    }
    return ret;
  }

  public static String getJsonLogEntryParameters(List<LogEntryParameter> parameters) {
    String ret = "";
    try {
      JsonFactory factory = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      ret = mapper.writeValueAsString(parameters);
    } catch (IOException e) {
      LOGGER.error("Error transforming log entry parameter to json string", e);
    }
    return ret;
  }

  public static List<LogEntryParameter> getLogEntryParameters(String json) {
    List<LogEntryParameter> ret = new ArrayList<LogEntryParameter>();
    try {
      JsonFactory factory = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      ret = mapper.readValue(json, new TypeReference<List<LogEntryParameter>>() {
      });
    } catch (IOException e) {
      LOGGER.error("Error transforming json string to log entry parameters", e);
    }
    return ret;
  }

  public static LogEntry getLogEntry(String json) {
    LogEntry ret = null;
    try {
      JsonFactory factory = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      ret = mapper.readValue(json, LogEntry.class);
    } catch (IOException e) {
      LOGGER.error("Error transforming json string to log entry", e);
    }
    return ret;
  }

  public static SIPReport getSipState(String json) {
    try {
      JsonFactory factory = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      return mapper.readValue(json, SIPReport.class);
    } catch (IOException e) {
      LOGGER.error("Error transforming json string to sip state", e);
    }
    return null;
  }

  public static <T> List<String> extractAgentIdsFromPreservationBinary(Binary b, Class<T> c)
    throws ModelServiceException {
    List<String> ids = new ArrayList<String>();
    if (c.equals(File.class)) {
      // TODO
      LOGGER.error("Not implemented!");
    } else if (c.equals(EventComplexType.class)) {
      EventComplexType event = getPreservationEvent(b);
      List<LinkingAgentIdentifierComplexType> identifiers = event.getLinkingAgentIdentifierList();
      if (identifiers != null) {
        for (LinkingAgentIdentifierComplexType laict : identifiers) {
          ids.add(laict.getLinkingAgentIdentifierValue());
        }
      }
    } else if (c.equals(Representation.class)) {
      // TODO
      LOGGER.error("Not implemented!");
    } else {
      // TODO
      LOGGER.error("Not implemented!");
    }
    return ids;
  }

  public static String getPreservationType(Binary binary) {
    String type = "";
    EventComplexType event = ModelUtils.getPreservationEvent(binary);
    if (event != null) {
      type = "event";
    } else {
      lc.xmlns.premisV2.File file = ModelUtils.getPreservationFileObject(binary);
      if (file != null) {
        type = "file";
      } else {
        AgentComplexType agent = ModelUtils.getPreservationAgentObject(binary);
        if (agent != null) {
          type = "agent";
        } else {
          Representation representation = ModelUtils.getPreservationRepresentationObject(binary);
          if (representation != null) {
            type = "representation";
          } else {
            type = "unknown";
          }
        }
      }
    }
    return type;

  }

  public static StoragePath getOtherMetadataDirectory(String aipID) throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipID,
      RodaConstants.STORAGE_DIRECTORY_METADATA, RodaConstants.STORAGE_DIRECTORY_OTHER);
  }

  public static StoragePath getToolMetadataDirectory(String aipID, String type) throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipID,
      RodaConstants.STORAGE_DIRECTORY_METADATA, RodaConstants.STORAGE_DIRECTORY_OTHER, type);
  }

  public static StoragePath getToolRepresentationMetadataDirectory(String aipID, String representationId, String type)
    throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipID,
      RodaConstants.STORAGE_DIRECTORY_METADATA, RodaConstants.STORAGE_DIRECTORY_OTHER, type, representationId);
  }

  public static StoragePath getToolMetadataPath(String aipID, String representationId, String fileName, String type)
    throws StorageServiceException {
    return DefaultStoragePath.parse(RodaConstants.STORAGE_CONTAINER_AIP, aipID,
      RodaConstants.STORAGE_DIRECTORY_METADATA, RodaConstants.STORAGE_DIRECTORY_OTHER, type, representationId,
      fileName);

  }

  public static ObjectNode sdoToJSON(SimpleDescriptionObject sdo, IndexService index, ObjectMapper mapper,
    List<DescriptionLevel> representationLevels) throws IndexServiceException {
    ObjectNode node = mapper.createObjectNode();
    if (sdo.getTitle() != null) {
      node = node.put("title", sdo.getTitle());
    }
    if (sdo.getId() != null) {
      node = node.put("id", sdo.getId());
    }
    if (sdo.getParentID() != null) {
      node = node.put("parentId", sdo.getParentID());
    }
    if (sdo.getLevel() != null) {
      node = node.put("descriptionlevel", sdo.getLevel());
    }
    Filter filter = new Filter(new BasicSearchFilterParameter(RodaConstants.AIP_PARENT_ID, sdo.getId()));
    long countChildren = index.count(SimpleDescriptionObject.class, filter);
    ArrayNode childrenArray = mapper.createArrayNode();
    if (countChildren > 0) {
      for (int i = 0; i < countChildren; i += 100) {
        IndexResult<SimpleDescriptionObject> collections = index.find(SimpleDescriptionObject.class, filter, null,
          new Sublist(i, 100));
        for (SimpleDescriptionObject children : collections.getResults()) {
          if (sdo.getLevel() != null && !ModelUtils.isRepresentationLevel(children, representationLevels)) {
            childrenArray = childrenArray.add(ModelUtils.sdoToJSON(children, index, mapper, representationLevels));
          }
        }
      }
    }
    node.set("children", childrenArray);
    return node;
  }

  public static boolean isRepresentationLevel(SimpleDescriptionObject sdo,
    List<DescriptionLevel> representationLevels) {
    boolean isRepresentationLevel = false;
    for (DescriptionLevel dl : representationLevels) {
      if (dl.getLevel().equalsIgnoreCase(sdo.getLevel())) {
        isRepresentationLevel = true;
        break;
      }
    }
    return isRepresentationLevel;
  }
}
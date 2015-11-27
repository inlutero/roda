/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.plugins.plugins.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.roda.core.data.PluginParameter;
import org.roda.core.data.Report;
import org.roda.core.data.common.InvalidParameterException;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.SimpleDescriptionObject;
import org.roda.core.index.IndexService;
import org.roda.core.model.AIP;
import org.roda.core.model.ModelService;
import org.roda.core.model.ModelServiceException;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.storage.StoragePath;
import org.roda.core.storage.StorageService;
import org.roda.core.storage.StorageServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveOrphansPlugin implements Plugin<SimpleDescriptionObject> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RemoveOrphansPlugin.class);
  private AIP newParent;

  @Override
  public void init() throws PluginException {
    // do nothing
  }

  @Override
  public void shutdown() {
    // do nothing
  }

  @Override
  public String getName() {
    return "MoveOrphansToNewParent";
  }

  @Override
  public String getDescription() {
    return "Moves the orphans AIP (not Fonds) to a new parent";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public List<PluginParameter> getParameters() {
    return new ArrayList<>();
  }

  @Override
  public Map<String, String> getParameterValues() {
    return new HashMap<>();
  }

  @Override
  public void setParameterValues(Map<String, String> parameters) throws InvalidParameterException {
    // no params
  }

  public AIP getNewParent() {
    return newParent;
  }

  public void setNewParent(AIP newParent) {
    this.newParent = newParent;
  }

  @Override
  public Report execute(IndexService index, ModelService model, StorageService storage,
    List<SimpleDescriptionObject> list) throws PluginException {

    for (SimpleDescriptionObject sdo : list) {
      try {
        LOGGER.debug("Processing AIP " + sdo.getId());
        if (sdo.getLevel() == null || !sdo.getLevel().trim().equalsIgnoreCase("fonds")) {
          AIP aip = model.retrieveAIP(sdo.getId());
          StoragePath aiPpath = ModelUtils.getAIPpath(aip.getId());

          LOGGER.debug("  Moving orphan " + sdo.getId() + " to under " + newParent.getId());
          Map<String, Set<String>> simpleDescriptionObjectMetadata = storage.getMetadata(aiPpath);
          simpleDescriptionObjectMetadata.put(RodaConstants.STORAGE_META_PARENT_ID,
            new HashSet<String>(Arrays.asList(newParent.getId())));
          storage.updateMetadata(aiPpath, simpleDescriptionObjectMetadata, true);
          aip.setParentId(newParent.getId());
          index.reindexAIP(aip);
        } else {
          LOGGER.debug("  AIP doesn't need to be moved... Level: " + sdo.getLevel());
        }
      } catch (StorageServiceException | ModelServiceException e) {
        LOGGER.error("Error processing SimpleDescriptionObject " + sdo.getId() + " (RemoveOrphansAction)");
        LOGGER.error(e.getMessage(), e);
      }
    }
    return null;
  }

  @Override
  public Report beforeExecute(IndexService index, ModelService model, StorageService storage) throws PluginException {

    return null;
  }

  @Override
  public Report afterExecute(IndexService index, ModelService model, StorageService storage) throws PluginException {
    LOGGER.debug("End");
    return null;
  }

  @Override
  public Plugin<SimpleDescriptionObject> cloneMe() {
    return new RemoveOrphansPlugin();
  }

}
/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.plugins.orchestrate.akka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.InvalidParameterException;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.v2.Job;
import org.roda.core.data.v2.Job.ORCHESTRATOR_METHOD;
import org.roda.core.data.v2.TransferredResource;
import org.roda.core.index.IndexServiceException;
import org.roda.core.plugins.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.UntypedActor;

public class AkkaJobWorkerActor extends UntypedActor {
  private static final Logger LOGGER = LoggerFactory.getLogger(AkkaJobWorkerActor.class);

  public AkkaJobWorkerActor() {

  }

  @Override
  public void onReceive(Object msg) throws Exception {
    if (msg instanceof Job) {
      Job job = (Job) msg;
      if (ORCHESTRATOR_METHOD.ON_TRANSFERRED_RESOURCES == job.getOrchestratorMethod()) {
        Plugin<TransferredResource> plugin = (Plugin<TransferredResource>) RodaCoreFactory.getPluginManager()
          .getPlugin(job.getPlugin());
        Map<String, String> parameters = new HashMap<String, String>(job.getPluginParameters());
        parameters.put(RodaConstants.PLUGIN_PARAMS_JOB_ID, job.getId());
        try {
          plugin.setParameterValues(parameters);
        } catch (InvalidParameterException e) {
          LOGGER.error("Error setting plug-in parameters", e);
        }

        RodaCoreFactory.getPluginOrchestrator().runPluginOnTransferredResources(plugin,
          getTransferredResourcesFromObjectIds(job.getObjectIds()));
      }
    }
  }

  public List<TransferredResource> getTransferredResourcesFromObjectIds(List<String> objectIds)
    throws NotFoundException {
    List<TransferredResource> res = new ArrayList<TransferredResource>();
    for (String objectId : objectIds) {
      try {
        res.add(RodaCoreFactory.getIndexService().retrieve(TransferredResource.class, objectId));
      } catch (IndexServiceException e) {
        LOGGER.error("Error retrieving TransferredResource", e);
      }
    }
    return res;
  }

}
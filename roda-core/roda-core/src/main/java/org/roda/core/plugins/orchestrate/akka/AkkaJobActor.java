/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.plugins.orchestrate.akka;

import java.io.Serializable;
import java.util.Optional;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.index.IsIndexed;
import org.roda.core.data.v2.index.SelectedItemsAll;
import org.roda.core.data.v2.index.SelectedItemsFilter;
import org.roda.core.data.v2.index.SelectedItemsList;
import org.roda.core.data.v2.index.SelectedItemsNone;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.core.data.v2.ip.Representation;
import org.roda.core.data.v2.ip.TransferredResource;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.Job.JOB_STATE;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.orchestrate.JobsHelper;
import org.roda.core.plugins.plugins.PluginHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class AkkaJobActor extends UntypedActor {
  private static final Logger LOGGER = LoggerFactory.getLogger(AkkaJobActor.class);

  public AkkaJobActor() {

  }

  @Override
  public void onReceive(Object msg) throws Exception {
    if (msg instanceof Job) {
      Job job = (Job) msg;
      Plugin<?> plugin = (Plugin<?>) RodaCoreFactory.getPluginManager().getPlugin(job.getPlugin());
      PluginHelper.setPluginParameters(plugin, job);

      String jobId = job.getId();
      ActorRef jobInfoActor = getContext().actorOf(Props.create(AkkaJobInfoActor.class, plugin), jobId);
      RodaCoreFactory.getPluginOrchestrator().setInitialJobInfo(jobId, jobInfoActor);

      PluginHelper.updateJobState(plugin, JOB_STATE.STARTED, Optional.empty());

      try {
        if (job.getSourceObjects() instanceof SelectedItemsAll<?>) {
          runOnAll(job, plugin);
        } else if (job.getSourceObjects() instanceof SelectedItemsNone<?>) {
          RodaCoreFactory.getPluginOrchestrator().runPlugin(plugin);
        } else if (job.getSourceObjects() instanceof SelectedItemsList<?>) {
          runFromList(job, plugin);
        } else if (job.getSourceObjects() instanceof SelectedItemsFilter<?>) {
          runFromFilter(job, plugin);
        }
      } catch (GenericException e) {
        jobInfoActor.tell(
          new Messages.JobStateUpdated(plugin, JOB_STATE.FAILED_TO_COMPLETE, Optional.ofNullable(e.getMessage())),
          ActorRef.noSender());
      }

    } else {
      LOGGER.error(AkkaJobActor.class.getName() + " received a message that it doesn't know how to process...");
    }
  }

  private void runOnAll(Job job, Plugin<?> plugin) throws GenericException {
    // get class
    Class<Serializable> sourceObjectsClass = JobsHelper
      .getSelectedClassFromString(job.getSourceObjects().getSelectedClass());

    if (AIP.class.getCanonicalName().equals(sourceObjectsClass.getCanonicalName())) {
      RodaCoreFactory.getPluginOrchestrator().runPluginOnAllAIPs((Plugin<AIP>) plugin);
    } else if (Representation.class.getCanonicalName().equals(sourceObjectsClass.getCanonicalName())) {
      RodaCoreFactory.getPluginOrchestrator().runPluginOnAllRepresentations((Plugin<Representation>) plugin);
    } else if (File.class.getCanonicalName().equals(sourceObjectsClass.getCanonicalName())) {
      RodaCoreFactory.getPluginOrchestrator().runPluginOnAllFiles((Plugin<File>) plugin);
    } else {
      LOGGER.error("Error executing job on unknown source objects class '{}'", sourceObjectsClass.getCanonicalName());
      throw new GenericException(
        "Error executing job on unknown source objects class '" + sourceObjectsClass.getCanonicalName() + "'");
    }
  }

  private void runFromList(Job job, Plugin<?> plugin) throws GenericException {
    // get class
    Class<Serializable> sourceObjectsClass = JobsHelper
      .getSelectedClassFromString(job.getSourceObjects().getSelectedClass());

    if (AIP.class.getCanonicalName().equals(sourceObjectsClass.getCanonicalName())) {
      RodaCoreFactory.getPluginOrchestrator().runPluginOnAIPs((Plugin<AIP>) plugin,
        ((SelectedItemsList<IndexedAIP>) job.getSourceObjects()).getIds(), true);
    } else if (IndexedAIP.class.getCanonicalName().equals(sourceObjectsClass.getCanonicalName())) {
      RodaCoreFactory.getPluginOrchestrator().runPluginOnAIPs((Plugin<AIP>) plugin,
        ((SelectedItemsList<IndexedAIP>) job.getSourceObjects()).getIds(), false);
    } else if (IndexedRepresentation.class.getCanonicalName().equals(sourceObjectsClass.getCanonicalName())) {
      RodaCoreFactory.getPluginOrchestrator().runPluginOnRepresentations((Plugin<Representation>) plugin,
        ((SelectedItemsList<IndexedRepresentation>) job.getSourceObjects()).getIds());
    } else if (IndexedFile.class.getCanonicalName().equals(sourceObjectsClass.getCanonicalName())) {
      RodaCoreFactory.getPluginOrchestrator().runPluginOnFiles((Plugin<File>) plugin,
        ((SelectedItemsList<IndexedFile>) job.getSourceObjects()).getIds());
    } else if (TransferredResource.class.getCanonicalName().equals(sourceObjectsClass.getCanonicalName())) {
      RodaCoreFactory.getPluginOrchestrator().runPluginOnTransferredResources((Plugin<TransferredResource>) plugin,
        ((SelectedItemsList<TransferredResource>) job.getSourceObjects()).getIds());
    } else {
      LOGGER.error("Error executing job on unknown source objects class '{}'", sourceObjectsClass.getCanonicalName());
      throw new GenericException(
        "Error executing job on unknown source objects class '" + sourceObjectsClass.getCanonicalName() + "'");
    }
  }

  private void runFromFilter(Job job, Plugin<?> plugin) throws GenericException, RequestNotValidException {
    // cast
    SelectedItemsFilter<?> selectedItems = (SelectedItemsFilter<?>) job.getSourceObjects();

    // get class
    Class<IsIndexed> sourceObjectsClass = JobsHelper
      .getIsIndexedSelectedClassFromString(selectedItems.getSelectedClass());

    // count objects & update job stats
    Long objectsCount = RodaCoreFactory.getIndexService().count(sourceObjectsClass, selectedItems.getFilter());
    PluginHelper.updateJobObjectsCount(plugin, RodaCoreFactory.getModelService(), objectsCount);

    // execute
    RodaCoreFactory.getPluginOrchestrator().runPluginFromIndex(sourceObjectsClass, selectedItems.getFilter(),
      (Plugin) plugin);
  }

}
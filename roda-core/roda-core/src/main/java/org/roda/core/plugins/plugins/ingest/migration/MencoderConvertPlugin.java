package org.roda.core.plugins.plugins.ingest.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.storage.Binary;
import org.roda.core.storage.StorageService;
import org.roda.core.util.CommandException;

public class MencoderConvertPlugin extends GeneralCommandConvertPlugin {

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
    return "Mencoder video conversion";
  }

  @Override
  public String getDescription() {
    return "Generates a video format file from other video format one using Mencoder.";
  }

  @Override
  public String getVersion() {
    try {
      return MencoderConvertPluginUtils.getVersion();
    } catch (CommandException e) {
      logger.debug("Error getting Mencoder version");
      return new String();
    }
  }

  @Override
  public Plugin<AIP> cloneMe() {
    return new MencoderConvertPlugin();
  }

  @Override
  public List<PluginParameter> getParameters() {
    String outputFormats = RodaCoreFactory.getRodaConfigurationAsString("tools", "mencoderconvert", "general",
      "outputFormats");
    convertableTo.addAll(Arrays.asList(outputFormats.split(" ")));

    List<PluginParameter> params = new ArrayList<PluginParameter>();

    PluginParameter outputParam = new PluginParameter("outputParams", "Output parameters", PluginParameterType.STRING,
      "", convertableTo, true, true, "Lists the possible output formats");

    PluginParameter commandArgs = new PluginParameter("commandArgs", "Command arguments", PluginParameterType.STRING,
      "", true, true, "Command arguments to modify the command to execute");

    params.add(outputParam);
    params.add(commandArgs);
    return params;
  }

  @Override
  public Path executePlugin(Binary binary) throws UnsupportedOperationException, IOException, CommandException {
    Path uriPath = Paths.get(binary.getContent().getURI());
    Path pluginResult;

    if (Files.exists(uriPath)) {
      pluginResult = MencoderConvertPluginUtils
        .runMencoderConvert(uriPath, inputFormat, outputFormat, commandArguments);
    } else {
      pluginResult = MencoderConvertPluginUtils.runMencoderConvert(binary.getContent().createInputStream(),
        inputFormat, outputFormat, commandArguments);
    }

    return pluginResult;
  }

  @Override
  public Report beforeExecute(IndexService index, ModelService model, StorageService storage) throws PluginException {
    return null;
  }

  @Override
  public Report afterExecute(IndexService index, ModelService model, StorageService storage) throws PluginException {
    return null;
  }

}
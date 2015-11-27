/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.plugins.plugins.ingest.characterization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.roda.core.RodaCoreFactory;
import org.roda.core.plugins.PluginException;
import org.roda.core.storage.Binary;
import org.roda.core.util.CommandException;
import org.roda.core.util.CommandUtility;

public class JpylyzerPluginUtils {
  static final private Logger logger = LoggerFactory.getLogger(JpylyzerPluginUtils.class);

  public static Path inspect(File f) throws PluginException {
    try {
      List<String> command = getCommand();
      command.add(f.getAbsolutePath());
      String jpylyzerOutput = CommandUtility.execute(command);
      Path p = Files.createTempFile("jpylyzer", ".xml");
      Files.write(p, jpylyzerOutput.getBytes());
      return p;
    } catch (CommandException e) {
      throw new PluginException("Error while executing jpylyzer command");
    } catch (IOException e) {
      throw new PluginException("Error while parsing jpylyzer output");
    }
  }

  private static List<String> getCommand() {
    Path rodaHome = RodaCoreFactory.getRodaHomePath();
    Path jpylyzerHome = rodaHome.resolve(RodaCoreFactory.getRodaConfigurationAsString("tools", "jpylyzer", "path"));

    File JPYLYZER_DIRECTORY = jpylyzerHome.toFile();

    String osName = System.getProperty("os.name");
    List<String> command;
    if (osName.startsWith("Windows")) {
      command = new ArrayList<String>(
        Arrays.asList(JPYLYZER_DIRECTORY.getAbsolutePath() + File.separator + "jpylyzer.exe"));
    } else {
      command = new ArrayList<String>(
        Arrays.asList(JPYLYZER_DIRECTORY.getAbsolutePath() + File.separator + "jpylyzer"));
    }
    return command;
  }

  public static Path runFFProbe(org.roda.core.model.File file, Binary binary, Map<String, String> parameterValues)
    throws IOException, PluginException {
    java.io.File f = File.createTempFile("temp", ".temp");
    FileOutputStream fos = new FileOutputStream(f);
    IOUtils.copy(binary.getContent().createInputStream(), fos);
    fos.close();
    return inspect(f);
  }
}
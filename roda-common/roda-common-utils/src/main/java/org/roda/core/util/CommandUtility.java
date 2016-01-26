/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * System command utility.
 * 
 * @author Rui Castro
 * @author Luis Faria
 */
public class CommandUtility {
  private static final Logger logger = LoggerFactory.getLogger(CommandUtility.class);

  /**
   * Execute the given command line.
   * 
   * @param args
   *          the command line as a list of arguments.
   * 
   * @return a {@link String} with the output of the command.
   * 
   * @throws CommandException
   */
  public static String execute(String... args) throws CommandException {
    return execute(true, args);
  }

  /**
   * Execute the given command line.
   * 
   * @param args
   *          the command line as a list of arguments.
   * 
   * @return a {@link String} with the output of the command.
   * 
   * @throws CommandException
   */
  public static String execute(boolean withErrorStream, String... args) throws CommandException {

    int exitValue = 0;
    String output;

    try {

      StringBuilder builder = new StringBuilder();
      for (String arg : args) {
        builder.append(arg + " "); //$NON-NLS-1$
      }
      logger.debug("Executing '" + builder.toString() + "'"); //$NON-NLS-1$ //$NON-NLS-2$

      // create and execute process
      ProcessBuilder processBuilder = new ProcessBuilder(args);
      processBuilder.redirectErrorStream(withErrorStream);
      Process process = processBuilder.start();

      // Get process output
      InputStream is = process.getInputStream();

      // output = StreamUtility.inputStreamToString(is);

      CaptureOutputThread captureOutputThread = new CaptureOutputThread(is);

      synchronized (is) {

        captureOutputThread.start();

        // Wait until the CaptureOutputThread notifies that is finished
        // reading the input stream.
        logger.debug("Waiting until CaptureOutputThread notifies");
        is.wait();

      }

      logger.debug("CaptureOutputThread notified. Getting output...");

      output = captureOutputThread.output;

      // Get process exit value
      exitValue = process.waitFor();

      // Closing streams in the hopes of fixing error
      // "java.io.IOException: Too many open files" in roda-migrator
      is.close();

      logger.debug("Command " + Arrays.toString(args) + " terminated with value " + exitValue);

      if (exitValue == 0) {
        return output.toString();
      } else {
        throw new CommandException("Command " + Arrays.toString(args) + " terminated with error code " + exitValue,
          exitValue, output);
      }

    } catch (IOException e) {

      logger.debug("Error executing command " + Arrays.toString(args) + " - " + e.getMessage(), e);
      throw new CommandException("Error executing command " + Arrays.toString(args) + " - " + e.getMessage(), e);

    } catch (InterruptedException e) {

      logger.debug("Error executing command " + Arrays.toString(args) + " - " + e.getMessage(), e);
      throw new CommandException("Error executing command " + Arrays.toString(args) + " - " + e.getMessage(), e);
    }
  }

  /**
   * Execute the given command line.
   * 
   * @param args
   *          the command line as a list of arguments.
   * 
   * @return a {@link String} with the output of the command.
   * 
   * @throws CommandException
   */
  public static String execute(List<String> args) throws CommandException {
    return execute(args, true);
  }

  public static String execute(List<String> args, boolean withErrorStream) throws CommandException {
    return execute(withErrorStream, args.toArray(new String[args.size()]));
  }

}

class CaptureOutputThread extends Thread {
  private static final Logger logger = LoggerFactory.getLogger(CaptureOutputThread.class);

  InputStream is;
  String output;

  public CaptureOutputThread(InputStream is) {
    this.is = is;
  }

  public void run() {

    StringBuffer outputBuffer = new StringBuffer();

    try {

      // output = StreamUtility.inputStreamToString(is);
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));

      String line = null;

      while ((line = reader.readLine()) != null) {
        outputBuffer.append(line + "\n"); //$NON-NLS-1$
        logger.trace(line);
      }

    } catch (IOException e) {
      logger.error("Exception reading from input stream - " //$NON-NLS-1$
        + e.getMessage(), e);
    }

    output = outputBuffer.toString();

    synchronized (is) {
      is.notify();
    }
  }
}

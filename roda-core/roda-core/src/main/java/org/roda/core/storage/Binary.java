/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.storage;

import java.util.Map;

public interface Binary extends Resource {

  /**
   * Retrieve the payload for the data of the binary file.
   * 
   * @return
   */
  public ContentPayload getContent();

  /**
   * The total number of bytes of content of this resource.
   * 
   * @return
   */
  public Long getSizeInBytes();

  /**
   * The binary is a reference to the real content, which is managed externally.
   * 
   * @return
   */
  public boolean isReference();

  /**
   * Get the checksums of the binary content.
   * 
   * @return A map with all the checksums where the key is the checksum
   *         algorithm and the value is the value of the checksum for that
   *         algorithm.
   * 
   *         Example: {("md5", "1234abc..."), ("sha1", "1234567890abc...")}
   * 
   */
  public Map<String, String> getContentDigest();

  /**
   * OTHER METHODS TO CONSIDER:
   * 
   * 
   * * Get file format of the binary content.
   * 
   * Example: {("mime", "application/pdf"), ("pronom", "fmt/13")}
   * 
   * public Map<String, String> getContentFileFormat();
   * 
   * Chance also to just add the getMimeType() as more systems support this
   * out-of-the-box.
   * 
   */
}
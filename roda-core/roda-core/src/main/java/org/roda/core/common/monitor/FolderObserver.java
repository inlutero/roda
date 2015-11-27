/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.common.monitor;

import org.roda.core.data.v2.TransferredResource;

public interface FolderObserver {
  public void transferredResourceAdded(TransferredResource resource, boolean commit);

  public void transferredResourceAdded(TransferredResource resource);

  public void transferredResourceModified(TransferredResource resource);

  public void transferredResourceDeleted(TransferredResource resource);

}
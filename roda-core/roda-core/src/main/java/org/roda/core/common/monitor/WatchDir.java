package org.roda.core.common.monitor;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchDir implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(WatchDir.class);
  private final boolean recursive;
  private final Path watched;
  private ReindexSipRunnable reindexRunnable;
  private boolean watchInitialized;
  private Thread threadReindex;
  private ExecutorService executor;
  private Date indexDate;
  private SolrClient index;
  private List<FolderObserver> observers;

  public WatchDir(Path dir, boolean recursive, Date indexDate, SolrClient index, List<FolderObserver> observers)
    throws IOException {
    this.recursive = recursive;
    this.watched = dir;
    this.reindexRunnable = null;
    this.watchInitialized = false;
    this.indexDate = indexDate;
    this.index = index;
    this.observers = observers;
    this.executor = Executors.newSingleThreadExecutor();

  }

  public ReindexSipRunnable getReindexRunnable() {
    return reindexRunnable;
  }

  public void setReindexRunnable(ReindexSipRunnable reindexRunnable) {
    this.reindexRunnable = reindexRunnable;
  }

  @SuppressWarnings("unchegetInstancecked")
  <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  @Override
  public void run() {
    long startTime = System.currentTimeMillis();
    try {
      if (recursive) {
        MonitorVariables.getInstance().registerAll(watched);
      } else {
        MonitorVariables.getInstance().register(watched);
      }
    } catch (IOException e) {
      LOGGER.error("Error initialing watch thread: " + e.getMessage(), e);
    }
    LOGGER.debug("TIME ELAPSED (INITIALIZE WATCH): " + ((System.currentTimeMillis() - startTime) / 1000) + " segundos");

    watchInitialized = true;
    if (index != null) {
      reindexRunnable = new ReindexSipRunnable(watched, indexDate, index);
      threadReindex = new Thread(reindexRunnable, "ReindexThread");
      threadReindex.start();
    }

    processEvents();
  }

  void processEvents() {
    for (;;) {
      WatchKey key;
      try {
        key = MonitorVariables.getInstance().getWatcher().take();
      } catch (InterruptedException x) {
        return;
      }
      Path dir = MonitorVariables.getInstance().getKeys().get(key);
      if (dir == null) {
        continue;
      }
      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind<?> kind = event.kind();
        if (kind == OVERFLOW) {
          continue;
        }
        WatchEvent<Path> ev = cast(event);
        Path name = ev.context();
        Path child = dir.resolve(name);

        NotifierThread nt = new NotifierThread(observers, watched, child, kind, recursive);
        executor.execute(nt);

      }
      boolean valid = key.reset();
      if (!valid) {
        MonitorVariables.getInstance().getKeys().remove(key);
        if (MonitorVariables.getInstance().getKeys().isEmpty()) {
          break;
        }
      }
    }
  }

  public boolean isFullyInitialized() {
    return watchInitialized && threadReindex != null && !threadReindex.isAlive();
  }

  public void setObservers(List<FolderObserver> obs) {
    this.observers = obs;
  }
}
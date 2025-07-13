package org.crawler.service.worker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

public abstract class AbstractStoppableWorker implements StoppableWorker {
  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread worker;
  private final CountDownLatch stoppedLatch = new CountDownLatch(1);

  @Override
  public final void run() {
    running.set(true);
    worker = Thread.currentThread();

    try {
      while (running.get()) {
        try {
          doWork();
        } catch (Exception e) {
          if (isRunning()) {
            getLogger().error("Worker error", e);
          }
        }
      }
    } finally {
      getLogger().info("Worker shutting down");
      stoppedLatch.countDown();
    }
  }

  @Override
  public void interrupt() {
    running.set(false);
    if (worker != null) worker.interrupt();

    try {
      stoppedLatch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  protected abstract void doWork() throws Exception;

  protected abstract Logger getLogger();
}

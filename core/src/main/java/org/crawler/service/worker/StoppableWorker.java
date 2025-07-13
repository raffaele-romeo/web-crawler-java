package org.crawler.service.worker;

public interface StoppableWorker extends Runnable {
  void interrupt();

  boolean isRunning();
}

package org.crawler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.crawler.domain.Link;
import org.crawler.domain.URLPredicate;
import org.crawler.domain.URLPredicateImpl;
import org.crawler.infrastructure.*;
import org.crawler.service.WorkersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static final int REDIS_TIMEOUT = 1;
  private static final int MAX_DEPTH = 10;

  private static final Link seedLink = new Link("https://example.com", 0);

  public static void main(String[] args) {
    var numberOfCores = numberOfCores();
    var numberOfPageFetcherWorkers = numberOfCores * 2;
    var numberOfLinksExtractorWorker = numberOfCores * 2;

    ExecutorService executorService = null;
    JedisPool jedisPool = null;

    try {
      executorService = Executors.newVirtualThreadPerTaskExecutor();
      jedisPool = new JedisPool();

      WorkersManager workersManager =
          makeWorkersManager(
              executorService, jedisPool, numberOfPageFetcherWorkers, numberOfLinksExtractorWorker);

      registerShutdownHook(workersManager, executorService, jedisPool);
      workersManager.start();

      var vt =
          Thread.startVirtualThread(
              () -> {
                try {
                  Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  logger.error("Thread was interrupted");
                }
              });

      vt.join();
    } catch (Exception e) {
      logger.error("Fatal error during startup: {}", e.getMessage(), e);
    } finally {
      shutdown(executorService);

      if (jedisPool != null) {
        try {
          jedisPool.close();
        } catch (Exception e) {
          logger.error("Failed to close JedisPool: {}", e.getMessage(), e);
        }
      }
    }
  }

  private static WorkersManager makeWorkersManager(
      ExecutorService executorService,
      JedisPool jedisPool,
      int numberOfPageFetcherWorkers,
      int numberOfLinksExtractorWorker) {
    FrontierQueue frontierQueue = new FrontierQueueImpl(jedisPool, REDIS_TIMEOUT);
    FetchedPagesQueue fetchedPagesQueue = new FetchedPagesQueueImpl(jedisPool, REDIS_TIMEOUT);
    VisitedUrlsSet visitedUrlsSet = new VisitedUrlsSetImpl(jedisPool);

    URLPredicate urlPredicate = new URLPredicateImpl();

    var workersManger =
        new WorkersManager(
            frontierQueue,
            fetchedPagesQueue,
            visitedUrlsSet,
            executorService,
            urlPredicate,
            MAX_DEPTH,
            numberOfPageFetcherWorkers,
            numberOfLinksExtractorWorker);

    visitedUrlsSet.clear();
    frontierQueue.push(seedLink);

    return workersManger;
  }

  private static int numberOfCores() {
    return Runtime.getRuntime().availableProcessors();
  }

  private static void registerShutdownHook(
      WorkersManager workersManager, ExecutorService executorService, JedisPool jedisPool) {

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("Shutting down...");

                  workersManager.shutdown();

                  if (jedisPool != null) {
                    jedisPool.close();
                  }

                  shutdown(executorService);
                }));
  }

  private static void shutdown(ExecutorService executorService) {
    if (executorService != null) {
      executorService.shutdown();

      try {
        // Wait a while for existing tasks to terminate
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow(); // Cancel currently executing tasks
          // Wait a while for tasks to respond to being cancelled
          if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
            logger.error("Pool did not terminate");
        }
      } catch (InterruptedException ex) {
        // (Re-)Cancel if current thread also interrupted
        executorService.shutdownNow();
        // Preserve interrupt status
        Thread.currentThread().interrupt();
      }
    }
  }
}

package org.crawler;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.crawler.common.URLPredicate;
import org.crawler.common.URLPredicateImpl;
import org.crawler.config.ConfigLoader;
import org.crawler.config.ConfigLoaderImpl;
import org.crawler.domain.config.AppConfig;
import org.crawler.infrastructure.*;
import org.crawler.infrastructure.redis.FetchedPagesQueueImpl;
import org.crawler.infrastructure.redis.FrontierQueueImpl;
import org.crawler.infrastructure.redis.VisitedUrlsSetImpl;
import org.crawler.service.RobotsChecker;
import org.crawler.service.RobotsCheckerImpl;
import org.crawler.service.WorkersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    var numberOfCores = numberOfCores();
    var numberOfPageFetcherWorkers = numberOfCores * 5;
    var numberOfLinksExtractorWorker = numberOfCores * 5;

    ExecutorService executorService = null;
    JedisPool jedisPool = null;
    WorkersManager workersManager = null;

    try {
      executorService = Executors.newVirtualThreadPerTaskExecutor();
      JedisPoolConfig config = new JedisPoolConfig();
      config.setMaxTotal(200);
      config.setMaxIdle(50);
      config.setMinIdle(10);
      config.setBlockWhenExhausted(true);
      config.setMaxWait(Duration.ofSeconds(5));
      jedisPool = new JedisPool(config);

      ConfigLoader configLoader = new ConfigLoaderImpl();
      AppConfig appConfig = configLoader.load("config.properties");
      workersManager =
          makeWorkersManager(
              executorService,
              jedisPool,
              numberOfPageFetcherWorkers,
              numberOfLinksExtractorWorker,
                  appConfig);

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
      shutdown(workersManager, executorService, jedisPool);
    }
  }

  private static WorkersManager makeWorkersManager(
      ExecutorService executorService,
      JedisPool jedisPool,
      int numberOfPageFetcherWorkers,
      int numberOfLinksExtractorWorker,
      AppConfig config) {
    FrontierQueue frontierQueue = new FrontierQueueImpl(jedisPool, config.redis().timeout());
    FetchedPagesQueue fetchedPagesQueue =
        new FetchedPagesQueueImpl(jedisPool, config.redis().timeout());
    VisitedUrlsSet visitedUrlsSet = new VisitedUrlsSetImpl(jedisPool);
    RobotsChecker robotsChecker = new RobotsCheckerImpl();

    URLPredicate urlPredicate = new URLPredicateImpl();

    var workersManger =
        new WorkersManager(
            frontierQueue,
            fetchedPagesQueue,
            visitedUrlsSet,
            executorService,
            urlPredicate,
            robotsChecker,
            config.maxDepth(),
            numberOfPageFetcherWorkers,
            numberOfLinksExtractorWorker);

    visitedUrlsSet.clear();
    frontierQueue.clear();
    fetchedPagesQueue.clear();

    frontierQueue.push(config.seedLink());

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

                  shutdown(workersManager, executorService, jedisPool);
                }));
  }

  private static void shutdown(
      WorkersManager workersManager, ExecutorService executorService, JedisPool jedisPool) {
    if (workersManager != null) {
      workersManager.shutdown();
    }

    if (jedisPool != null) {
      try {
        jedisPool.close();
      } catch (Exception e) {
        logger.error("Failed to close JedisPool: {}", e.getMessage(), e);
      }
    }

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

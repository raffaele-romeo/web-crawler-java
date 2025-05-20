package org.crawler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.crawler.common.URLPredicates;
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

public class WebCrawlerRunner {
  private static final Logger logger = LoggerFactory.getLogger(WebCrawlerRunner.class);

  public void run(AppConfig appConfig) {
    var redisConfig = appConfig.redis();

    try (var executorService = Executors.newVirtualThreadPerTaskExecutor();
        var jedisPool =
            new JedisPool(redisConfig.jedisPoolConfig(), redisConfig.host(), redisConfig.port())) {

      WorkersManager workersManager = setupWorkers(executorService, jedisPool, appConfig);

      registerShutdownHook(workersManager, executorService, jedisPool);
      workersManager.start();

      Thread.currentThread().join();
    } catch (Exception e) {
      logger.error("Fatal error during startup: {}", e.getMessage(), e);
    }
  }

  private static WorkersManager setupWorkers(
      ExecutorService executorService, JedisPool jedisPool, AppConfig config) {
    var numberOfPageFetcherWorkers = config.numberOfPageFetcherWorkers();
    var numberOfLinksExtractorWorker = config.numberOfLinksExtractorWorker();

    FrontierQueue frontierQueue = new FrontierQueueImpl(jedisPool, config.redis().timeout());
    FetchedPagesQueue fetchedPagesQueue =
        new FetchedPagesQueueImpl(jedisPool, config.redis().timeout());
    VisitedUrlsSet visitedUrlsSet = new VisitedUrlsSetImpl(jedisPool);
    RobotsChecker robotsChecker = new RobotsCheckerImpl();

    var workersManger =
        new WorkersManager(
            frontierQueue,
            fetchedPagesQueue,
            visitedUrlsSet,
            executorService,
            URLPredicates.defaultValidator(),
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

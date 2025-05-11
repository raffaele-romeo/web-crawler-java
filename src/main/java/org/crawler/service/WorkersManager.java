package org.crawler.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.crawler.domain.URLPredicate;
import org.crawler.infrastructure.FetchedPagesQueue;
import org.crawler.infrastructure.FrontierQueue;
import org.crawler.infrastructure.VisitedUrlsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkersManager {
  private static final Logger logger = LoggerFactory.getLogger(WorkersManager.class);

  private final List<PageFetcherWorker> pageFetcherWorkers = new ArrayList<>();
  private final List<LinksExtractorWorker> linksExtractorWorkers = new ArrayList<>();

  private final ExecutorService executorService;
  private final FrontierQueue frontierQueue;
  private final FetchedPagesQueue fetchedPagesQueue;
  private final VisitedUrlsSet visitedUrlsSet;
  private final URLPredicate urlPredicate;
  private final int maxDepth;
  private final int numberOfPageFetcherWorkers;
  private final int numberOfLinksExtractorWorker;

  public WorkersManager(
      FrontierQueue frontierQueue,
      FetchedPagesQueue fetchedPagesQueue,
      VisitedUrlsSet visitedUrlsSet,
      ExecutorService executorService,
      URLPredicate urlPredicate,
      int maxDepth,
      int numberOfPageFetcherWorkers,
      int numberOfLinksExtractorWorker) {
    this.executorService = executorService;
    this.frontierQueue = frontierQueue;
    this.fetchedPagesQueue = fetchedPagesQueue;
    this.visitedUrlsSet = visitedUrlsSet;
    this.urlPredicate = urlPredicate;
    this.maxDepth = maxDepth;
    this.numberOfLinksExtractorWorker = numberOfLinksExtractorWorker;
    this.numberOfPageFetcherWorkers = numberOfPageFetcherWorkers;
  }

  public void start() {
    for (int i = 0; i < numberOfPageFetcherWorkers; i++) {
      PageFetcherWorker worker =
          new PageFetcherWorker(frontierQueue, fetchedPagesQueue, visitedUrlsSet);
      pageFetcherWorkers.add(worker);
      executorService.execute(worker);
    }

    logger.info("Started {} Page Fetcher workers", numberOfPageFetcherWorkers);

    // Create and execute links extractor workers
    for (int i = 0; i < numberOfLinksExtractorWorker; i++) {
      LinksExtractorWorker worker =
          new LinksExtractorWorker(frontierQueue, fetchedPagesQueue, urlPredicate, maxDepth);
      linksExtractorWorkers.add(worker);
      executorService.execute(worker);
    }

    logger.info("Started {} Link Extractors workers", numberOfLinksExtractorWorker);
  }

  public void shutdown() {
    logger.info("Shutting down Page Fetcher workers...");

    for (PageFetcherWorker worker : pageFetcherWorkers) {
      worker.interrupt();
    }

    logger.info("Shutting down Link Extractors workers...");

    for (LinksExtractorWorker worker : linksExtractorWorkers) {
      worker.interrupt();
    }
  }
}

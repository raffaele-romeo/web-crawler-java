package org.crawler.service.worker;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.crawler.domain.Link;
import org.crawler.domain.Page;
import org.crawler.domain.exception.ConnectionException;
import org.crawler.infrastructure.FetchedPagesQueue;
import org.crawler.infrastructure.FrontierQueue;
import org.crawler.infrastructure.VisitedUrlsSet;
import org.crawler.service.RobotsChecker;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageFetcherWorker implements PageFetcher, Runnable {
  private static final Logger logger = LoggerFactory.getLogger(PageFetcherWorker.class);

  private final FrontierQueue frontierQueue;
  private final FetchedPagesQueue fetchedPagesQueue;
  private final VisitedUrlsSet visitedUrlsSet;

  private final RobotsChecker robotsChecker;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread worker;

  public PageFetcherWorker(
      FrontierQueue frontierQueue,
      FetchedPagesQueue fetchedPagesQueue,
      VisitedUrlsSet visitedUrlsSet,
      RobotsChecker robotsChecker) {
    this.frontierQueue = frontierQueue;
    this.fetchedPagesQueue = fetchedPagesQueue;
    this.visitedUrlsSet = visitedUrlsSet;
    this.robotsChecker = robotsChecker;
  }

  @Override
  public Page fetchPage(Link link) {
    Objects.requireNonNull(link);

    String html;

    try {
      html = Jsoup.connect(link.uri().toString()).get().html();

    } catch (Exception e) {
      logger.error("Error while fetching page: {}", link, e);
      throw new ConnectionException("Failed to connect to URL: " + link, e);
    }

    return new Page(link, html);
  }

  @Override
  public void run() {
    running.set(true);

    worker = Thread.currentThread();
    Link link;

    while (running.get()) {
      try {
        var maybeElem = frontierQueue.pop();

        if (maybeElem.isPresent()) {
          link = maybeElem.get();

          process(link);
        } else {
          // To avoid CPU spinning when queue is empty
          Thread.sleep(100);
        }
      } catch (Exception e) {
        if (isRunning()) {
          logger.error("Error retrieving from queue", e);
        }
      }
    }
  }

  protected void process(Link link) {
    try {
      boolean wasAdded = visitedUrlsSet.addIfNotPresent(link.uri().toString());

      if (wasAdded && robotsChecker.isUrlAllowed(link.uri())) {
        logger.debug("Processing link {}", link);

        Page page = fetchPage(link);
        fetchedPagesQueue.push(page);
      }
    } catch (Exception e) {
      logger.error("Failed to fetch page from {}", link, e);
    }
  }

  public void interrupt() {
    running.set(false);

    // break thread pool out of frontierQueue.pop() call.
    worker.interrupt();
  }

  boolean isRunning() {
    return running.get();
  }
}

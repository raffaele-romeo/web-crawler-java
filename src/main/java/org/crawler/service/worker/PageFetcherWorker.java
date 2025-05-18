package org.crawler.service.worker;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.crawler.domain.Link;
import org.crawler.domain.Page;
import org.crawler.domain.exception.ConnectionException;
import org.crawler.infrastructure.FetchedPagesQueue;
import org.crawler.infrastructure.FrontierQueue;
import org.crawler.infrastructure.VisitedUrlsSet;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageFetcherWorker implements PageFetcher, Runnable {
  private static final Logger logger = LoggerFactory.getLogger(PageFetcherWorker.class);

  private final FrontierQueue frontierQueue;
  private final FetchedPagesQueue fetchedPagesQueue;
  private final VisitedUrlsSet visitedUrlsSet;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread worker;

  public PageFetcherWorker(
      FrontierQueue frontierQueue,
      FetchedPagesQueue fetchedPagesQueue,
      VisitedUrlsSet visitedUrlsSet) {
    this.frontierQueue = frontierQueue;
    this.fetchedPagesQueue = fetchedPagesQueue;
    this.visitedUrlsSet = visitedUrlsSet;
  }

  @Override
  public Page fetchPage(Link link) {
    Objects.requireNonNull(link);

    String html;

    try {
      html = Jsoup.connect(link.url()).get().html();

    } catch (IOException e) {
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

          try {
            boolean wasAdded = visitedUrlsSet.addIfNotPresent(link.url());

            if (wasAdded) {
              logger.debug("Thread {} - Processing link {}", worker.getName(), link);

              Page page = fetchPage(link);
              fetchedPagesQueue.push(page);
            }
          } catch (Exception e) {
            logger.error("Thread {} - Failed to fetch page from {}", worker.getName(), link, e);
          }
        } else {
          Thread.sleep(100);
        }
      } catch (Exception e) {
        if (isRunning()) {
          logger.error("Thread {} - Error retrieving from queue", worker.getName(), e);
        }
      }
    }
  }

  public void interrupt() {
    running.set(false);

    // break pool thread out of pageQueue.get() call.
    worker.interrupt();
  }

  boolean isRunning() {
    return running.get();
  }
}

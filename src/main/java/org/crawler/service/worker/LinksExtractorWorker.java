package org.crawler.service.worker;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.crawler.common.URLPredicate;
import org.crawler.domain.Link;
import org.crawler.domain.Page;
import org.crawler.domain.exception.ParserException;
import org.crawler.infrastructure.FetchedPagesQueue;
import org.crawler.infrastructure.FrontierQueue;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinksExtractorWorker implements LinkExtractors, Runnable {
  private static final Logger logger = LoggerFactory.getLogger(LinksExtractorWorker.class);

  private final FrontierQueue frontierQueue;
  private final FetchedPagesQueue fetchedPagesQueue;
  private final URLPredicate urlPredicate;
  private final int maxDepth;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread worker;

  public LinksExtractorWorker(
      FrontierQueue frontierQueue,
      FetchedPagesQueue fetchedPagesQueue,
      URLPredicate urlPredicate,
      int maxDepth) {
    this.frontierQueue = frontierQueue;
    this.fetchedPagesQueue = fetchedPagesQueue;
    this.urlPredicate = urlPredicate;
    this.maxDepth = maxDepth;
  }

  @Override
  public Set<Link> extractLinks(Page page) {
    Set<Link> links;

    try {
      if (page.link().depth() >= maxDepth) {
        links = Set.of();
      } else {
        links =
            Jsoup.parse(page.html(), page.link().url()).select("a[href]").stream()
                .map(elem -> elem.attr("abs:href"))
                .filter(url -> !url.isBlank())
                .filter(urlPredicate::isValid)
                .map(url -> new Link(url.trim().toLowerCase(), page.link().depth() + 1))
                .collect(Collectors.toSet());
      }
    } catch (Exception e) {
      logger.error("Error while parsing page from url {}", page.link(), e);
      throw new ParserException("Failed to extract links from url: " + page.link(), e);
    }

    return links;
  }

  @Override
  public void run() {
    running.set(true);

    worker = Thread.currentThread();
    Page page;
    Set<Link> links;

    while (running.get()) {
      try {
        var maybeElem = fetchedPagesQueue.pop();

        if (maybeElem.isPresent()) {
          page = maybeElem.get();

          logger.debug("Thread {} - Processing page {}", worker.getName(), page.link());

          try {
            links = extractLinks(page);

            logger.debug(
                "Thread {} - Extracted the following links {}",
                worker.getName(),
                linksToJson(links));

            links.forEach(frontierQueue::push);
          } catch (Exception e) {
            logger.error(
                "Thread {} - Failed to extract links from {}", worker.getName(), page.link());
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

  private static String linksToJson(Set<Link> links) {
    return links.stream().map(Link::toJson).collect(Collectors.joining(",", "[", "]"));
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

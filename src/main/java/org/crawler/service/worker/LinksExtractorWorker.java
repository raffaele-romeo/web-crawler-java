package org.crawler.service.worker;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.crawler.common.URLPredicate;
import org.crawler.domain.Link;
import org.crawler.domain.Page;
import org.crawler.domain.exception.ParserException;
import org.crawler.infrastructure.FetchedPagesQueue;
import org.crawler.infrastructure.FrontierQueue;
import org.crawler.infrastructure.VisitedUrlsSet;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinksExtractorWorker implements LinkExtractors, Runnable {
  private static final Logger logger = LoggerFactory.getLogger(LinksExtractorWorker.class);

  private final FrontierQueue frontierQueue;
  private final FetchedPagesQueue fetchedPagesQueue;
  private final URLPredicate urlPredicate;
  private final VisitedUrlsSet visitedUrlsSet;
  private final int maxDepth;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread worker;

  public LinksExtractorWorker(
      FrontierQueue frontierQueue,
      FetchedPagesQueue fetchedPagesQueue,
      VisitedUrlsSet visitedUrlsSet,
      URLPredicate urlPredicate,
      int maxDepth) {
    this.frontierQueue = frontierQueue;
    this.fetchedPagesQueue = fetchedPagesQueue;
    this.urlPredicate = urlPredicate;
    this.visitedUrlsSet = visitedUrlsSet;
    this.maxDepth = maxDepth;
  }

  @Override
  public Set<Link> extractLinks(Page page) {
    Set<Link> links;

    try {
      if (page.link().depth() >= maxDepth) {
        links = Set.of();
      } else {
        String baseUrl = page.link().url().toLowerCase().trim();

        links =
            Jsoup.parse(page.html(), baseUrl).select("a[href]").stream()
                .map(elem -> elem.attr("abs:href"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(url -> !url.isBlank())
                .filter(url -> !url.equals(baseUrl))
                .filter(url -> !url.startsWith("javascript:"))
                .filter(url -> !url.startsWith("mailto:"))
                .filter(url -> !url.contains("#"))
                .filter(url -> !isBinaryResource(url))
                .filter(urlPredicate::isValid)
                .filter(url -> !visitedUrlsSet.isPresent(url))
                .map(url -> new Link(sanitizeUrl(url), page.link().depth() + 1))
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

    while (running.get()) {
      try {
        var maybeElem = fetchedPagesQueue.pop();

        if (maybeElem.isPresent()) {
          page = maybeElem.get();

          logger.debug("Processing page {}", page.link());

          process(page);
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

  protected void process(Page page) {
    Set<Link> links;

    try {
      links = extractLinks(page);

      logger.debug("Extracted the following links {}", linksToJson(links));

      links.forEach(frontierQueue::push);
    } catch (Exception e) {
      logger.error("Failed to extract links from {}", page.link());
    }
  }

  private static boolean isBinaryResource(String url) {
    return url.matches(".*\\.(pdf|zip|rar|tar|gz|exe|docx?|xlsx?|pptx?)$");
  }

  private static String sanitizeUrl(String url) {
    try {
      URI uri = new URI(url);
      URI sanitized = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null);
      return sanitized.toString().replaceAll("/$", ""); // remove trailing slash
    } catch (Exception e) {
      return url;
    }
  }

  private static String linksToJson(Set<Link> links) {
    return links.stream().map(Link::toJson).collect(Collectors.joining(",", "[", "]"));
  }

  public void interrupt() {
    running.set(false);

    // break thread pool out of fetchedPagesQueue.pop() call.
    worker.interrupt();
  }

  boolean isRunning() {
    return running.get();
  }
}

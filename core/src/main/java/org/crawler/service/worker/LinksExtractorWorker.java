package org.crawler.service.worker;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
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

public class LinksExtractorWorker extends AbstractStoppableWorker implements LinkExtractors {
  private static final Logger logger = LoggerFactory.getLogger(LinksExtractorWorker.class);

  private final FrontierQueue frontierQueue;
  private final FetchedPagesQueue fetchedPagesQueue;
  private final URLPredicate urlPredicate;
  private final VisitedUrlsSet visitedUrlsSet;
  private final int maxDepth;

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
        String baseUrl = page.link().uri().toString().toLowerCase().trim();

        links =
            Jsoup.parse(page.html(), baseUrl).select("a[href]").stream()
                .map(elem -> elem.attr("abs:href"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(url -> !url.equals(baseUrl))
                .filter(urlPredicate::isValid)
                .filter(url -> !visitedUrlsSet.isPresent(url))
                .map(LinksExtractorWorker::sanitizeUrl)
                .flatMap(Optional::stream)
                .map(uri -> new Link(uri, page.link().depth() + 1))
                .collect(Collectors.toSet());
      }
    } catch (Exception e) {
      throw new ParserException("Failed to extract links from url: " + page.link(), e);
    }

    return links;
  }

  @Override
  protected void doWork() throws Exception {
    var maybeElem = fetchedPagesQueue.pop();

    if (maybeElem.isPresent()) {
      Page page = maybeElem.get();

      logger.debug("Processing page {}", page.link());

      process(page);
    } else {
      // To avoid CPU spinning when queue is empty
      Thread.onSpinWait();
    }
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  protected void process(Page page) {
    Set<Link> links;

    try {
      links = extractLinks(page);

      logger.debug("Extracted the following links {}", linksToJson(links));

      links.forEach(frontierQueue::push);
    } catch (Exception e) {
      logger.error("Failed to extract links from {}", page.link(), e);
    }
  }

  private static Optional<URI> sanitizeUrl(String url) {
    try {
      var uri = URI.create(url);
      return Optional.of(new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private static String linksToJson(Set<Link> links) {
    return links.stream().map(Link::toJson).collect(Collectors.joining(",", "[", "]"));
  }
}

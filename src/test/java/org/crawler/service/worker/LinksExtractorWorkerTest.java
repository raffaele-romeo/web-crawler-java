package org.crawler.service.worker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;
import java.util.stream.Collectors;
import org.crawler.common.URLPredicate;
import org.crawler.domain.Link;
import org.crawler.domain.Page;
import org.crawler.infrastructure.FetchedPagesQueue;
import org.crawler.infrastructure.FrontierQueue;
import org.crawler.infrastructure.VisitedUrlsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinksExtractorWorkerTest {

  @Mock private FrontierQueue frontierQueue;
  @Mock private FetchedPagesQueue fetchedPagesQueue;
  @Mock private VisitedUrlsSet visitedUrlsSet;
  @Mock private URLPredicate urlPredicate;

  private LinksExtractorWorker linksExtractorWorker;
  private final int maxDepth = 2;

  @BeforeEach
  void setUp() {
    linksExtractorWorker =
        new LinksExtractorWorker(
            frontierQueue, fetchedPagesQueue, visitedUrlsSet, urlPredicate, maxDepth);
  }

  @Test
  void extractLinks_shouldExtractValidLinks() {
    // Given
    Link link = new Link("https://example.com", 0);
    String html =
        """
                <html><body>
                <a href='https://example.com/page1'>Page 1</a>
                <a href='https://example.com/page2'>Page 2</a>
                <a href='https://other.com/page3'>Page 3</a>
                <a href='https://other.com/page3.pdf'>Page 3</a>
                <a href=''>Empty Link</a>
                </body></html>""";

    Page page = new Page(link, html);

    when(urlPredicate.isValid(anyString())).thenReturn(true);
    when(visitedUrlsSet.isPresent(anyString())).thenReturn(false);

    // When
    Set<Link> extractedLinks = linksExtractorWorker.extractLinks(page);

    // Then
    var expectedUrls =
        Set.of("https://example.com/page1", "https://example.com/page2", "https://other.com/page3");

    assertEquals(3, extractedLinks.size());
    assertTrue(
        extractedLinks.stream()
            .map(Link::url)
            .collect(Collectors.toSet())
            .containsAll(expectedUrls));
    extractedLinks.forEach(l -> assertEquals(1, l.depth()));
  }

  @Test
  void extractLinks_shouldRespectMaxDepth() {
    // Given
    Link link = new Link("https://example.com", maxDepth);
    String html =
        """
            <html><body>
            <a href='https://example.com/page1'>Page 1</a>
            </body></html>
            """;
    Page page = new Page(link, html);

    // When
    Set<Link> extractedLinks = linksExtractorWorker.extractLinks(page);

    // Then
    assertTrue(extractedLinks.isEmpty());
  }

  @Test
  void extractLinks_shouldFilterVisitedUrls() {
    // Given
    Link link = new Link("https://example.com", 0);
    String html =
        """
                    <html><body>
                    <a href='https://example.com/page1'>Page 1</a>
                    <a href='https://example.com/page2'>Page 2</a>
                    </body></html>""";

    Page page = new Page(link, html);

    when(urlPredicate.isValid(anyString())).thenReturn(true);
    when(visitedUrlsSet.isPresent("https://example.com/page1")).thenReturn(true);
    when(visitedUrlsSet.isPresent("https://example.com/page2")).thenReturn(false);

    // When
    Set<Link> extractedLinks = linksExtractorWorker.extractLinks(page);

    // Then
    assertEquals(1, extractedLinks.size());
    assertTrue(extractedLinks.stream().allMatch(x -> x.url().equals("https://example.com/page2")));
  }

  @Test
  void extractLinks_shouldFilterInvalidUrls() {
    // Given
    Link link = new Link("https://example.com", 0);
    String html =
        """
                    <html><body>
                    <a href='https://example.com/page1'>Page 1</a>
                    <a href='https://example.com/page2'>Page 2</a>
                    </body></html>""";

    Page page = new Page(link, html);

    when(urlPredicate.isValid("https://example.com/page1")).thenReturn(true);
    when(urlPredicate.isValid("https://example.com/page2")).thenReturn(false);
    when(visitedUrlsSet.isPresent(anyString())).thenReturn(false);

    // When
    Set<Link> extractedLinks = linksExtractorWorker.extractLinks(page);

    // Then
    assertEquals(1, extractedLinks.size());
    assertTrue(extractedLinks.stream().allMatch(x -> x.url().equals("https://example.com/page1")));
  }

  @Test
  void process_shouldAddLinksToFrontierQueue() {
    // Given
    Link link = new Link("https://example.com", 0);
    String html =
        """
                <html><body>
                 <a href='https://example.com/page1'>Page 1</a>
                <a href='https://example.com/page2'>Page 2</a>
                <a href='https://other.com/page3'>Page 3</a>
                </body></html>
                """;
    Page page = new Page(link, html);

    when(urlPredicate.isValid(anyString())).thenReturn(true);
    when(visitedUrlsSet.isPresent(anyString())).thenReturn(false);

    // When
    linksExtractorWorker.process(page);

    // Then
    verify(frontierQueue).push(new Link("https://example.com/page1", 1));
    verify(frontierQueue).push(new Link("https://example.com/page2", 1));
    verify(frontierQueue).push(new Link("https://other.com/page3", 1));
  }
}

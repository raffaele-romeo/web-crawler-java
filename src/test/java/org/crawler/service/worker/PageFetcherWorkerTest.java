package org.crawler.service.worker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import org.crawler.domain.Link;
import org.crawler.domain.Page;
import org.crawler.domain.exception.ConnectionException;
import org.crawler.infrastructure.FetchedPagesQueue;
import org.crawler.infrastructure.FrontierQueue;
import org.crawler.infrastructure.VisitedUrlsSet;
import org.crawler.service.RobotsChecker;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PageFetcherWorkerTest {

  @Mock private FrontierQueue frontierQueue;
  @Mock private FetchedPagesQueue fetchedPagesQueue;
  @Mock private VisitedUrlsSet visitedUrlsSet;
  @Mock private RobotsChecker robotsChecker;

  private PageFetcherWorker pageFetcherWorker;

  @BeforeEach
  void setUp() {
    pageFetcherWorker =
        new PageFetcherWorker(frontierQueue, fetchedPagesQueue, visitedUrlsSet, robotsChecker);
  }

  @Test
  void fetchPage_shouldReturnPage_whenHtmlFetchedSuccessfully() {
    // Given
    String url = "https://example.com";
    Link link = new Link(url, 0);

    // When
    Page page = pageFetcherWorker.fetchPage(link);

    // Then
    assertNotNull(page);
    assertEquals(url, page.link().url());
    assertTrue(page.html().contains("html"));
  }

  @Test
  void fetchPage_shouldThrowConnectionException_whenJsoupFails() {
    // Given
    String url = "https://invalid.example.com";
    Link link = new Link(url, 0);

    try (var jsoupMocked = mockStatic(Jsoup.class)) {
      jsoupMocked.when(() -> Jsoup.connect(url)).thenThrow(new RuntimeException("Timeout"));

      // Then
      assertThrows(ConnectionException.class, () -> pageFetcherWorker.fetchPage(link));
    }
  }

  @Test
  void process_shouldPushToFetchedQueue_whenLinkNotVisitedAndAllowed() throws IOException {
    // Given
    String url = "https://example.com";
    Link link = new Link(url, 0);
    String html = "<html>test</html>";

    Connection mockConnection = mock(Connection.class);
    Document mockDocument = mock(Document.class);

    when(visitedUrlsSet.addIfNotPresent(url)).thenReturn(true);
    when(robotsChecker.isUrlAllowed(url)).thenReturn(true);

    try (var jsoupMocked = mockStatic(Jsoup.class)) {
      jsoupMocked.when(() -> Jsoup.connect(url)).thenReturn(mockConnection);
      when(mockConnection.get()).thenReturn(mockDocument);
      when(mockDocument.html()).thenReturn(html);

      // When
      pageFetcherWorker.process(link);

      // Then
      verify(fetchedPagesQueue, times(1)).push(any(Page.class));
    }
  }

  @Test
  void process_shouldDoNothing_whenAlreadyVisited() {
    // Given
    Link link = new Link("https://example.com", 0);
    when(visitedUrlsSet.addIfNotPresent(link.url())).thenReturn(false);

    // When
    pageFetcherWorker.process(link);

    // Then
    verifyNoInteractions(fetchedPagesQueue);
  }

  @Test
  void process_shouldDoNothing_whenDisallowedByRobots() {
    // Given
    String url = "https://example.com";
    Link link = new Link(url, 0);
    when(visitedUrlsSet.addIfNotPresent(url)).thenReturn(true);
    when(robotsChecker.isUrlAllowed(url)).thenReturn(false);

    // When
    pageFetcherWorker.process(link);

    // Then
    verifyNoInteractions(fetchedPagesQueue);
  }
}

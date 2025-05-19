package org.crawler.service.worker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import org.crawler.domain.Link;
import org.crawler.domain.Page;
import org.crawler.domain.exception.ConnectionException;
import org.crawler.fixture.LinkFixture;
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
    Link link = LinkFixture.sampleLink();

    // When
    Page page = pageFetcherWorker.fetchPage(link);

    // Then
    assertNotNull(page);
    assertEquals(link.uri(), page.link().uri());
    assertTrue(page.html().contains("html"));
  }

  @Test
  void fetchPage_shouldThrowConnectionException_whenJsoupFails() {
    // Given
    Link link = LinkFixture.sampleLink();

    try (var jsoupMocked = mockStatic(Jsoup.class)) {
      jsoupMocked
          .when(() -> Jsoup.connect(link.uri().toString()))
          .thenThrow(new RuntimeException("Timeout"));

      // Then
      assertThrows(ConnectionException.class, () -> pageFetcherWorker.fetchPage(link));
    }
  }

  @Test
  void process_shouldPushToFetchedQueue_whenLinkNotVisitedAndAllowed() throws IOException {
    // Given
    Link link = LinkFixture.sampleLink();
    String html = "<html>test</html>";

    Connection mockConnection = mock(Connection.class);
    Document mockDocument = mock(Document.class);

    when(visitedUrlsSet.addIfNotPresent(link.uri().toString())).thenReturn(true);
    when(robotsChecker.isUrlAllowed(link.uri())).thenReturn(true);

    try (var jsoupMocked = mockStatic(Jsoup.class)) {
      jsoupMocked.when(() -> Jsoup.connect(link.uri().toString())).thenReturn(mockConnection);
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
    Link link = LinkFixture.sampleLink();
    when(visitedUrlsSet.addIfNotPresent(link.uri().toString())).thenReturn(false);

    // When
    pageFetcherWorker.process(link);

    // Then
    verifyNoInteractions(fetchedPagesQueue);
  }

  @Test
  void process_shouldDoNothing_whenDisallowedByRobots() {
    // Given
    Link link = LinkFixture.sampleLink();
    when(visitedUrlsSet.addIfNotPresent(link.uri().toString())).thenReturn(true);
    when(robotsChecker.isUrlAllowed(link.uri())).thenReturn(false);

    // When
    pageFetcherWorker.process(link);

    // Then
    verifyNoInteractions(fetchedPagesQueue);
  }
}

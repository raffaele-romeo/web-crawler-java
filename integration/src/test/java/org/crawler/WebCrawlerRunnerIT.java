package org.crawler;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.net.URI;
import org.crawler.domain.Link;
import org.crawler.domain.config.AppConfig;
import org.crawler.domain.config.RedisConfig;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPoolConfig;

public class WebCrawlerRunnerIT extends AbstractIntegrationTest {

  @Test
  public void testWebCrawlerRunnerRunsSuccessfully() throws Exception {
    WebCrawlerRunner runner = new WebCrawlerRunner();
    var redisConfig = new RedisConfig(0, redisHost(), redisPort(), new JedisPoolConfig());
    var appConfig =
        new AppConfig(
            new Link(URI.create(String.format("%s/seed", wireMockBaseUrl())), 0),
            2,
            2,
            2,
            redisConfig);

    var seedResponse =
        String.format(
            """
              <html><body>
                      <a href='%1$s/page1'>Page 1</a>
                      <a href='%1$s/page2'>Page 2</a>
                      <a href='%1$s/page3'>Page 3</a>
                  </body></html>
            """,
            wireMockBaseUrl());

    var emptyResponse = "<html><body>No more links</body></html>";
    stubSimpleHtmlPage("/seed", seedResponse);
    stubSimpleHtmlPage("/page1", emptyResponse);
    stubSimpleHtmlPage("/page2", emptyResponse);
    stubSimpleHtmlPage("/page3", emptyResponse);

    Thread thread = new Thread(() -> runner.run(appConfig));
    thread.start();

    Thread.sleep(1000);

    WIRE_MOCK_SERVER.verify(getRequestedFor(urlEqualTo("/seed")));
    WIRE_MOCK_SERVER.verify(getRequestedFor(urlEqualTo("/page1")));
    WIRE_MOCK_SERVER.verify(getRequestedFor(urlEqualTo("/page2")));
    WIRE_MOCK_SERVER.verify(getRequestedFor(urlEqualTo("/page3")));
  }

  private void stubSimpleHtmlPage(String path, String bodyContent) {
    WIRE_MOCK_SERVER.stubFor(
        get(urlEqualTo(path))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/html")
                    .withBody(bodyContent)));
  }
}

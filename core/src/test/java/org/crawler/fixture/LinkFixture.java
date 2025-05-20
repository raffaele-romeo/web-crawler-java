package org.crawler.fixture;

import java.net.URI;
import org.crawler.domain.Link;

public class LinkFixture {
  public static Link sampleLink() {
    return new Link(URI.create("https://example.com"), 0);
  }

  public static Link deepLink(int depth) {
    return new Link(URI.create("https://example.com/deep"), depth);
  }
}

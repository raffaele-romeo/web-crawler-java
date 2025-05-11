package org.crawler.service;

import org.crawler.domain.Link;
import org.crawler.domain.Page;

public interface PageFetcher {
  Page fetchPage(Link link);
}

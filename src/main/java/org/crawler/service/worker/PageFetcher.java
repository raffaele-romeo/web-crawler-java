package org.crawler.service.worker;

import org.crawler.domain.Link;
import org.crawler.domain.Page;

public interface PageFetcher {
  Page fetchPage(Link link);
}

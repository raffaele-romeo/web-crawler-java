package org.crawler.service.worker;

import java.util.Set;
import org.crawler.domain.Link;
import org.crawler.domain.Page;

public interface LinkExtractors {
  Set<Link> extractLinks(Page page);
}

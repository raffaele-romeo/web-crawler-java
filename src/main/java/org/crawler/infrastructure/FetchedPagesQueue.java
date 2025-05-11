package org.crawler.infrastructure;

import java.util.Optional;
import org.crawler.domain.Page;

public interface FetchedPagesQueue {
  Optional<Page> pop();

  void push(Page page);
}

package org.crawler.infrastructure;

import java.util.Optional;
import org.crawler.domain.Link;

public interface FrontierQueue {
  Optional<Link> pop();

  void push(Link link);
}

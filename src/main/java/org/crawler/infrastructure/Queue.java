package org.crawler.infrastructure;

import java.util.Optional;

public interface Queue<T> {
  Optional<T> pop();

  void push(T t);
}

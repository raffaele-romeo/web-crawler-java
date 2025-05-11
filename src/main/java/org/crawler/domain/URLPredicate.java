package org.crawler.domain;

@FunctionalInterface
public interface URLPredicate {
  boolean isValid(String url);
}

package org.crawler.common;

@FunctionalInterface
public interface URLPredicate {
  boolean isValid(String url);
}

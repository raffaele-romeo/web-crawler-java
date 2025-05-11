package org.crawler.infrastructure;

public interface VisitedUrlsSet {
  void clear();

  boolean addIfNotPresent(String url);
}

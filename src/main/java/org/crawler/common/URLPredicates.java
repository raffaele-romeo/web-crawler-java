package org.crawler.common;

public class URLPredicates {
  public static URLPredicate defaultValidator() {
    return url ->
        !url.isBlank()
            && !url.startsWith("javascript:")
            && !url.startsWith("mailto:")
            && !url.contains("#");
  }
}

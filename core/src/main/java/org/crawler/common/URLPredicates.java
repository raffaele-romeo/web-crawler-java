package org.crawler.common;

public class URLPredicates {
  public static URLPredicate defaultValidator() {
    return url ->
        !url.isBlank()
            && !url.startsWith("javascript:")
            && !url.startsWith("mailto:")
            && !url.contains("#")
            && !isBinaryResource(url);
  }

  private static boolean isBinaryResource(String url) {
    return url.matches(".*\\.(pdf|zip|rar|tar|gz|exe|docx?|xlsx?|pptx?|txt)$");
  }
}

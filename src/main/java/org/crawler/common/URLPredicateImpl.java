package org.crawler.common;

import java.net.URI;
import java.net.URISyntaxException;

public class URLPredicateImpl implements URLPredicate {
  @Override
  public boolean isValid(String url) {
    try {
      new URI(url);

      return true;
    } catch (URISyntaxException e) {
      return false;
    }
  }
}

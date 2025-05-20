package org.crawler.domain.exception;

public class ConnectionException extends WebCrawlerException {
  public ConnectionException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConnectionException(String message) {
    super(message);
  }
}

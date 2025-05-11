package org.crawler.domain.exception;

public class ParserException extends WebCrawlerException {
  public ParserException(String message, Throwable cause) {
    super(message, cause);
  }

  public ParserException(String message) {
    super(message);
  }
}

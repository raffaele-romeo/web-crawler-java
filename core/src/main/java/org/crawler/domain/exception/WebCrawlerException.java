package org.crawler.domain.exception;

public abstract class WebCrawlerException extends RuntimeException {
  public WebCrawlerException() {
    super();
  }

  public WebCrawlerException(String message) {
    super(message);
  }

  public WebCrawlerException(String message, Throwable throwable) {
    super(message, throwable);
  }

  public WebCrawlerException(Throwable throwable) {
    super(throwable);
  }
}

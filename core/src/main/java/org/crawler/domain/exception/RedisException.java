package org.crawler.domain.exception;

public class RedisException extends RuntimeException {
  public RedisException(String message) {
    super(message);
  }

  public RedisException(String message, Throwable cause) {
    super(message, cause);
  }
}

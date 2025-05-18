package org.crawler.infrastructure.redis;

import org.crawler.domain.exception.RedisException;
import org.crawler.infrastructure.VisitedUrlsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class VisitedUrlsSetImpl implements VisitedUrlsSet {
  private static final Logger logger = LoggerFactory.getLogger(VisitedUrlsSetImpl.class);

  private static final String VISITED_URLS_KEY = "set#visited_urls";
  private final JedisPool jedisPool;

  public VisitedUrlsSetImpl(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  @Override
  public void clear() {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(VISITED_URLS_KEY);
    } catch (Exception e) {
      var message = "Failed to clear visited set";

      logger.error(message, e);
      throw new RedisException(message, e);
    }
  }

  @Override
  public boolean addIfNotPresent(String url) {
    try (Jedis jedis = jedisPool.getResource()) {
      long result = jedis.sadd(VISITED_URLS_KEY, url);

      logger.info("Added link to visited urls, result: {}", result > 0);
      return result > 0;
    } catch (Exception e) {
      var message = "Failed to add url to visited set";

      logger.error(message, e);
      throw new RedisException(message, e);
    }
  }

  @Override
  public boolean isPresent(String url) {
    try (Jedis jedis = jedisPool.getResource()) {

      return jedis.sismember(VISITED_URLS_KEY, url);
    } catch (Exception e) {
      var message = "Failed to add url to visited set";

      logger.error(message, e);
      throw new RedisException(message, e);
    }
  }
}

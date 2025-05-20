package org.crawler.infrastructure.redis;

import java.util.Optional;
import org.crawler.domain.Page;
import org.crawler.domain.exception.RedisException;
import org.crawler.infrastructure.FetchedPagesQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class FetchedPagesQueueImpl implements FetchedPagesQueue {
  private static final Logger logger = LoggerFactory.getLogger(FetchedPagesQueueImpl.class);

  private static final String PARSING_QUEUE_KEY = "queue#parsing";
  private final JedisPool jedisPool;
  private final int timeout;

  public FetchedPagesQueueImpl(JedisPool jedisPool, int timeout) {
    this.jedisPool = jedisPool;
    this.timeout = timeout;
  }

  @Override
  public Optional<Page> pop() {
    try (Jedis jedis = jedisPool.getResource()) {
      var result = jedis.blpop(timeout, PARSING_QUEUE_KEY);

      if (result != null && result.size() > 1) {
        return Optional.of(Page.fromJson(result.get(1)));
      } else {
        return Optional.empty();
      }
    } catch (Exception e) {
      var message = "Failed to get page from queue";

      logger.error(message, e);
      throw new RedisException(message, e);
    }
  }

  @Override
  public void push(Page page) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.lpush(PARSING_QUEUE_KEY, page.toJson());
    } catch (Exception e) {
      var message = "Failed to add link to queue";
      logger.error(message, e);
      throw new RedisException(message, e);
    }
  }

  @Override
  public void clear() {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(PARSING_QUEUE_KEY);
    } catch (Exception e) {
      var message = "Failed to clear parsing queue";

      logger.error(message, e);
      throw new RedisException(message, e);
    }
  }
}

package org.crawler.infrastructure;

import java.util.Optional;
import org.crawler.domain.Link;
import org.crawler.domain.exception.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class FrontierQueueImpl implements FrontierQueue {
  private static final Logger logger = LoggerFactory.getLogger(FrontierQueueImpl.class);

  private static final String FRONTIER_QUEUE_KEY = "queue#frontier";
  private final JedisPool jedisPool;
  private final int timeout;

  public FrontierQueueImpl(JedisPool jedisPool, int timeout) {
    this.jedisPool = jedisPool;
    this.timeout = timeout;
  }

  @Override
  public Optional<Link> pop() {
    try (Jedis jedis = jedisPool.getResource()) {
      var result = jedis.blpop(timeout, FRONTIER_QUEUE_KEY);

      if (result != null && result.size() > 1) {
        return Optional.of(Link.fromJson(result.get(1)));
      } else {
        return Optional.empty();
      }
    } catch (Exception e) {
      var message = "Failed to get link from queue";

      logger.error(message, e);
      throw new RedisException(message, e);
    }
  }

  @Override
  public void push(Link link) {
    try (Jedis jedis = jedisPool.getResource()) {
      long result = jedis.lpush(FRONTIER_QUEUE_KEY, link.toJson());

      logger.info("Pushed link to Frontier queue, result: {}", result > 0);
    } catch (Exception e) {
      var message = "Failed to add link to queue";

      logger.error(message, e);
      throw new RedisException(message, e);
    }
  }
}

package org.crawler.domain.config;

import redis.clients.jedis.JedisPoolConfig;

public record RedisConfig(int timeout, String host, int port, JedisPoolConfig jedisPoolConfig) {}

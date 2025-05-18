package org.crawler.domain.config;

import org.crawler.domain.Link;

public record AppConfig(Link seedLink, int maxDepth, RedisConfig redis) {}

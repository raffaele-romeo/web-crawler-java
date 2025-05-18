package org.crawler.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.crawler.domain.Link;
import org.crawler.domain.config.AppConfig;
import org.crawler.domain.config.RedisConfig;
import org.crawler.domain.exception.ConfigurationException;

public class ConfigLoaderImpl implements ConfigLoader {
  @Override
  public AppConfig load(String propertiesFilePath) {
    Properties props = new Properties();

    try {
      InputStream input =
          ConfigLoaderImpl.class.getClassLoader().getResourceAsStream(propertiesFilePath);
      props.load(input);
    } catch (IOException e) {
      throw new ConfigurationException(e);
    }

    return loadAppConfig(props);
  }

  private AppConfig loadAppConfig(Properties props) throws ConfigurationException {
    try {
      String seedLinkStr = props.getProperty("app.seedLink");
      if (seedLinkStr == null) {
        throw new ConfigurationException("Missing app.seedLink property");
      }
      Link seedLink = new Link(seedLinkStr, 0);

      String maxDepthStr = props.getProperty("app.maxDepth");
      if (maxDepthStr == null) {
        throw new ConfigurationException("Missing app.maxDepth property");
      }
      int maxDepth = Integer.parseInt(maxDepthStr);

      String redisTimeoutStr = props.getProperty("redis.timeout");
      if (redisTimeoutStr == null) {
        throw new ConfigurationException("Missing redis.timeout property");
      }
      int redisTimeout = Integer.parseInt(redisTimeoutStr);

      RedisConfig redisConfig = new RedisConfig(redisTimeout);

      return new AppConfig(seedLink, maxDepth, redisConfig);
    } catch (Exception e) {
      throw new ConfigurationException("Error parsing configuration: " + e.getMessage(), e);
    }
  }
}

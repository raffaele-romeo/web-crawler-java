package org.crawler.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.function.Function;
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
      var propertyReader = makePropertyReader(props);

      Link seedLink = new Link(URI.create(propertyReader.apply("app.seedLink")), 0);

      int maxDepth = Integer.parseInt(propertyReader.apply("app.maxDepth"));

      int redisTimeout = Integer.parseInt(propertyReader.apply("redis.timeout"));

      RedisConfig redisConfig = new RedisConfig(redisTimeout);

      return new AppConfig(seedLink, maxDepth, redisConfig);
    } catch (Exception e) {
      throw new ConfigurationException("Error parsing configuration: " + e.getMessage(), e);
    }
  }

  private Function<String, String> makePropertyReader(Properties props) {
    return name -> {
      String value = props.getProperty(name);
      if (value == null) {
        throw new ConfigurationException(String.format("Missing %s property", name));
      }

      return value;
    };
  }
}

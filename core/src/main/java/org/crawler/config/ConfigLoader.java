package org.crawler.config;

import org.crawler.domain.config.AppConfig;

public interface ConfigLoader {
  AppConfig load(String propertiesFilePath);
}

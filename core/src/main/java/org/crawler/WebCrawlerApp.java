package org.crawler;

import org.crawler.config.ConfigLoaderImpl;

public class WebCrawlerApp {
  public static void main(String[] args) {
    new WebCrawlerRunner().run(new ConfigLoaderImpl().load("config.properties"));
  }
}

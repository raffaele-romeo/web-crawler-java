package org.crawler.service;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.BaseRobotsParser;
import crawlercommons.robots.SimpleRobotRulesParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RobotsCheckerImpl implements RobotsChecker {
  private static final Logger logger = LoggerFactory.getLogger(RobotsCheckerImpl.class);

  private static final String USER_AGENT = "MyCrawler";

  @Override
  public boolean isUrlAllowed(URI uri) {
    InputStream inputStream = null;
    ByteArrayOutputStream baos = null;

    try {
      URL url = uri.toURL();
      String host = url.getProtocol() + "://" + url.getHost();
      String robotsTxtUrl = host + "/robots.txt";

      HttpURLConnection connection =
          (HttpURLConnection) URI.create(robotsTxtUrl).toURL().openConnection();
      connection.setRequestProperty("User-Agent", USER_AGENT);

      inputStream = connection.getInputStream();
      baos = new ByteArrayOutputStream();
      inputStream.transferTo(baos);
      byte[] content = baos.toByteArray();

      BaseRobotsParser parser = new SimpleRobotRulesParser();
      BaseRobotRules rules =
          parser.parseContent(robotsTxtUrl, content, "text/plain", Set.of(USER_AGENT));

      return rules.isAllowed(url.toString());
    } catch (Exception e) {
      return true;
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          logger.error("Failed to close input stream");
        }
      }

      if (baos != null) {
        try {
          baos.close();
        } catch (IOException e) {
          logger.error("Failed to close output stream");
        }
      }
    }
  }
}

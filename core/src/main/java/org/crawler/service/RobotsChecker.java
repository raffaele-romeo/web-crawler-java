package org.crawler.service;

import java.net.URI;

public interface RobotsChecker {
  boolean isUrlAllowed(URI uri);
}

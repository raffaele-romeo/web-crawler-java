package org.crawler;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public abstract class AbstractIntegrationTest {
  static final GenericContainer<?> REDIS_CONTAINER;
  static final WireMockServer WIRE_MOCK_SERVER;

  static {
    REDIS_CONTAINER =
        new GenericContainer<>("redis:8.0.1")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());
    REDIS_CONTAINER.start();

    WIRE_MOCK_SERVER = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    WIRE_MOCK_SERVER.start();
  }

  protected String redisHost() {
    return REDIS_CONTAINER.getHost();
  }

  protected int redisPort() {
    return REDIS_CONTAINER.getMappedPort(6379);
  }

  protected String wireMockBaseUrl() {
    return "http://localhost:" + WIRE_MOCK_SERVER.port();
  }
}

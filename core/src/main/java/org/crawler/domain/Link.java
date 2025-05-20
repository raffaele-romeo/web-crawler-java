package org.crawler.domain;

import com.google.gson.Gson;
import java.net.URI;

public record Link(URI uri, int depth) {
  private static final Gson gson = new Gson();

  public String toJson() {
    return gson.toJson(this);
  }

  public static Link fromJson(String json) {
    return gson.fromJson(json, Link.class);
  }
}

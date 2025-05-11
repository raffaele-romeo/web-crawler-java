package org.crawler.domain;

import com.google.gson.Gson;

public record Link(String url, int depth) {
  private static final Gson gson = new Gson();

  public String toJson() {
    return gson.toJson(this);
  }

  public static Link fromJson(String json) {
    return gson.fromJson(json, Link.class);
  }
}

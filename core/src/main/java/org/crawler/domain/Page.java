package org.crawler.domain;

import com.google.gson.Gson;

public record Page(Link link, String html) {
  private static final Gson gson = new Gson();

  public String toJson() {
    return gson.toJson(this);
  }

  public static Page fromJson(String json) {
    return gson.fromJson(json, Page.class);
  }
}

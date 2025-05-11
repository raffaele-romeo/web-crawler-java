package org.crawler.domain;

import com.google.gson.Gson;
import java.util.Set;

public record Result(String parentUrl, String html, Set<String> urls) {
  private static final Gson gson = new Gson();

  public String toJson() {
    return gson.toJson(this);
  }

  public static Result fromJson(String json) {
    return gson.fromJson(json, Result.class);
  }
}

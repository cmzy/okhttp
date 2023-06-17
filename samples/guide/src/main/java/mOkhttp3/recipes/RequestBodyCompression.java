/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mOkhttp3.recipes;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import mOkhttp3.Interceptor;
import mOkhttp3.MediaType;
import mOkhttp3.OkHttpClient;
import mOkhttp3.Request;
import mOkhttp3.RequestBody;
import mOkhttp3.Response;
import mOkio.BufferedSink;
import mOkio.GzipSink;
import mOkio.Okio;

public final class RequestBodyCompression {
  /**
   * The Google API KEY for OkHttp recipes. If you're using Google APIs for anything other than
   * running these examples, please request your own client ID!
   *
   * https://console.developers.google.com/project
   */
  public static final String GOOGLE_API_KEY = "AIzaSyAx2WZYe0My0i-uGurpvraYJxO7XNbwiGs";
  public static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json");

  private final OkHttpClient client = new OkHttpClient.Builder()
      .addInterceptor(new GzipRequestInterceptor())
      .build();
  private final Moshi moshi = new Moshi.Builder().build();
  private final JsonAdapter<Map<String, String>> mapJsonAdapter = moshi.adapter(
      Types.newParameterizedType(Map.class, String.class, String.class));

  public void run() throws Exception {
    Map<String, String> requestBody = new LinkedHashMap<>();
    requestBody.put("longUrl", "https://publicobject.com/2014/12/04/html-formatting-javadocs/");
    RequestBody jsonRequestBody = RequestBody.create(
        mapJsonAdapter.toJson(requestBody), MEDIA_TYPE_JSON);
    Request request = new Request.Builder()
        .url("https://www.googleapis.com/urlshortener/v1/url?key=" + GOOGLE_API_KEY)
        .post(jsonRequestBody)
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

      System.out.println(response.body().string());
    }
  }

  public static void main(String... args) throws Exception {
    new RequestBodyCompression().run();
  }

  /** This interceptor compresses the HTTP request body. Many webservers can't handle this! */
  static class GzipRequestInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
      Request originalRequest = chain.request();
      if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
        return chain.proceed(originalRequest);
      }

      Request compressedRequest = originalRequest.newBuilder()
          .header("Content-Encoding", "gzip")
          .method(originalRequest.method(), gzip(originalRequest.body()))
          .build();
      return chain.proceed(compressedRequest);
    }

    private RequestBody gzip(final RequestBody body) {
      return new RequestBody() {
        @Override public MediaType contentType() {
          return body.contentType();
        }

        @Override public long contentLength() {
          return -1; // We don't know the compressed length in advance!
        }

        @Override public void writeTo(BufferedSink sink) throws IOException {
          BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
          body.writeTo(gzipSink);
          gzipSink.close();
        }
      };
    }
  }
}

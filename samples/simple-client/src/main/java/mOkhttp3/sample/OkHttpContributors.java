package mOkhttp3.sample;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.util.Collections;
import java.util.List;
import mOkhttp3.OkHttpClient;
import mOkhttp3.Request;
import mOkhttp3.Response;
import mOkhttp3.ResponseBody;

public class OkHttpContributors {
  private static final String ENDPOINT = "https://api.github.com/repos/square/okhttp/contributors";
  private static final Moshi MOSHI = new Moshi.Builder().build();
  private static final JsonAdapter<List<Contributor>> CONTRIBUTORS_JSON_ADAPTER = MOSHI.adapter(
      Types.newParameterizedType(List.class, Contributor.class));

  static class Contributor {
    String login;
    int contributions;
  }

  public static void main(String... args) throws Exception {
    OkHttpClient client = new OkHttpClient();

    // Create request for remote resource.
    Request request = new Request.Builder()
        .url(ENDPOINT)
        .build();

    // Execute the request and retrieve the response.
    try (Response response = client.newCall(request).execute()) {
      // Deserialize HTTP response to concrete type.
      ResponseBody body = response.body();
      mOkio.BufferedSource source = body.source();
      okio.Source sourceOkio = okio.Okio.source(source.inputStream());
      List<Contributor> contributors = CONTRIBUTORS_JSON_ADAPTER.fromJson(okio.Okio.buffer(sourceOkio));

      // Sort list by the most contributions.
      Collections.sort(contributors, (c1, c2) -> c2.contributions - c1.contributions);

      // Output list of contributors.
      for (Contributor contributor : contributors) {
        System.out.println(contributor.login + ": " + contributor.contributions);
      }
    }
  }

  private OkHttpContributors() {
    // No instances.
  }
}

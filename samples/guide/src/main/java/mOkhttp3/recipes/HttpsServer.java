/*
 * Copyright (C) 2018 Square, Inc.
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

import java.net.InetAddress;
import mOkhttp3.Call;
import mOkhttp3.OkHttpClient;
import mOkhttp3.Request;
import mOkhttp3.Response;
import mOkhttp3.mockwebserver.MockResponse;
import mOkhttp3.mockwebserver.MockWebServer;
import mOkhttp3.tls.HandshakeCertificates;
import mOkhttp3.tls.HeldCertificate;

/**
 * Create an HTTPS server with a self-signed certificate that OkHttp trusts.
 */
public class HttpsServer {
  public void run() throws Exception {
    String localhost = InetAddress.getByName("localhost").getCanonicalHostName();
    HeldCertificate localhostCertificate = new HeldCertificate.Builder()
        .addSubjectAlternativeName(localhost)
        .build();

    HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder()
        .heldCertificate(localhostCertificate)
        .build();
    MockWebServer server = new MockWebServer();
    server.useHttps(serverCertificates.sslSocketFactory(), false);
    server.enqueue(new MockResponse());

    HandshakeCertificates clientCertificates = new HandshakeCertificates.Builder()
        .addTrustedCertificate(localhostCertificate.certificate())
        .build();
    OkHttpClient client = new OkHttpClient.Builder()
        .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager())
        .build();

    Call call = client.newCall(new Request.Builder()
        .url(server.url("/"))
        .build());
    Response response = call.execute();
    System.out.println(response.handshake().tlsVersion());
  }

  public static void main(String... args) throws Exception {
    new HttpsServer().run();
  }
}

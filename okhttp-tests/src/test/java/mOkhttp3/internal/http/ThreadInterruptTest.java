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
package mOkhttp3.internal.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import mOkhttp3.Call;
import mOkhttp3.DelegatingServerSocketFactory;
import mOkhttp3.DelegatingSocketFactory;
import mOkhttp3.MediaType;
import mOkhttp3.OkHttpClient;
import mOkhttp3.OkHttpClientTestRule;
import mOkhttp3.Request;
import mOkhttp3.RequestBody;
import mOkhttp3.Response;
import mOkhttp3.mockwebserver.MockResponse;
import mOkhttp3.mockwebserver.MockWebServer;
import mOkio.Buffer;
import mOkio.BufferedSink;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

public final class ThreadInterruptTest {
  @Rule public final OkHttpClientTestRule clientTestRule = new OkHttpClientTestRule();

  // The size of the socket buffers in bytes.
  private static final int SOCKET_BUFFER_SIZE = 256 * 1024;

  private MockWebServer server;
  private OkHttpClient client = clientTestRule.client;

  @Before public void setUp() throws Exception {
    // Sockets on some platforms can have large buffers that mean writes do not block when
    // required. These socket factories explicitly set the buffer sizes on sockets created.
    server = new MockWebServer();
    server.setServerSocketFactory(
        new DelegatingServerSocketFactory(ServerSocketFactory.getDefault()) {
          @Override
          protected ServerSocket configureServerSocket(ServerSocket serverSocket)
              throws IOException {
            serverSocket.setReceiveBufferSize(SOCKET_BUFFER_SIZE);
            return serverSocket;
          }
        });
    client = clientTestRule.client.newBuilder()
        .socketFactory(new DelegatingSocketFactory(SocketFactory.getDefault()) {
          @Override
          protected Socket configureSocket(Socket socket) throws IOException {
            socket.setSendBufferSize(SOCKET_BUFFER_SIZE);
            socket.setReceiveBufferSize(SOCKET_BUFFER_SIZE);
            return socket;
          }
        })
        .build();
  }

  @After public void tearDown() throws Exception {
    Thread.interrupted(); // Clear interrupted state.
  }

  @Test public void interruptWritingRequestBody() throws Exception {
    server.enqueue(new MockResponse());
    server.start();

    Call call = client.newCall(new Request.Builder()
        .url(server.url("/"))
        .post(new RequestBody() {
          @Override public @Nullable MediaType contentType() {
            return null;
          }

          @Override public void writeTo(BufferedSink sink) throws IOException {
            for (int i = 0; i < 10; i++) {
              sink.writeByte(0);
              sink.flush();
              sleep(100);
            }
            fail("Expected connection to be closed");
          }
        })
        .build());

    interruptLater(500);
    try {
      call.execute();
      fail();
    } catch (IOException expected) {
    }
  }

  @Test public void interruptReadingResponseBody() throws Exception {
    int responseBodySize = 8 * 1024 * 1024; // 8 MiB.

    server.enqueue(new MockResponse()
        .setBody(new Buffer().write(new byte[responseBodySize]))
        .throttleBody(64 * 1024, 125, TimeUnit.MILLISECONDS)); // 500 Kbps
    server.start();

    Call call = client.newCall(new Request.Builder()
        .url(server.url("/"))
        .build());

    Response response = call.execute();
    interruptLater(500);
    InputStream responseBody = response.body().byteStream();
    byte[] buffer = new byte[1024];
    try {
      while (responseBody.read(buffer) != -1) {
      }
      fail("Expected connection to be interrupted");
    } catch (IOException expected) {
    }

    responseBody.close();
  }

  private void sleep(int delayMillis) {
    try {
      Thread.sleep(delayMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void interruptLater(int delayMillis) {
    Thread toInterrupt = Thread.currentThread();
    Thread interruptingCow = new Thread(() -> {
      sleep(delayMillis);
      toInterrupt.interrupt();
    });
    interruptingCow.start();
  }
}

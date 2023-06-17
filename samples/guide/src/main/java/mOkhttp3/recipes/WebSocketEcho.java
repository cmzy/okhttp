package mOkhttp3.recipes;

import java.util.concurrent.TimeUnit;
import mOkhttp3.OkHttpClient;
import mOkhttp3.Request;
import mOkhttp3.Response;
import mOkhttp3.WebSocket;
import mOkhttp3.WebSocketListener;
import mOkio.ByteString;

public final class WebSocketEcho extends WebSocketListener {
  private void run() {
    OkHttpClient client = new OkHttpClient.Builder()
        .readTimeout(0,  TimeUnit.MILLISECONDS)
        .build();

    Request request = new Request.Builder()
        .url("ws://echo.websocket.org")
        .build();
    client.newWebSocket(request, this);

    // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
    client.dispatcher().executorService().shutdown();
  }

  @Override public void onOpen(WebSocket webSocket, Response response) {
    webSocket.send("Hello...");
    webSocket.send("...World!");
    webSocket.send(ByteString.decodeHex("deadbeef"));
    webSocket.close(1000, "Goodbye, World!");
  }

  @Override public void onMessage(WebSocket webSocket, String text) {
    System.out.println("MESSAGE: " + text);
  }

  @Override public void onMessage(WebSocket webSocket, ByteString bytes) {
    System.out.println("MESSAGE: " + bytes.hex());
  }

  @Override public void onClosing(WebSocket webSocket, int code, String reason) {
    webSocket.close(1000, null);
    System.out.println("CLOSE: " + code + " " + reason);
  }

  @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    t.printStackTrace();
  }

  public static void main(String... args) {
    new WebSocketEcho().run();
  }
}

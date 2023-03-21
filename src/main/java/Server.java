package org.example;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;


import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
  private final List<String> validPath = List.of("/index.html", "/spring.svg", "/spring.png",
    "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html",
    "/classic.html", "/events.html", "/events.js");
  private final int PORT = 9999;
  private final ExecutorService executorService;
  private final ServerSocket serverSocket;

  public Server() {
    try {
      serverSocket = new ServerSocket(PORT);
      executorService = Executors.newFixedThreadPool(64);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void start() {
    while (true) {
      try {
        final var socket = serverSocket.accept();
        executorService.submit(() -> {
          this.connection(socket);
        });

      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public void connection(Socket socket) {
    try (
      final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      final var out = new BufferedOutputStream(socket.getOutputStream())) {
      while (true) {
        // read only request line for simplicity
        // must be in form GET /path HTTP/1.1
        final var requestLine = in.readLine();
        if (requestLine == null || requestLine.trim().length() == 0) {
          break;
        }
        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
          socket.close();
          break;
        }

        final var path = parts[1];
        if (!validPath.contains(path)) {
          notFound404(out);
          break;
        }

        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);

        // special case for classic
        if (path.equals("/classic.html")) {
          classicPath(filePath, out, mimeType);
          break;

        }
        success200(filePath, out, mimeType);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private void success200(Path filePath, OutputStream out, String mimeType) {
    try {
      final var length = Files.size(filePath);
      out.write((
        "HTTP/1.1 200 OK\r\n" +
          "Content-Type: " + mimeType + "\r\n" +
          "Content-Length: " + length + "\r\n" +
          "Connection: close\r\n" +
          "\r\n"
      ).getBytes());
      Files.copy(filePath, out);
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void classicPath(Path filePath, OutputStream out, String mimeType) {
    try {
      final var template = Files.readString(filePath);
      final var content = template.replace(
        "{time}",
        LocalDateTime.now().toString()
      ).getBytes();
      out.write((
        "HTTP/1.1 200 OK\r\n" +
          "Content-Type: " + mimeType + "\r\n" +
          "Content-Length: " + content.length + "\r\n" +
          "Connection: close\r\n" +
          "\r\n"
      ).getBytes());
      out.write(content);
      out.flush();

    } catch (IOException e) {
    e.printStackTrace();
    }
  }

  private void notFound404(OutputStream out) {
    try {
      out.write((
        "HTTP/1.1 404 Not Found\r\n" +
          "Content-Length: 0\r\n" +
          "Connection: close\r\n" +
          "\r\n"
      ).getBytes());
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
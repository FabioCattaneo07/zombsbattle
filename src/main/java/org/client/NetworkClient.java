package org.client;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient extends Thread {

    private final String host;
    private final int port;
    private final Consumer<String> messageHandler;

    private Socket socket;
    private PrintWriter out;

    public NetworkClient(String host, int port, Consumer<String> messageHandler) {
        this.host = host;
        this.port = port;
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(host, port);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                messageHandler.accept(message);
            }

        } catch (IOException e) {
            System.out.println("Impossibile connettersi al server.");
        }
    }

    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }
}
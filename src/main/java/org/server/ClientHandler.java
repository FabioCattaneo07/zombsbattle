package org.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {

    private final Socket socket;
    private final GameServer server;
    private final int playerId;
    private PrintWriter out;

    public ClientHandler(Socket socket, GameServer server, int playerId) {
        this.socket = socket;
        this.server = server;
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            out = new PrintWriter(socket.getOutputStream(), true);

            send("WELCOME " + playerId);

            String command;
            while ((command = in.readLine()) != null) {
                server.handleCommand(playerId, command);
            }

        } catch (IOException e) {
            System.out.println("Connessione persa con player " + playerId);
        } finally {
            server.removeClient(this);

            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}
package org.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {

    private final int port;
    private final GameWorld world = new GameWorld();
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public GameServer(int port) {
        this.port = port;
    }

    public void start() {
        new GameLoop(world, this).start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("SERVER AVVIATO sulla porta " + port);

            while (true) {
                Socket socket = serverSocket.accept();

                int playerId = world.addPlayer();
                ClientHandler client = new ClientHandler(socket, this, playerId);

                clients.add(client);
                client.start();

                System.out.println("Client connesso. Player ID = " + playerId);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleCommand(int playerId, String command) {
        world.handleCommand(playerId, command);
    }

    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        world.removePlayer(client.getPlayerId());
        System.out.println("Client disconnesso. Player ID = " + client.getPlayerId());
    }

    public boolean hasPlayers() {
        return !clients.isEmpty();
    }
}
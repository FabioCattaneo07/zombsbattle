package org.server;

public class GameLoop extends Thread {

    private final GameWorld world;
    private final GameServer server;

    private boolean sleeping = false;

    public GameLoop(GameWorld world, GameServer server) {
        this.world = world;
        this.server = server;
    }

    @Override
    public void run() {

        while (true) {

            try {

                // Nessun player online
                if (!server.hasPlayers()) {

                    if (!sleeping) {
                        System.out.println("Server in standby...");
                        sleeping = true;
                    }

                    Thread.sleep(1000);
                    continue;
                }

                if (world.isMatchOver()) {
                    server.broadcast(world.serialize());

                    if (world.shouldRestartMatch()) {
                        System.out.println("Match concluso. Riavvio del server/match...");
                        world.resetMatch();
                        server.broadcast(world.serialize());
                    }

                    Thread.sleep(50);
                    continue;
                }

                // Primo player entrato
                if (sleeping) {
                    System.out.println("Player rilevato. Ripresa game loop.");
                    sleeping = false;
                }

                world.update();

                server.broadcast(world.serialize());

                Thread.sleep(50);

            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
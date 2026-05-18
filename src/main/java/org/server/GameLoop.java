package org.server;

public class GameLoop extends Thread {

    private final GameWorld world;
    private final GameServer server;

    public GameLoop(GameWorld world, GameServer server) {
        this.world = world;
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            world.update();
            server.broadcast(world.serialize());

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
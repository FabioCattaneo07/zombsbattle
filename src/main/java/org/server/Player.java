package org.server;

public class Player {
    public int id;
    public double x;
    public double y;

    public int health = 100;

    public int ammo = 5;
    public static final int MAX_AMMO = 5;

    public long lastDamageTime = 0;
    public long lastReloadTime = System.currentTimeMillis();

    public Player(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public void updateReload() {
        long now = System.currentTimeMillis();

        if (now - lastReloadTime >= 10_000) {
            ammo = MAX_AMMO;
            lastReloadTime = now;
        }
    }
}
package org.server;

import java.util.List;

public class Zombie {

    public enum Type {
        NORMAL(1, 1.5, 1.0, 18),
        BIG(2, 1.15, 1.28, 24),
        FAST(1, 2.35, 0.82, 14);

        public final int maxHealth;
        public final double speed;
        public final double scale;
        public final double hitRadius;

        Type(int maxHealth, double speed, double scale, double hitRadius) {
            this.maxHealth = maxHealth;
            this.speed = speed;
            this.scale = scale;
            this.hitRadius = hitRadius;
        }
    }

    public int id;
    public double x;
    public double y;
    public double speed;
    public final Type type;
    public int health;

    public Zombie(int id, double x, double y, Type type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
        this.speed = type.speed;
        this.health = type.maxHealth;
    }

    public void moveToward(double targetX, double targetY) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance == 0) return;

        x += speed * dx / distance;
        y += speed * dy / distance;
    }

    public boolean damage(int amount) {
        health -= amount;
        return health <= 0;
    }

    public double getRenderSize() {
        return 30 * type.scale;
    }

    public double getHitRadius() {
        return type.hitRadius;
    }

    public void separateFromOtherZombies(List<Zombie> zombies) {
        double minDistance = 35;

        for (Zombie other : zombies) {
            if (other == this) continue;

            double dx = x - other.x;
            double dy = y - other.y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 0 && distance < minDistance) {
                double push = (minDistance - distance) * 0.04;

                x += push * dx / distance;
                y += push * dy / distance;
            }
        }
    }
}
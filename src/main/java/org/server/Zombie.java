package org.server;

import java.util.List;

public class Zombie {
    public int id;
    public double x;
    public double y;
    public double speed = 1.5;

    public Zombie(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public void moveToward(double targetX, double targetY) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance == 0) return;

        x += speed * dx / distance;
        y += speed * dy / distance;
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
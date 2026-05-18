package org.server;

public class Wall {
    public double x, y, width, height;

    public Wall(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean collides(double px, double py, double radius) {
        double closestX = Math.max(x, Math.min(px, x + width));
        double closestY = Math.max(y, Math.min(py, y + height));

        double dx = px - closestX;
        double dy = py - closestY;

        return dx * dx + dy * dy < radius * radius;
    }
}
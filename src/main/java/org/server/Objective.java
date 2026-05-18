package org.server;

public class Objective {
    public double x;
    public double y;
    public int health = 200;
    public static final int MAX_HEALTH = 200;

    public Objective(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
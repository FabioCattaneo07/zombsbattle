package org.server;

public class Shot {
    public double x1;
    public double y1;
    public double x2;
    public double y2;
    public long createdAt;

    public Shot(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > 150;
    }
}
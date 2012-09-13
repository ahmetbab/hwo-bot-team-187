package redlynx.pong.util;

public class Vector2 {

    public double x;
    public double y;

    public Vector2() {
        x = y = 0;
    }

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    double length() {
        return Math.sqrt(x*x + y*y + 0.000000001);
    }

    public Vector2 normalize() {
        double l = length();
        x /= l;
        y /= l;
        return this;
    }

    public double distance(Vector2 v) {
        return Math.sqrt(distanceSquared(v));
    }

    public double distanceSquared(Vector2 v) {
        double dx = v.x - x;
        double dy = v.y - y;
        return dx * dx + dy * dy;
    }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

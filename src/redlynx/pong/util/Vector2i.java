package redlynx.pong.util;

public class Vector2i {

    public int x;
    public int y;

    public Vector2i() {
        x = y = 0;
    }

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    double length() {
        return Math.sqrt(x*x + y*y);
    }

    public double distance(Vector2i v) {
        return Math.sqrt(distanceSquared(v));
    }

    public int distanceSquared(Vector2i v) {
        int dx = v.x - x;
        int dy = v.y - y;
        return dx * dx + dy * dy;
    }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

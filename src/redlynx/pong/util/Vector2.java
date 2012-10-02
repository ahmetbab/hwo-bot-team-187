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

    public Vector2(Vector2 pos) {
        this.x = pos.x;
        this.y = pos.y;
    }

    public double length() {
        return Math.sqrt(x*x + y*y + 0.000000001);
    }

    public void rotate(double rad) {
        double xnew = x * Math.cos(rad) - y * Math.sin(rad);
        y = x * Math.sin(rad) + y * Math.cos(rad);
        x = xnew;
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

    public Vector2 minus(Vector2 v) {
        return new Vector2(x - v.x, y - v.y);
    }

    public Vector2 scaled(double s) {
        x *= s;
        y *= s;
        return this;
    }
    public Vector2 setLength(double s) {
    	double mul = s/length();
        x *= mul;
        y *= mul;
        return this;
    }
    

    public void copy(Vector2 v) {
        x = v.x;
        y = v.y;
    }
}

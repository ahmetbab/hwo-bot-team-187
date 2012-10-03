package redlynx.pong.util;

public class Vector3 {

    public double x, y, z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void copy(Vector3 v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }
}

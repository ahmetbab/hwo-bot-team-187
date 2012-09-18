package redlynx.pong.client.collisionmodel;

import redlynx.pong.util.Vector2;

public interface PongModel {
    public void learn(double pos, double vy_in, double vy_out);
    public Vector2 guess(double pos, double vx_in, double vy_in);
    public double modelError();
}

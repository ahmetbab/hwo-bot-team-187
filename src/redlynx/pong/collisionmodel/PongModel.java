package redlynx.pong.collisionmodel;

import redlynx.pong.Vector2;

public interface PongModel {
    public void learn(double pos, Vector2 in, Vector2 out);
    public Vector2 guess(float pos, Vector2 in);
    public double modelError();
}

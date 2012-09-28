package redlynx.pong.collisionmodel;

import redlynx.pong.util.Vector2;

public interface PongModel {
    public void learn(double pos, double vy_in, double vy_out);

    public double getAngle(double vx_in, double vy_in);
    public Vector2 guess(double pos, double vx_in, double vy_in);
    public Vector2 guessGivenAngle(double pos, double vx_in, double vy_in, double angle);
    public Vector2 guessGivenSpeed(double pos, double vx_in, double vy_in, double speed);
    public double modelError();
}

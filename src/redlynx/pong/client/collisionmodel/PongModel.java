package redlynx.pong.client.collisionmodel;

public interface PongModel {
    public void learn(double pos, double vy_in, double vy_out);
    public double guess(double pos, double vy_in);
    public double modelError();
}

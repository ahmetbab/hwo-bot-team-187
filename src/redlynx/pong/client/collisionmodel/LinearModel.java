package redlynx.pong.client.collisionmodel;

import redlynx.pong.util.Vector2;

public class LinearModel implements PongModel {


    private double modelValue = 1.3;
    private final Vector2 out = new Vector2();

    @Override
    public void learn(double pos, double vy_in, double vy_out) {
    }

    @Override
    public Vector2 guess(double pos, double vx_in, double vy_in) {
        out.x = -vx_in;
        out.y = +vy_in + pos * Math.sqrt(vx_in * vx_in + vy_in * vy_in) * 0.3;
        return out;
    }

    @Override
    public double modelError() {
        return 10000000;
    }
}
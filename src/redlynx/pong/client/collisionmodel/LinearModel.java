package redlynx.pong.client.collisionmodel;

public class LinearModel implements PongModel {


    private double modelValue = 75;

    @Override
    public void learn(double pos, double vy_in, double vy_out) {
    }

    @Override
    public double guess(double pos, double vy_in) {
        return pos * modelValue;
    }

    @Override
    public double modelError() {
        return 10000000;
    }
}

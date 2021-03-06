package redlynx.bots.preliminaries.sauron;

import redlynx.pong.client.PongGameBot;
import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.util.Vector2;

public class SauronModel implements PongModel {

    private double modelValue = 1.3;
    private final Vector2 out = new Vector2();
    private final PongGameBot host;

    public SauronModel(PongGameBot bot) {
        this.host = bot;
    }

    @Override
    public void learn(double pos, double vx_in, double vy_in, double vx_out, double vy_out) {
    }


    @Override
    public Vector2 guess(double pos, double vx_in, double vy_in) {
        out.x = -vx_in;
        out.y = +vy_in + pos * host.getBallVelocity() * 0.3;
        return out;
    }

    public double getAngle(double vx_in, double vy_in) {return 0;}

    public Vector2 guessGivenAngle(double pos, double vx_in, double vy_in, double angle) {
    	return guess(pos, vx_in, vy_in);
    }
    @Override
    public Vector2 guessGivenSpeed(double pos, double vx_in, double vy_in, double speed) {
    	return guess(pos, vx_in, vx_in);
    }
    
    @Override
    public double modelError() {
        return 10000000;
    }
}

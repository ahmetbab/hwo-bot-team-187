package redlynx.bots.semifinals;

import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.collisionmodel.PongModel;
import redlynx.pong.util.Vector2;

public class SFSauronModel implements PongModel {

    private final Vector2 out = new Vector2();
    private final PongGameBot host;

    public SFSauronModel(PongGameBot bot) {
        this.host = bot;
    }

    @Override
    public void learn(double pos, double vy_in, double vy_out) {
    }

    @Override
    public Vector2 guess(double pos, double vx_in, double vy_in) {
        out.x = -vx_in;
        out.y = +vy_in + pos * host.getBallVelocity() * (0.22 + Math.abs(vy_in / vx_in) * 0.10);
        return out;
    }

    @Override
    public double getAngle(double vx_in, double vy_in) {return 0;}

    @Override
    public Vector2 guessGivenAngle(double pos, double vx_in, double vy_in, double angle) {
    	return guess(pos, vx_in, vy_in);
    }
    
    @Override
    public double modelError() {
        return 10000000;
    }
}

package redlynx.bots.finals;

import redlynx.pong.client.PongGameBot;
import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.util.Vector2;

public class FinalSauronModel implements PongModel {

    private final Vector2 out = new Vector2();
    private final PongGameBot host;

    public FinalSauronModel(PongGameBot bot) {
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
    public Vector2 guessGivenSpeed(double pos, double vx_in, double vy_in, double speed) {
    	  out.x = -vx_in;
          out.y = +vy_in + pos * speed * (0.22 + Math.abs(vy_in / vx_in) * 0.10);
          return out;
    }
    
    @Override
    public double modelError() {
        return 10000000;
    }
}
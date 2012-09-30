package redlynx.pong.collisionmodel;

import redlynx.pong.util.Vector2;

public class LinearModel implements PongModel {

    private final Vector2 out = new Vector2();

    public LinearModel() {
        
    }

    @Override
    public void learn(double pos, double vx_in, double vy_in, double vx_out, double vy_out) {
    }

    @Override
    public double getAngle(double vx_in, double vy_in) {
    	double inLength = Math.sqrt(vx_in*vx_in+ vy_in*vy_in);
        return Math.asin(Math.abs(vx_in / inLength));
    }
    @Override
    public Vector2 guess(double pos, double vx_in, double vy_in) {
    	return guessGivenAngle(pos, vx_in, vy_in, getAngle(vx_in, vy_in));
    }
    @Override
    public Vector2 guessGivenSpeed(double pos, double vx_in, double vy_in, double speed) {
    	double angle = Math.asin(Math.abs(vx_in / speed));
    	return guessGivenAngle(pos, vx_in, vy_in, angle);
    }
    
    @Override
    public Vector2 guessGivenAngle(double pos, double vx_in, double vy_in, double angle) {
        
        double outAngle;

        if(vy_in > 0) {
            //outAngle = angle * (1 - 0.27 * pos);
        	outAngle = angle * (1 - 0.18 * pos-0.23*pos*pos*pos);
        }
        else {
            //outAngle = angle * (1 + 0.27 * pos);
            outAngle = angle * (1 + 0.18 * pos+0.23*pos*pos*pos);
        }

        if(vx_in > 0) {
            out.x = -Math.sin(outAngle);
        }
        else {
            out.x = +Math.sin(outAngle);
        }

        if(vy_in > 0) {
            out.y = +Math.cos(outAngle);
        }
        else {
            out.y = -Math.cos(outAngle);
        }

        return out;
    }

    @Override
    public double modelError() {
        return 10000000;
    }
}

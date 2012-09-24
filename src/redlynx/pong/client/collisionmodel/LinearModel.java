package redlynx.pong.client.collisionmodel;

import redlynx.pong.util.Vector2;

public class LinearModel implements PongModel {

    private final Vector2 out = new Vector2();

    @Override
    public void learn(double pos, double vy_in, double vy_out) {
    }

    @Override
    public double getAngle(double vx_in, double vy_in) {
    	double inLength = Math.sqrt(vx_in * vx_in + vy_in * vy_in);
        return Math.asin(Math.abs(vx_in / inLength));
    }
    @Override
    public Vector2 guess(double pos, double vx_in, double vy_in) {
    	return guess(pos, vx_in, vy_in, getAngle(vx_in, vy_in));
    }
    
    @Override
    public Vector2 guess(double pos, double vx_in, double vy_in, double angle) {
        
        double outAngle;

        if(vy_in > 0) {
            outAngle = angle * (1 - 0.33 * pos);
        }
        else {
            outAngle = angle * (1 + 0.33 * pos);
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

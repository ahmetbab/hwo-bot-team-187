package redlynx.pong.collisionmodel;

import redlynx.pong.util.Vector2;

public class SFSauronGeneralModel implements PongModel {

    private final Vector2 out = new Vector2();
    
  

    public SFSauronGeneralModel() {
    }

    @Override
    public void learn(double pos, double vx_in, double vy_in, double vx_out, double vy_out) {
    }
    @Override
    public Vector2 guess(double pos, double vx_in, double vy_in) {
    	return guessGivenSpeed(pos, vx_in, vy_in, Math.sqrt(vx_in*vx_in + vy_in*vy_in));
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
        //out.y = +vy_in + pos * speed * (0.22 + Math.abs(vy_in / vx_in) * 0.10);
        out.y = +vy_in + pos * speed * (0.253 + Math.abs(vy_in / vx_in) * 0.01); //better at smaller angles
        return out;
    }
    
    @Override
    public double modelError() {
        return 10000000;
    }
}

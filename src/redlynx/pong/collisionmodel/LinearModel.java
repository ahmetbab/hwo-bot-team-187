package redlynx.pong.collisionmodel;

import redlynx.pong.util.Vector2;

import java.util.ArrayList;

public class LinearModel implements PongModel {


    private ArrayList<Double> samples = new ArrayList<Double>();
    private double modelValue = 0;

    @Override
    public void learn(double pos, Vector2 in, Vector2 out) {
        // bounced off a wall between measurements. discard sample.
        if(in.y * out.y < 0)
            return;

        in.normalize();
        out.normalize();

        if(pos == 0)
            return;

        double dy = out.y - in.y;
        double multiplier = dy / pos;

        samples.add(multiplier);

        System.out.println(multiplier);
    }

    @Override
    public Vector2 guess(float pos, Vector2 in) {
        if(modelValue == 0)
            modelError();
        return new Vector2(in.x, in.y + pos * modelValue);
    }

    @Override
    public double modelError() {
        double avg = 0;
        for(int i=0; i<samples.size(); ++i) {
            avg += samples.get(i);
        }
        avg /= samples.size();

        double error = 0;
        for(int i=0; i<samples.size(); ++i) {
           double delta = avg - samples.get(i);
            error += delta * delta;
        }

        return error / samples.size();
    }
}

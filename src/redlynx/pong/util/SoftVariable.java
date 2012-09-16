package redlynx.pong.util;

public class SoftVariable {

    private int pointer = 0;
    private double[] values = {0,0,0,0,0,0,0,0,0};
    private double sum = 0;

    public SoftVariable(double initialValue) {
        reset(initialValue);
    }

    public void reset(double initialValue) {
        for(int i=0; i<values.length; ++i) {
            values[i] = initialValue;
        }
        sum = initialValue * values.length;
    }

    public void update(double value) {
        sum -= values[pointer];
        values[pointer] = value;
        sum += value;
        pointer = ++pointer % values.length;
    }

    public double value() {
        return sum / values.length;
    }
}

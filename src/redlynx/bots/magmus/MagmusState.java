package redlynx.bots.magmus;

public class MagmusState {
    public boolean catching() {
        return activeState == StateName.GOTO_TARGET;
    }

    public double velocity() {
        return velocity;
    }

    public void setToHandling() {
        activeState = StateName.GOTO_TARGET;
    }

    public void setToWaiting() {
        activeState = StateName.HOLD_POSITION;
    }

    public void setVelocity(double v) {
        velocity = v;
    }

    public static enum StateName {
        HOLD_POSITION,
        GOTO_TARGET
    }

    public StateName activeState = StateName.GOTO_TARGET;
    public float currentTargetPosition = 0;
    public double velocity = 0;
}

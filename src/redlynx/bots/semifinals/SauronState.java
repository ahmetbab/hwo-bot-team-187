package redlynx.bots.semifinals;

public class SauronState {

    // this could be just removed. since we only use one state in the bot.
    public boolean catching() {
        return activeState == StateName.GOTO_TARGET;
    }

    public double velocity() {
        return velocity;
    }

    public void setToHandling() {
        activeState = StateName.GOTO_TARGET;
    }

    public void setVelocity(double v) {
        velocity = v;
    }

    public static enum StateName {
        HOLD_POSITION,
        GOTO_TARGET
    }

    public StateName activeState = StateName.GOTO_TARGET;
    public double velocity = 0;
}

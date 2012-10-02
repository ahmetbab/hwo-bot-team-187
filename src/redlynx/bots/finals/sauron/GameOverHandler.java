package redlynx.bots.finals.sauron;

public class GameOverHandler {
    private final FinalSauron finalSauron;

    public GameOverHandler(FinalSauron finalSauron) {
        this.finalSauron = finalSauron;
    }

    public void onGameOver(boolean won) {
        SauronState state = finalSauron.getMyState();
        state.setToHandling();

        finalSauron.getMyState().setVelocity(0);
        finalSauron.getLastKnownStatus().reset();
        finalSauron.getExtrapolatedStatus().reset();
        finalSauron.setExtrapolatedTime(0);
    }
}
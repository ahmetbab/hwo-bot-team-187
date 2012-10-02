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
        finalSauron.numGames++;

        if (won) {
            finalSauron.numWins++;
        }

        System.out.println(finalSauron.getDefaultName() + " wins " + finalSauron.numWins + "/" + finalSauron.numGames + " (" + ((float) finalSauron.numWins / finalSauron.numGames) + ")");

        finalSauron.getLastKnownStatus().reset();
        finalSauron.getExtrapolatedStatus().reset();
        finalSauron.setExtrapolatedTime(0);
    }
}
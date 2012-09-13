package redlynx.pong.util;

import redlynx.pong.state.GameStatus;
import redlynx.pong.state.PongGameBot;

public class GameStateAccessor implements GameStateAccessorInterface {

    private final PongGameBot bot;
    private GameStatus status;

    public GameStateAccessor(PongGameBot bot) {
        this.bot = bot;
        status = bot.getExtrapolatedStatus();
    }

    @Override
    public void fetchExtrapolated() {
        status = bot.getExtrapolatedStatus();
    }

    @Override
    public void fetchLastKnown() {
        status = bot.getLastKnownStatus();
    }

    @Override
    public int getBallRadius() {
        return status.conf.ballRadius;
    }

    @Override
    public double getLeftPedalY() {
        return status.left.y;
    }

    @Override
    public double getRightPedalY() {
        return status.right.y;
    }

    @Override
    public double getBallX() {
        return status.ball.x;
    }

    @Override
    public double getBallY() {
        return status.ball.y;
    }

    @Override
    public double getPedalHeight() {
        return status.conf.paddleHeight;
    }

    @Override
    public double getPedalWidth() {
        return status.conf.paddleWidth;
    }

    @Override
    public double getAreaWidth() {
        return status.conf.maxWidth;
    }

    @Override
    public double getAreaHeight() {
        return status.conf.maxHeight;
    }
}

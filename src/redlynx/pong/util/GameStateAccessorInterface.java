package redlynx.pong.util;

public interface GameStateAccessorInterface {
    void fetchExtrapolated();

    void fetchLastKnown();

    int getBallRadius();

    double getLeftPedalY();

    double getRightPedalY();

    double getBallX();

    double getBallY();

    double getPedalHeight();

    double getPedalWidth();
    double getAreaWidth();
    double getAreaHeight();
}

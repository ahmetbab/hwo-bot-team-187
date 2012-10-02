package redlynx.bots.finals.sauron;

import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

import java.awt.*;

public class SauronVisualiser {
    private final FinalSauron finalSauron;
    private final ClientGameState.Ball tmpBall = new ClientGameState.Ball();

    public SauronVisualiser(FinalSauron finalSauron) {
        this.finalSauron = finalSauron;
    }

    void visualisePlan(double paddleTarget, Color color) {
        tmpBall.copy(finalSauron.getBallWorkMemory(), true);
        finalSauron.ballCollideToPaddle(paddleTarget, tmpBall);
        PongUtil.simulateOld(tmpBall, finalSauron.getLastKnownStatus().conf, finalSauron.getLines(), color);
    }

    void visualiseModel(double x, double y, double minReach, double maxReach) {

        double targetPos = finalSauron.getBallWorkMemory().y - finalSauron.getLastKnownStatus().conf.paddleHeight * 0.5;
        double paddleMaxPos = finalSauron.getLastKnownStatus().conf.maxHeight - finalSauron.getLastKnownStatus().conf.paddleHeight;
        double paddleMinPos = 0;

        for (int i = 0; i < 11; ++i) {

            double pos = (i - 5) / 5.0;
            Color color = Color.green;

            if (pos < minReach || pos > maxReach) {
                color = Color.blue;
            }

            double evaluatedPaddlePos = targetPos - pos * finalSauron.getLastKnownStatus().conf.paddleHeight * 0.5;
            if (paddleMaxPos < evaluatedPaddlePos || paddleMinPos > evaluatedPaddlePos) {
                color = Color.red;
            }

            Vector2 ballOut = finalSauron.getMyModel().guess(pos, finalSauron.getBallWorkMemory().vx, finalSauron.getBallWorkMemory().vy);
            ballOut.normalize().scaled(100);

            double y_point = y + pos * finalSauron.getLastKnownStatus().conf.paddleHeight * 0.5 + finalSauron.getLastKnownStatus().conf.paddleHeight * 0.5;
            finalSauron.getLines().add(new UILine(new Vector2i(x, y_point), new Vector2i(x + ballOut.x, y_point + ballOut.y), color));

        }
    }
}
package redlynx.bots.magmus;

import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.client.state.PongGameBot;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;

public class MagmusEvaluator {


    private static ClientGameState.Ball ballMemory = new ClientGameState.Ball();

    public static Vector2 offensiveEval(PongGameBot bot, ClientGameState state, PongGameBot.PlayerSide catcher, ClientGameState.Ball collidingBallState, ClientGameState.Ball tmpBall, double minVal, double maxVal) {
        double targetPos = collidingBallState.y - state.conf.paddleHeight * 0.5;
        double botValue = -10000;
        double topValue = -10000;
        double paddleTargetBot = 0;
        double paddleTargetTop = 0;
        double paddleMaxPos = state.conf.maxHeight - state.conf.paddleHeight;
        double paddleMinPos = 0;

        double defensiveMaxMinScore = -1000000;
        double defensiveMaxMinTarget = 0;

        {
            for(int i=10; i<90; ++i) {
                double tmpTarget = (i - 50) / 50.0;
                double evaluatedPaddlePos = targetPos - tmpTarget * state.conf.paddleHeight * 0.5;

                // if return not physically possible, don't evaluate it.
                if(paddleMaxPos < evaluatedPaddlePos || paddleMinPos > evaluatedPaddlePos) {
                    continue;
                }

                // if not enough time left to make the return, don't evaluate it.
                if(tmpTarget < minVal || tmpTarget > maxVal) {
                    continue;
                }

                tmpBall.copy(collidingBallState, true);
                bot.ballCollideToPaddle(tmpTarget, tmpBall);
                double opponentTime = PongUtil.simulate(tmpBall, state.conf);
                double opponentReach = opponentTime * bot.getPaddleMaxVelocity() + state.conf.paddleHeight * 0.5;
                double opponentBot = state.getPedal(catcher).y - opponentReach - state.conf.paddleHeight * 0.5;
                double opponentTop = state.getPedal(catcher).y + opponentReach - state.conf.paddleHeight * 0.5;

                double tmpBotValue = -(tmpBall.y - opponentBot);
                double tmpTopValue = +(tmpBall.y - opponentTop);

                if(tmpBotValue > botValue) {
                    botValue = tmpBotValue;
                    paddleTargetBot = tmpTarget;
                }

                if(tmpTopValue > topValue) {
                    topValue = tmpTopValue;
                    paddleTargetTop = tmpTarget;
                }

                /*
                double defensiveScore = MagmusEvaluator.defensiveEval(bot, state, PongGameBot.PlayerSide.getOtherSide(catcher), opponentBot, opponentTop, tmpBall);
                if(defensiveScore > defensiveMaxMinScore) {
                    defensiveMaxMinScore = defensiveScore;
                    defensiveMaxMinTarget = tmpTarget;
                }
                */
            }
        }

        double bestValue = (botValue > topValue) ? botValue : topValue;

        // found a solid attack move! lets do that.
        if(true || bestValue > 0) {
            double paddleTarget = (botValue > topValue) ? paddleTargetBot : paddleTargetTop;
            targetPos -= paddleTarget * state.conf.paddleHeight * 0.5;

            // bind target position inside play area
            targetPos = targetPos < paddleMinPos ? paddleMinPos : targetPos;
            targetPos = targetPos > paddleMaxPos ? paddleMaxPos : targetPos;
            return new Vector2(targetPos, paddleTarget);
        }
        else {
            targetPos -= defensiveMaxMinTarget * state.conf.paddleHeight * 0.5;

            // bind target position inside play area
            targetPos = targetPos < paddleMinPos ? paddleMinPos : targetPos;
            targetPos = targetPos > paddleMaxPos ? paddleMaxPos : targetPos;
            return new Vector2(targetPos, defensiveMaxMinTarget);
        }
    }


    private static double defensiveEval(PongGameBot bot, ClientGameState state, PongGameBot.PlayerSide catcher, double minVal, double maxVal, ClientGameState.Ball tmpBall) {

        double targetPos = tmpBall.y - state.conf.paddleHeight * 0.5;
        double paddleMaxPos = state.conf.maxHeight - state.conf.paddleHeight;
        double paddleMinPos = 0;

        double minScore = 10000;

        for(int i=5; i<=45; ++i) {
            double tmpTarget = (i - 25) / 25.0;
            double evaluatedPaddlePos = targetPos - tmpTarget * state.conf.paddleHeight * 0.5;

            // if return not physically possible, don't evaluate it.
            if(paddleMaxPos < evaluatedPaddlePos || paddleMinPos > evaluatedPaddlePos) {
                continue;
            }

            // if not enough time left to make the return, don't evaluate it.
            if(tmpTarget < minVal || tmpTarget > maxVal) {
                continue;
            }

            ballMemory.copy(tmpBall, true);
            bot.ballCollideToPaddle(tmpTarget, ballMemory);

            double opponentTime = PongUtil.simulate(ballMemory, state.conf);

            // which returns are possible for the opponent?
            double opponentReach = opponentTime * bot.getPaddleMaxVelocity() + state.conf.paddleHeight * 0.5;
            double opponentBot = state.getPedal(catcher).y - opponentReach - state.conf.paddleHeight * 0.5;
            double opponentTop = state.getPedal(catcher).y + opponentReach - state.conf.paddleHeight * 0.5;

            if(opponentBot < tmpBall.y - state.conf.paddleHeight * 0.5) opponentBot = ballMemory.y - state.conf.paddleHeight * 0.5;
            if(opponentTop > tmpBall.y + state.conf.paddleHeight * 0.5) opponentTop = ballMemory.y + state.conf.paddleHeight * 0.5;
            opponentBot -= ballMemory.y;
            opponentTop -= ballMemory.y;

            // these are the possible return deflection values
            opponentBot /= state.conf.paddleHeight * 0.5;
            opponentTop /= state.conf.paddleHeight * 0.5;

            double score = opponentTop - opponentBot;
            if(score < minScore) {
                minScore = score;
            }
        }

        return minScore;
    }

}

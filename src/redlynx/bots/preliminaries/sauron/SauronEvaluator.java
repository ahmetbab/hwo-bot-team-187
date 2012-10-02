package redlynx.bots.preliminaries.sauron;

import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.client.PongGameBot;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector3;

public class SauronEvaluator {


    private ClientGameState.Ball ballMemory = new ClientGameState.Ball();
    private ClientGameState.Ball ballMemory2 = new ClientGameState.Ball();

    public Vector3 offensiveEval(PongGameBot bot, ClientGameState state, PongGameBot.PlayerSide catcher, ClientGameState.Ball collidingBallState, ClientGameState.Ball tmpBall, double minVal, double maxVal) {
        double targetPos = collidingBallState.y - state.conf.paddleHeight * 0.5;
        double botValue = -10000;
        double topValue = -10000;
        double paddleTargetBot = 0;
        double paddleTargetTop = 0;
        double paddleMaxPos = state.conf.maxHeight - state.conf.paddleHeight;
        double paddleMinPos = 0;

        {
            for(int i=10; i<90; ++i) {
                double tmpTarget = (i - 50) / 50.0;
                double evaluatedPaddlePos = targetPos - tmpTarget * state.conf.paddleHeight * 0.5;

                // if return not physically possible, don't evaluate it.
                if(paddleMaxPos < evaluatedPaddlePos || paddleMinPos > evaluatedPaddlePos) {
                    // System.out.println("out of bounds");
                    continue;
                }

                // if not enough time left to make the return, don't evaluate it.
                if(tmpTarget < minVal || tmpTarget > maxVal) {
                    // System.out.println("not enough time");
                    continue;
                }

                tmpBall.copy(collidingBallState, true);
                bot.ballCollideToPaddle(tmpTarget, tmpBall);

                double opponentTime = PongUtil.simulate(tmpBall, state.conf);
                double opponentReach = opponentTime * bot.getPaddleMaxVelocity() + state.conf.paddleHeight * 0.5;
                double opponentBot = state.getPedal(catcher).y - opponentReach + state.conf.paddleHeight * 0.5;
                double opponentTop = state.getPedal(catcher).y + opponentReach + state.conf.paddleHeight * 0.5;

                double tmpBotValue = +(opponentBot - tmpBall.y);
                double tmpTopValue = -(opponentTop - tmpBall.y);

                if(tmpBotValue > botValue) {
                    botValue = tmpBotValue;
                    paddleTargetBot = tmpTarget;
                }

                if(tmpTopValue > topValue) {
                    topValue = tmpTopValue;
                    paddleTargetTop = tmpTarget;
                }
            }
        }

        double bestValue = (botValue > topValue) ? botValue : topValue;

        // found a solid attack move! lets do that.
        double paddleTarget = (botValue > topValue) ? paddleTargetBot : paddleTargetTop;
        targetPos -= paddleTarget * state.conf.paddleHeight * 0.5;

        // bind target position inside play area
        targetPos = targetPos < paddleMinPos ? paddleMinPos : targetPos;
        targetPos = targetPos > paddleMaxPos ? paddleMaxPos : targetPos;
        return new Vector3(targetPos, paddleTarget, bestValue);
    }


    // defensive eval tries to minimize opponents offensive eval.
    public Vector3 defensiveEval(PongGameBot bot, ClientGameState state, PongGameBot.PlayerSide catcher, double minVal, double maxVal, ClientGameState.Ball tmpBall) {

        double targetPos = tmpBall.y - state.conf.paddleHeight * 0.5;
        double paddleMaxPos = state.conf.maxHeight - state.conf.paddleHeight;
        double paddleMinPos = 0;

        double minScore = -10000;
        double minTarget = 0;
        double minTargetPos = targetPos;

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

            // should take extra defense time into account somehow?
            double defenseTime = PongUtil.simulate(ballMemory, state.conf);

            // which returns are possible for the opponent?
            // Vector2 possibleReturns = bot.getPaddlePossibleReturns(state, ballMemory, PongGameBot.PlayerSide.getOtherSide(catcher), defenseTime);
            double opponentReach = defenseTime * bot.getPaddleMaxVelocity() + state.conf.paddleHeight * 0.5;
            double opponentBot = state.getPedal(catcher).y - opponentReach + state.conf.paddleHeight * 0.5;
            double opponentTop = state.getPedal(catcher).y + opponentReach + state.conf.paddleHeight * 0.5;

            double tmpBotValue = +(opponentBot - ballMemory.y) / (state.conf.paddleHeight * 0.5);
            double tmpTopValue = +(opponentTop - ballMemory.y) / (state.conf.paddleHeight * 0.5);

            // TODO: Review the range checking code.

            /*
            if(tmpBotValue > +1.0) {
                System.out.println("bot: " + ", reach: " + opponentReach + "ball: " + ballMemory.y + ", opponentTop: " + opponentBot + ", tmpTop: " + tmpBotValue);
            }
            if(tmpTopValue < -1.0) {
                System.out.println("top: " + tmpTopValue + ", reach: " + opponentReach + "ball: " + ballMemory.y + ", opponentTop: " + opponentTop + ", tmpTop: " + tmpTopValue);
            }
            */

            Vector3 opponentBestMove = offensiveEval(bot, state, PongGameBot.PlayerSide.getOtherSide(catcher), ballMemory, ballMemory2, tmpBotValue, tmpTopValue);

            double score = -opponentBestMove.z;

            if(score > minScore) {
                minScore = score;
                minTarget = tmpTarget;
                minTargetPos = targetPos - tmpTarget * state.conf.paddleHeight * 0.5;
            }
        }

        // if score < 0, opponent can make a winning move now.
        // otherwise we should be able to counter anything.
        return new Vector3(minTargetPos, minTarget, minScore);
    }

}

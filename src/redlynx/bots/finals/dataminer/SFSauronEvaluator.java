package redlynx.bots.finals.dataminer;

import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector3;

public class SFSauronEvaluator {


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

        double[] botScores = new double[8];
        double[] topScores = new double[8];

        for(int i=0; i<=7; ++i) {
            botScores[i] = -1000;
            topScores[i] = -1000;
        }

        int pointer = 0;

        {
            for(int i=7; i<93; ++i) {
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
                double opponentBot = state.getPedal(catcher).y - opponentReach + state.conf.paddleHeight * 0.5;
                double opponentTop = state.getPedal(catcher).y + opponentReach + state.conf.paddleHeight * 0.5;

                double tmpBotValue = +(opponentBot - tmpBall.y);
                double tmpTopValue = -(opponentTop - tmpBall.y);

                double botReach = +(opponentBot - tmpBall.y) / (state.conf.paddleHeight * 0.5);
                double topReach = +(opponentTop - tmpBall.y) / (state.conf.paddleHeight * 0.5);

                /*
                botReach = Math.max(-1, botReach);
                topReach = Math.min(+1, topReach);

                double possibleReturns = topReach - botReach;
                if(possibleReturns > 0 && possibleReturns < 0.3) {
                    // TODO: check what happens deeper in the game tree.
                    // NOTE: Should attribute opponents time from this round to my time for next, since it is forced.


                }
                */


                botScores[pointer] = tmpBotValue;
                topScores[pointer] = tmpTopValue;
                pointer = ++pointer & 7;

                // select minimum value from range.
                for(int k=0; k<=7; ++k) {
                    if(tmpBotValue > botScores[k]) tmpBotValue = botScores[k];
                    if(tmpTopValue > topScores[k]) tmpTopValue = topScores[k];
                }

                if(tmpBotValue > botValue) {
                    botValue = tmpBotValue;
                    paddleTargetBot = (i - 3 - 50) / 50.0;
                }

                if(tmpTopValue > topValue) {
                    topValue = tmpTopValue;
                    paddleTargetTop = (i - 3 - 50) / 50.0;
                }
            }
        }

        double bestValue = (botValue > topValue) ? botValue : topValue;

        // found a solid attack move! lets do that.
        double paddleTarget = (botValue > topValue) ? paddleTargetBot : paddleTargetTop;
        targetPos -= paddleTarget * state.conf.paddleHeight * 0.5;

        // bind target position inside play area (should not be necessary anymore, as the filtering is done before analyzing)
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

        double[] scoresSlidingWindow = new double[8];

        for(int i=0; i<=7; ++i) {
            scoresSlidingWindow[i] = -1000;
        }

        int pointer = 0;

        //for(int i=3; i<=47; ++i) {
        //    double tmpTarget = (i - 25) / 25.0;
        for(int i=7; i<93; ++i) {
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

            ballMemory.copy(tmpBall, true);
            bot.ballCollideToPaddle(tmpTarget, ballMemory);

            // should take extra defense time into account somehow?
            double defenseTime = PongUtil.simulate(ballMemory, state.conf);

            // which returns are possible for the opponent?
            // Vector2 possibleReturns = bot.getPaddlePossibleReturns(state, ballMemory, BaseBot.PlayerSide.getOtherSide(catcher), defenseTime);
            double opponentReach = defenseTime * bot.getPaddleMaxVelocity() + state.conf.paddleHeight * 0.5;
            double opponentBot = state.getPedal(catcher).y - opponentReach + state.conf.paddleHeight * 0.5;
            double opponentTop = state.getPedal(catcher).y + opponentReach + state.conf.paddleHeight * 0.5;

            double botReach = +(opponentBot - ballMemory.y) / (state.conf.paddleHeight * 0.5);
            double topReach = +(opponentTop - ballMemory.y) / (state.conf.paddleHeight * 0.5);

            Vector3 opponentBestMove = offensiveEval(bot, state, PongGameBot.PlayerSide.getOtherSide(catcher), ballMemory, ballMemory2, botReach, topReach);
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

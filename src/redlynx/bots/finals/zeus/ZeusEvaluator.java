package redlynx.bots.finals.zeus;

import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.PongGameBot.PlayerSide;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector3;

public class ZeusEvaluator {


    private ClientGameState.Ball ballMemory = new ClientGameState.Ball();
    private ClientGameState.Ball ballMemory2 = new ClientGameState.Ball();
    private ClientGameState.Ball ballMemory3 = new ClientGameState.Ball();
    private ClientGameState.Ball ballMemory4 = new ClientGameState.Ball();

    public Vector3 myOffensiveEval(double timeLeft, PongGameBot bot, ClientGameState state, PongGameBot.PlayerSide catcher, double catcherPos, ClientGameState.Ball collidingBallState, ClientGameState.Ball tmpBall, double minVal, double maxVal) {

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

        double paddleReach = state.conf.paddleHeight * 0.5+state.conf.ballRadius;

        {
            for(int i=3; i<97; ++i) {
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

                double opponentTime = PongUtil.simulate(tmpBall, state.conf)+timeLeft;
                double opponentReach = opponentTime * bot.getPaddleMaxVelocity() + paddleReach;
                double opponentBot = catcherPos- opponentReach + state.conf.paddleHeight * 0.5;
                double opponentTop = catcherPos + opponentReach + state.conf.paddleHeight * 0.5;

                double tmpBotValue = +(opponentBot - tmpBall.y);
                double tmpTopValue = -(opponentTop - tmpBall.y);

                double missileTime = bot.missileCommand.getMissileTime();
                double missileDistance = Math.abs(collidingBallState.y - tmpBall.y);
                double moveTime = opponentTime - missileTime - 0.1 - timeLeft;
                if(bot.hasMissiles() && moveTime * bot.getPaddleMaxVelocity() > missileDistance) {
                    tmpTopValue = 1000000 + moveTime * bot.getPaddleMaxVelocity(); // should be an easy win.
                    tmpBotValue = 1000000 + moveTime * bot.getPaddleMaxVelocity(); // should be an easy win.
                }

                botScores[pointer] = tmpBotValue;
                topScores[pointer] = tmpTopValue;
                pointer = ++pointer & 3;

                // select minimum value from range.
                for(int k=0; k<=3; ++k) {
                    if(tmpBotValue > botScores[k]) tmpBotValue = botScores[k];
                    if(tmpTopValue > topScores[k]) tmpTopValue = topScores[k];
                }

                if(tmpBotValue > botValue) {
                    botValue = tmpBotValue;
                    paddleTargetBot = (i - 1 - 50) / 50.0;
                }

                if(tmpTopValue > topValue) {
                    topValue = tmpTopValue;
                    paddleTargetTop = (i - 1 - 50) / 50.0;
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

    public Vector3 oppOffensiveEval( PongGameBot bot, ClientGameState state, PongGameBot.PlayerSide catcher, double catcherPos, ClientGameState.Ball collidingBallState, ClientGameState.Ball tmpBall, double minVal, double maxVal) {
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

        double paddleReach = state.conf.paddleHeight * 0.5+state.conf.ballRadius;

        {
            for(int i=3; i<97; ++i) {
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
                double opponentReach = opponentTime * bot.getPaddleMaxVelocity() + paddleReach;
                double opponentBot = catcherPos- opponentReach + state.conf.paddleHeight * 0.5;
                double opponentTop = catcherPos + opponentReach + state.conf.paddleHeight * 0.5;

                double tmpBotValue = +(opponentBot - tmpBall.y);
                double tmpTopValue = -(opponentTop - tmpBall.y);

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
    public Vector3 defensiveEval(double timeLeft, int depth, PongGameBot bot, ClientGameState state, PongGameBot.PlayerSide catcher, double minVal, double maxVal, ClientGameState.Ball tmpBall) {

        double targetPos = tmpBall.y - state.conf.paddleHeight * 0.5;
        double paddleMaxPos = state.conf.maxHeight - state.conf.paddleHeight;
        double paddleMinPos = 0;

        double minScore = -10000;
        double minTarget = 0;
        double minTargetPos = targetPos;

        for(int i=5; i<=95; i+=1) {
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
            double defenseTime = PongUtil.simulate(ballMemory, state.conf)+timeLeft;

            // which returns are possible for the opponent?
            // Vector2 possibleReturns = bot.getPaddlePossibleReturns(state, ballMemory, PongGameBot.PlayerSide.getOtherSide(catcher), defenseTime);
            double opponentReach = defenseTime * bot.getPaddleMaxVelocity() + state.conf.paddleHeight * 0.5;
            double opponentBot = state.getPedal(catcher).y - opponentReach + state.conf.paddleHeight * 0.5;
            double opponentTop = state.getPedal(catcher).y + opponentReach + state.conf.paddleHeight * 0.5;

            double botReach = +(opponentBot - ballMemory.y) / (state.conf.paddleHeight * 0.5);
            double topReach = +(opponentTop - ballMemory.y) / (state.conf.paddleHeight * 0.5);


            double myPos = state.getPedal(PongGameBot.PlayerSide.getOtherSide(catcher)).y;
            Vector3 opponentBestMove = oppOffensiveEval(bot, state, PongGameBot.PlayerSide.getOtherSide(catcher), myPos, ballMemory, ballMemory2, botReach, topReach);
            double score = -opponentBestMove.z;

            double k = Math.abs(ballMemory.vy/ballMemory.vx);
            if (ballMemory.y < state.conf.paddleHeight*0.3 && ballMemory.y > state.conf.paddleHeight*0.1 && ballMemory.vy < 0 && k < 1.3) {
                //System.out.println("Preferred target 1!");
                score += 50;
            }
            else if (ballMemory.y > state.conf.maxHeight-  state.conf.paddleHeight*0.3 && ballMemory.y < state.conf.maxHeight-state.conf.paddleHeight*0.1 && ballMemory.vy > 0 && k < 1.3) {
                //System.out.println("Preferred target 2!");
                score += 50;
            }


            if(score > minScore) {

                ballMemory3.copy(ballMemory, true);
                minScore = score;
                minTarget = tmpTarget;
                minTargetPos = targetPos - tmpTarget * state.conf.paddleHeight * 0.5;
            }
        }

        if (depth < 1 && minScore > 100) {
            ballMemory4.copy(ballMemory3, true);
            ballMemory.copy(tmpBall, true);
            bot.ballCollideToPaddle(minTarget, ballMemory);
            double defenceTime = PongUtil.simulate(ballMemory, state.conf)+timeLeft;

            // which returns are possible for the opponent?
            // Vector2 possibleReturns = bot.getPaddlePossibleReturns(state, ballMemory, PongGameBot.PlayerSide.getOtherSide(catcher), defenseTime);
            double opponentReach = defenceTime * bot.getPaddleMaxVelocity() + state.conf.paddleHeight * 0.5;
            double opponentBot = state.getPedal(catcher).y - opponentReach + state.conf.paddleHeight * 0.5;
            double opponentTop = state.getPedal(catcher).y + opponentReach + state.conf.paddleHeight * 0.5;
            double botReach = +(opponentBot - ballMemory.y) / (state.conf.paddleHeight * 0.5);
            double topReach = +(opponentTop - ballMemory.y) / (state.conf.paddleHeight * 0.5);

            Vector3 opponentBestDefensiveMove = defensiveEval(defenceTime, depth+1, bot, state, PongGameBot.PlayerSide.getOtherSide(catcher), botReach, topReach, ballMemory4);
            double score = -opponentBestDefensiveMove.z;
            if (score > minScore) {
                System.out.println("found a good defensive shot!");
                return new Vector3(minTargetPos, minTarget, score);
            }
        }

        // if score < 0, opponent can make a winning move now.
        // otherwise we should be able to counter anything.
        return new Vector3(minTargetPos, minTarget, minScore);
    }

}

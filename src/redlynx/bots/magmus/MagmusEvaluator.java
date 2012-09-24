package redlynx.bots.magmus;

import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.client.PongGameBot;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector3;

import java.util.ArrayList;
import java.awt.Color;

public class MagmusEvaluator {


    private final Vector3 offensiveReturnValue = new Vector3(0, 0, 0);
    private final Vector3 defensiveReturnValue = new Vector3(0, 0, 0);

    private ClientGameState.Ball ballMemory = new ClientGameState.Ball();
    private ClientGameState.Ball ballMemory2 = new ClientGameState.Ball();

    // TODO: Value of paddle choice i should be the minimum value of (i-1, i, i+1) to account for error in the model.
    public Vector3 offensiveEval(PongGameBot bot, ClientGameState state, PongGameBot.PlayerSide catcher, ClientGameState.Ball collidingBallState, ClientGameState.Ball tmpBall, double minVal, double maxVal, boolean makeDefEval, ArrayList<UILine> lines, Color color) {
        double targetPos = collidingBallState.y - state.conf.paddleHeight * 0.5;
        double botValue = -10000;
        double topValue = -10000;
        double paddleTargetBot = 0;
        double paddleTargetTop = 0;
        double paddleMaxPos = state.conf.maxHeight - state.conf.paddleHeight;
        double paddleMinPos = 0;

        double bestDefVal = -10000;
        double bestDefPaddle = 0;

        {
            for(int i=2; i<=18; ++i) {
                double tmpTarget = (i - 10) / 10.0;
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

                double opponentTime = PongUtil.simulateNew(tmpBall, state.conf, lines, color);
                double opponentReach = opponentTime * bot.getPaddleMaxVelocity() + state.conf.paddleHeight * 0.5;
                double opponentBot = state.getPedal(catcher).y - opponentReach + state.conf.paddleHeight * 0.5;
                double opponentTop = state.getPedal(catcher).y + opponentReach + state.conf.paddleHeight * 0.5;

                double tmpBotValue = +(opponentBot - tmpBall.y);
                double tmpTopValue = -(opponentTop - tmpBall.y);

                if(makeDefEval) {
                    double offensiveScore = Math.max(tmpBotValue, tmpTopValue);
                    double defValue = defensiveEval(bot, state, catcher, minVal, maxVal, tmpBall, 0).z;

                    if(defValue < 0)
                        defValue = -100000000;
                    else {
                        defValue += offensiveScore;
                    }

                    if(defValue > bestDefVal) {
                        bestDefVal = defValue;
                        bestDefPaddle = tmpTarget;
                    }
                }

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

        // if no offense availabe, return def
        if(makeDefEval && bestValue < 50) {
            offensiveReturnValue.x = targetPos - bestDefPaddle * state.conf.paddleHeight * 0.5;
            offensiveReturnValue.y = bestDefPaddle;
            offensiveReturnValue.z = bestValue;
            return offensiveReturnValue;
        }

        // found a solid attack move! lets do that.
        double paddleTarget = (botValue > topValue) ? paddleTargetBot : paddleTargetTop;
        targetPos -= paddleTarget * state.conf.paddleHeight * 0.5;

        // bind target position inside play area
        targetPos = targetPos < paddleMinPos ? paddleMinPos : targetPos;
        targetPos = targetPos > paddleMaxPos ? paddleMaxPos : targetPos;

        offensiveReturnValue.x = targetPos;
        offensiveReturnValue.y = paddleTarget;
        offensiveReturnValue.z = bestValue;
        return offensiveReturnValue;
    }


    // defensive eval tries to minimize opponents offensive eval.
    public Vector3 defensiveEval(PongGameBot bot, ClientGameState state, PongGameBot.PlayerSide catcher, double minVal, double maxVal, ClientGameState.Ball tmpBall, int depth) {

        double targetPos = tmpBall.y - state.conf.paddleHeight * 0.5;
        double paddleMaxPos = state.conf.maxHeight - state.conf.paddleHeight;
        double paddleMinPos = 0;

        double minScore = -10000;
        double minTarget = 0;
        double minTargetPos = targetPos;

        for(int i=2; i<=18; ++i) {
            double tmpTarget = (i - 10) / 10.0;
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

            Vector3 opponentBestMove;
            if(depth == 0) {
                opponentBestMove = offensiveEval(bot, state, PongGameBot.PlayerSide.getOtherSide(catcher), ballMemory, ballMemory2, tmpBotValue, tmpTopValue, false, null, null);
            }
            else {
                ClientGameState.Ball ball = new ClientGameState.Ball();
                ball.copy(ballMemory, true);
                opponentBestMove = defensiveEval(bot, state, PongGameBot.PlayerSide.getOtherSide(catcher), tmpBotValue, tmpTopValue, ball, depth - 1);
            }

            bot.ballCollideToPaddle(opponentBestMove.y, ballMemory);
            double score = -opponentBestMove.z * (Math.abs(ballMemory.vy / ballMemory.vx));

            if(score > minScore) {
                minScore = score;
                minTarget = tmpTarget;
                minTargetPos = targetPos - tmpTarget * state.conf.paddleHeight * 0.5;
            }
        }

        // if score < 0, opponent can make a winning move now.
        // otherwise we should be able to counter anything.
        defensiveReturnValue.x = minTargetPos;
        defensiveReturnValue.y = minTarget;
        defensiveReturnValue.z = minScore;
        return defensiveReturnValue;
    }

}

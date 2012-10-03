package redlynx.bots.finals.sauron;

import redlynx.bots.finals.zeus.ZeusEvaluator;
import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.ui.UILine;
import redlynx.pong.ui.UIString;
import redlynx.pong.util.*;
import sun.beans.editors.StringEditor;

import java.awt.Color;

public class DecisionMaker {
    private final FinalSauron finalSauron;
    private final ZeusEvaluator evaluator;
    private final ClientGameState.Ball tmpBall = new ClientGameState.Ball();
    private double timeLeft = 0;
    private String kekkonen = new String();

    public DecisionMaker(FinalSauron finalSauron) {
        this.finalSauron = finalSauron;
        this.evaluator = new ZeusEvaluator();

        finalSauron.strings.add(new UIString(kekkonen, new Vector2i(0, -15), Color.magenta));
    }

    public double decisionMakerMyTurn(ClientGameState newStatus) {
        // find out impact velocity and position.
        ClientGameState.Ball ballWorkMemory = finalSauron.getBallWorkMemory();
        finalSauron.getBallWorkMemory().copy(newStatus.ball, true);
        finalSauron.getBallWorkMemory().setVelocity(finalSauron.getBallVelocity());
        timeLeft = (PongUtil.simulateOld(finalSauron.getBallWorkMemory(), finalSauron.getLastKnownStatus().conf, finalSauron.getLines(), Color.green));
        finalSauron.getMissileCommand().fireDefensiveMissiles(timeLeft, finalSauron.getBallWorkMemory());

        double requiredVelocityForMissiles = 100;

        Vector2 reach = finalSauron.getPaddlePossibleReturns(newStatus, finalSauron.getBallWorkMemory(), PongGameBot.PlayerSide.LEFT, timeLeft);
        double minReach = Math.max(-0.9, reach.x);
        double maxReach = Math.min(+0.9, reach.y);

        {
            // hack.. if angle is high, don't try to hit the ball with the wrong end of the paddle..
            double value = ballWorkMemory.vy / (Math.abs(ballWorkMemory.vx) + 0.000001);
            double pixelsPerTickEstimate = Math.abs((ballWorkMemory.vy) / (1000.0 / 21));
            double safelimit = (pixelsPerTickEstimate+1)/(finalSauron.lastKnownStatus.conf.paddleHeight/2);
            double amount = safelimit;

            if(value < 0.0 && minReach < -1+amount) {
                minReach = -1+amount;
            }
            else if(value > 0.0 && maxReach > 1-amount) {
                maxReach = +1-amount;
            }
        }


        // this is the expected y value when colliding against our paddle.
        // Vector3 target = evaluator.offensiveEval(finalSauron, newStatus, PongGameBot.PlayerSide.RIGHT, finalSauron.getBallWorkMemory(), tmpBall, minReach, maxReach);
        Vector3 target = evaluator.myOffensiveEval(timeLeft, finalSauron, newStatus, PongGameBot.PlayerSide.RIGHT, newStatus.right.y + newStatus.conf.paddleHeight * 0.5, finalSauron.getBallWorkMemory(), tmpBall, minReach, maxReach);

        // when no winning move available, use defense
        if (target.z < 8 * newStatus.conf.ballRadius) {
            double offenseScore = target.z;
            finalSauron.getBallWorkMemory().copy(newStatus.ball, true);
            finalSauron.getBallWorkMemory().setVelocity(finalSauron.getBallVelocity());
            timeLeft = (PongUtil.simulateOld(finalSauron.getBallWorkMemory(), finalSauron.getLastKnownStatus().conf, finalSauron.getLines(), Color.green));
            target = evaluator.defensiveEval(0, 0, finalSauron, finalSauron.getLastKnownStatus(), PongGameBot.PlayerSide.RIGHT, minReach, maxReach, finalSauron.getBallWorkMemory());
            kekkonen = "Def: " + ((int)(target.z)) + ", Off: " + ((int)offenseScore);
        }
        else {
            kekkonen = "Off: " + ((int)(target.z));
        }

        finalSauron.strings.remove(finalSauron.strings.size()-1);
        finalSauron.strings.add(new UIString(kekkonen, new Vector2i(20, -10), Color.magenta));

        if(finalSauron.getMissileCommand().getPlan() != null) {
            System.out.println("Following MissileCommand plan!");
            target.copy(finalSauron.getMissileCommand().getPlan());
        }

        {
            // time to target, time for missile. missile launcher reach.
            double halfPaddle = 0.5 * newStatus.conf.paddleHeight;
            double myPos = newStatus.left.y + halfPaddle;
            double distanceToTarget = Math.abs(target.x - myPos);
            double timeToTarget = distanceToTarget / finalSauron.getPaddleMaxVelocity();
            double timeForMissile = (timeLeft - timeToTarget) * 0.5;
            finalSauron.getLines().add(new UILine(12, myPos - halfPaddle - timeForMissile * finalSauron.getPaddleMaxVelocity(), 12, myPos + halfPaddle + timeForMissile * finalSauron.getPaddleMaxVelocity(), Color.magenta));

            // see if should make an offensive missile shot with current plan.
            tmpBall.copy(finalSauron.getBallWorkMemory(), true);
            finalSauron.ballCollideToPaddle(target.y, tmpBall);
            double opponentTime = PongUtil.simulateNew(tmpBall, finalSauron.getLastKnownStatus().conf, null, null) + timeLeft;
            requiredVelocityForMissiles = finalSauron.getMissileCommand().fireOffensiveMissiles(timeForMissile, opponentTime, tmpBall, target);

            Visualisation.visualizeOpponentReach(finalSauron.getLines(), finalSauron, opponentTime);
        }

        double minVal = finalSauron.getLastKnownStatus().conf.ballRadius;
        double maxVal = finalSauron.getLastKnownStatus().conf.maxHeight - finalSauron.getLastKnownStatus().conf.paddleHeight - finalSauron.getLastKnownStatus().conf.ballRadius;

        if (target.x < minVal) {
            target.x = minVal;
        }
        if (target.x > maxVal) {
            target.x = maxVal;
        }

        double targetPos = target.x;
        double paddleTarget = target.y;

        // draw stuff on the hud.
        finalSauron.getSauronVisualiser().visualiseModel(0, finalSauron.getLastKnownStatus().getPedal(PongGameBot.PlayerSide.LEFT).y, minReach, maxReach);
        finalSauron.getSauronVisualiser().visualisePlan(paddleTarget, Color.red);
        finalSauron.getSauronVisualiser().visualisePlan(0, Color.green);

        if(requiredVelocityForMissiles * requiredVelocityForMissiles > 1) {
            if (finalSauron.getMyState().catching()) {
                double myPos = newStatus.left.y;
                double distance = (targetPos - myPos);
                if (finalSauron.needToReact(targetPos, timeLeft) || finalSauron.reallyShouldUpdateRegardless()) {
                    finalSauron.changeCourse(distance, timeLeft);
                }
            }
        }
        else {
            // MissileCommand has issued a move order! Apply!
            System.out.println("Following MissileCommand's orders!");
            finalSauron.changeCourse(100.0, 100.0 / requiredVelocityForMissiles);
            // finalSauron.requestChangeSpeed(requiredVelocityForMissiles);
        }
        return timeLeft;
    }

    public double decisionMakerOpponentsTurn(ClientGameState newStatus) {
        // simulate twice, once there, and then back.
        finalSauron.getBallWorkMemory().copy(newStatus.ball, true);
        finalSauron.getBallWorkMemory().setVelocity(finalSauron.getBallVelocity());

        timeLeft = (PongUtil.simulateNew(finalSauron.getBallWorkMemory(), finalSauron.getLastKnownStatus().conf, finalSauron.getLines(), Color.green));
        Vector2 reach = finalSauron.getPaddlePossibleReturns(newStatus, finalSauron.getBallWorkMemory(), PongGameBot.PlayerSide.RIGHT, timeLeft);

        Visualisation.visualizeOpponentReach(finalSauron.getLines(), finalSauron, timeLeft);

        // add an extra ten percent, just to be sure.
        double minReach = reach.x - 0.1;
        double maxReach = reach.y + 0.1;

        // this is the current worst case. should try to cover that?
        Vector3 target = evaluator.oppOffensiveEval(finalSauron, newStatus, PongGameBot.PlayerSide.LEFT, newStatus.left.y + 0.5 * newStatus.conf.paddleHeight, finalSauron.getBallWorkMemory(), tmpBall, minReach, maxReach);
        finalSauron.getMissileCommand().fireOffensiveMissiles(0, timeLeft, finalSauron.getBallWorkMemory(), target);
        finalSauron.getMissileCommand().unCommit();
        double paddleTarget = target.y;

        // if no return is possible according to simulation. Then the least impossible return should be anticipated..
        if (target.z < -1000) {
            double deltaPaddle = (finalSauron.getBallWorkMemory().y - newStatus.right.y);
            if (deltaPaddle > 0) {
                // paddle is below ball. anticipate a high return.
                paddleTarget = +1;
            } else {
                // paddle above ball. anticipate a low return.
                paddleTarget = -1;
            }
        }

        // draw stuff on the hud.
        finalSauron.getSauronVisualiser().visualiseModel(finalSauron.getLastKnownStatus().conf.maxWidth, finalSauron.getLastKnownStatus().getPedal(PongGameBot.PlayerSide.RIGHT).y, minReach, maxReach);
        finalSauron.getSauronVisualiser().visualisePlan(paddleTarget, Color.red);
        finalSauron.getSauronVisualiser().visualisePlan(0, Color.green);

        finalSauron.ballCollideToPaddle(paddleTarget, finalSauron.getBallWorkMemory());
        double timeLeftAfter = PongUtil.simulateOld(finalSauron.getBallWorkMemory(), finalSauron.getLastKnownStatus().conf, finalSauron.getLines(), Color.red);
        timeLeft = (timeLeft + timeLeftAfter);

        // now we are done.
        ClientGameState.Player myPedal = finalSauron.getLastKnownStatus().getPedal(finalSauron.getMySide());
        double diff_y = finalSauron.getBallWorkMemory().y - myPedal.y;

        finalSauron.changeCourse(diff_y * 10000, timeLeft);
        return timeLeft;
    }
}
package redlynx.bots.finals.sauron;

import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.util.Vector3;
import redlynx.pong.util.Visualisation;

import java.awt.*;

public class MissileCommand {
    private final PongGameBot bot;
    private final Vector3 plan = new Vector3(0, 0, 0); // store plan in case missile firing is based on a plan.
    private boolean committedToPlan = false;
    private double missileTime = 1.5;
    private long lastTimeFiredSlowDownMissile = 0;

    public MissileCommand(PongGameBot bot) {
        this.bot = bot;
    }

    public double getMissileTime() {
        return missileTime;
    }

    public void setMissileTime(double time) {
        missileTime = time;
    }

    public void onGameOver() {
        committedToPlan = false;
    }

    public Vector3 getPlan() {
        /*
        if(committedToPlan)
            return plan;
        return null;
        */

        return null;
    }

    public void fireDefensiveMissiles(double timeLeft, ClientGameState.Ball ball) {
        // should prevent opponent from being able to fire killshot missiles at us.
    }

    public void unCommit() {
        committedToPlan = false;
    }


    public double fireKillShotMissiles(double timeForMissile, double timeLeft, ClientGameState.Ball ballWorkMemory) {

        if(!bot.hasMissiles())
            return 100;

        double halfPaddle = bot.getLastKnownStatus().conf.paddleHeight * 0.5;
        double ballPos = ballWorkMemory.y;
        if(ballPos < bot.lastKnownStatus.conf.paddleHeight) ballPos = bot.lastKnownStatus.conf.paddleHeight;
        if(ballPos > bot.lastKnownStatus.conf.maxHeight - bot.lastKnownStatus.conf.paddleHeight) ballPos = bot.lastKnownStatus.conf.maxHeight - bot.lastKnownStatus.conf.paddleHeight;
        double ballMe = ballPos + 1.5 * (ballWorkMemory.vy * bot.lastKnownStatus.conf.tickInterval / 1000.0) - bot.getLastKnownStatus().left.y - halfPaddle;

        double minimumError = 1000;
        double bestVelocity = 0;
        double bestI = 0;

        for(double i=0.05; i<timeForMissile; i+=0.05) {
            double requiredVelocity = ballMe / i / bot.getPaddleMaxVelocity();
            if(requiredVelocity * requiredVelocity > 1 && Math.abs(ballMe) > bot.lastKnownStatus.conf.paddleHeight * 0.10)
                continue;

            if(Math.abs(ballMe) < bot.lastKnownStatus.conf.paddleHeight * 0.10)
                requiredVelocity *= 0.1;

            double timeError = timeLeft - i - missileTime;

            if(timeError * timeError < minimumError) {
                minimumError = timeError * timeError;
                bestVelocity = requiredVelocity;
                bestI = i;
            }
        }

        if(minimumError < 0.2 * 0.2) {
            if(bestI > 0.11) {
                // need to move! return the move velocity!
                Visualisation.drawVector(bot.lines, Color.red, 0, ballWorkMemory.y, 100, 0);
                return bestVelocity;
            }
            else {
                if(bot.fireMissile()) {
                    lastTimeFiredSlowDownMissile = System.currentTimeMillis();
                    System.out.println("Firing killer missile at ball destination!");
                }
            }
        }

        return 100;
    }

    public double fireOffensiveMissiles(double timeLeft, ClientGameState.Ball ballWorkMemory, Vector3 currentPlan) {

        if(!bot.hasMissiles())
            return 100;

        double halfPaddle = bot.getLastKnownStatus().conf.paddleHeight * 0.5;
        double ballMe = ballWorkMemory.y - bot.getLastKnownStatus().left.y - halfPaddle;
        double ballHim = ballWorkMemory.y - bot.getLastKnownStatus().right.y - halfPaddle;
        double paddleDistance = Math.abs(bot.getLastKnownStatus().left.y - bot.getLastKnownStatus().right.y);
        double idealDistance = missileTime * bot.getPaddleMaxVelocity();
        double error = paddleDistance - idealDistance;
        error *= error;

        if(bot.hasMissiles()) {
            if (ballHim * ballMe > 0 && Math.abs(ballHim) > Math.abs(ballMe)) {
                // opponent must cross us before he can reach the ball destination.
                if (error < bot.getLastKnownStatus().conf.paddleHeight * bot.getLastKnownStatus().conf.paddleHeight / 16.0) {

                    double actualTimeLeft = timeLeft - bot.getPaddleMaxVelocity() * 25;
                    double actualReach = actualTimeLeft * bot.getPaddleMaxVelocity() + bot.getLastKnownStatus().conf.paddleHeight * 0.5;
                    double actualPos = bot.getLastKnownStatus().right.y + bot.getLastKnownStatus().conf.paddleHeight * 0.5;
                    double botReach = actualPos - actualReach;
                    double topReach = actualPos + actualReach;

                    if (ballWorkMemory.y < botReach + 10 || ballWorkMemory.y > topReach - 10) {
                        if (bot.fireMissile()) {
                            lastTimeFiredSlowDownMissile = System.currentTimeMillis();
                            System.out.println("Firing slowdown missile!");
                            plan.copy(currentPlan);
                            committedToPlan = true;
                        }
                    }
                }
            }
        }

        // Empty entire missile salvo if get the chance to slowdown. Fires missiles at 200-300ms intervals.
        if(bot.hasMissiles() && System.currentTimeMillis() - lastTimeFiredSlowDownMissile < 300) {
            if (bot.fireMissile()) {
                lastTimeFiredSlowDownMissile = System.currentTimeMillis();
                System.out.println("DIE DIE DIE!");
                plan.copy(currentPlan);
                committedToPlan = true;
            }
        }

        if(ballWorkMemory.y < halfPaddle) {
            ballMe -= halfPaddle - ballWorkMemory.y;
        }
        if(ballWorkMemory.y > bot.lastKnownStatus.conf.maxHeight - halfPaddle) {
            ballMe += ballWorkMemory.y - (bot.lastKnownStatus.conf.maxHeight - halfPaddle);
        }

        double ball_vy = bot.getLastKnownStatus().left.y - bot.getLastKnownStatus().right.y;
        if(ball_vy * ball_vy < 25 * 25) {
            // assume static ballspeed & continued vaakapallo.
            double bv = bot.lastKnownStatus.ball.vx;
            double pos = bot.lastKnownStatus.ball.x;

            double totalTime = 0;
            while(totalTime < bot.missileCommand.getMissileTime()) {
                if(bv < 0) {
                    totalTime += Math.abs(pos / bv);
                    pos = 10;
                    bv *= -1;
                }
                else {
                    totalTime += (bot.lastKnownStatus.conf.maxWidth - pos) / bv;
                    pos = bot.lastKnownStatus.conf.maxWidth - 10;
                    bv *= -1;

                    double vaakaPalloError = totalTime - bot.missileCommand.getMissileTime();
                    if(vaakaPalloError * vaakaPalloError < 0.1 * 0.1) {
                        if(bot.fireMissile()) {
                            System.out.println("Destroying vaakapallo players!");
                        }
                    }
                }
            }

        }

        return 100;
    }
}
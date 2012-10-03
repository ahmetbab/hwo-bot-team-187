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
        double ballMe = ballPos - bot.getLastKnownStatus().left.y - halfPaddle;

        double minimumError = 1000;
        double bestVelocity = 0;
        double bestI = 0;

        for(double i=0.05; i<timeForMissile; i+=0.05) {
            double requiredVelocity = ballMe / i / bot.getPaddleMaxVelocity();
            if(requiredVelocity * requiredVelocity > 1)
                continue;

            double timeError = timeLeft - i - missileTime;

            if(timeError * timeError < minimumError) {
                minimumError = timeError * timeError;
                bestVelocity = requiredVelocity;
                bestI = i;
            }
        }

        if(minimumError < 0.2 * 0.2) {
            // success!
            if(bestI > 0.21) {
                // need to move! return the move velocity!
                Visualisation.drawVector(bot.lines, Color.red, 0, ballWorkMemory.y, 100, 0);
                System.out.println("Moving to missile launch position!");
                return bestVelocity;
            }
            else {
                if(bot.fireMissile()) {
                    System.out.println("Firing killer missile at ball destination!");
                }
            }
        }

        return 100;
    }

    public double fireOffensiveMissiles(double timeLeft, ClientGameState.Ball ballWorkMemory, Vector3 currentPlan) {

        double halfPaddle = bot.getLastKnownStatus().conf.paddleHeight * 0.5;
        double ballMe = ballWorkMemory.y - bot.getLastKnownStatus().left.y - halfPaddle;
        double ballHim = ballWorkMemory.y - bot.getLastKnownStatus().right.y - halfPaddle;
        double paddleDistance = Math.abs(bot.getLastKnownStatus().left.y - bot.getLastKnownStatus().right.y);
        double idealDistance = missileTime * bot.getPaddleMaxVelocity();
        double error = paddleDistance - idealDistance;
        error *= error;

        if(bot.getMissileCount() > 1) {
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
                            System.out.println("Firing slowdown missile!");
                            plan.copy(currentPlan);
                            committedToPlan = true;
                        }
                    }
                }
            }
        }

        if(ballWorkMemory.y < halfPaddle) {
            ballMe -= halfPaddle - ballWorkMemory.y;
        }
        if(ballWorkMemory.y > bot.lastKnownStatus.conf.maxHeight - halfPaddle) {
            ballMe += ballWorkMemory.y - (bot.lastKnownStatus.conf.maxHeight - halfPaddle);
        }

        if(bot.getMissileCount() > 1) {
            double ball_vy = bot.getLastKnownStatus().ball.vy;
            if(ball_vy * ball_vy + ballWorkMemory.vx * ballWorkMemory.vx > 500 * 500) {
                if(ball_vy * ball_vy < 20 * 20) {
                    // very fast ball. just shoot and hope it hits or something.
                    if(bot.fireMissile()) {
                        System.out.println("Launching missiles RAWR :>");
                    }
                }
            }
        }

        return 100;
    }
}
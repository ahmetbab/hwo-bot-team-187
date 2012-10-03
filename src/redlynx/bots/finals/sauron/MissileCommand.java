package redlynx.bots.finals.sauron;

import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.util.Vector3;

public class MissileCommand {
    private final FinalSauron finalSauron;
    private final Vector3 plan = new Vector3(0, 0, 0); // store plan in case missile firing is based on a plan.
    private boolean committedToPlan = false;

    public MissileCommand(FinalSauron finalSauron) {
        this.finalSauron = finalSauron;
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

    double fireOffensiveMissiles(double timeForMissile, double timeLeft, ClientGameState.Ball ballWorkMemory, Vector3 currentPlan) {

        double halfPaddle = finalSauron.getLastKnownStatus().conf.paddleHeight * 0.5;
        double ballMe = ballWorkMemory.y - finalSauron.getLastKnownStatus().left.y - halfPaddle;
        double ballHim = ballWorkMemory.y - finalSauron.getLastKnownStatus().right.y - halfPaddle;
        double paddleDistance = Math.abs(finalSauron.getLastKnownStatus().left.y - finalSauron.getLastKnownStatus().right.y);
        double idealDistance = 1.6 * finalSauron.getPaddleMaxVelocity();
        double error = paddleDistance - idealDistance;
        error *= error;

        if (ballHim * ballMe > 0 && Math.abs(ballHim) > Math.abs(ballMe)) {
            // opponent must cross us before he can reach the ball destination.
            if (error < finalSauron.getLastKnownStatus().conf.paddleHeight * finalSauron.getLastKnownStatus().conf.paddleHeight / 16.0) {

                double actualTimeLeft = timeLeft - finalSauron.getPaddleMaxVelocity() * 25;
                double actualReach = actualTimeLeft * finalSauron.getPaddleMaxVelocity() + finalSauron.getLastKnownStatus().conf.paddleHeight * 0.5;
                double actualPos = finalSauron.getLastKnownStatus().right.y + finalSauron.getLastKnownStatus().conf.paddleHeight * 0.5;
                double botReach = actualPos - actualReach;
                double topReach = actualPos + actualReach;

                if (ballWorkMemory.y < botReach + 10 || ballWorkMemory.y > topReach - 10) {
                    if (finalSauron.fireMissile()) {
                        System.out.println("Firing slowdown missile!");
                        plan.copy(currentPlan);
                        committedToPlan = true;
                    }
                }
            }
        }

        if(ballWorkMemory.y < halfPaddle) {
            ballMe -= halfPaddle - ballWorkMemory.y;
        }
        if(ballWorkMemory.y > finalSauron.lastKnownStatus.conf.maxHeight - halfPaddle) {
            ballMe += ballWorkMemory.y - (finalSauron.lastKnownStatus.conf.maxHeight - halfPaddle);
        }

        for(double i=0; i<timeForMissile; i+=0.05) {
            double requiredVelocity = ballMe / i;
            if(requiredVelocity * requiredVelocity > 1)
                continue;

            double timeError = timeLeft - i - 1.6;
            if(timeError * timeError < 0.2 * 0.2) {
                // success!
                if(i > 0.11) {
                    // need to move! return the move velocity!
                    System.out.println("Moving to missile launch position!");
                    plan.copy(currentPlan);
                    committedToPlan = true;
                    return requiredVelocity;
                }
                else {
                    if(finalSauron.fireMissile()) {
                        plan.copy(currentPlan);
                        committedToPlan = true;
                        System.out.println("Firing killer missile at ball destination!");
                    }
                }
            }
        }

        double ball_vy = finalSauron.getLastKnownStatus().ball.vy;
        if(ball_vy * ball_vy + ballWorkMemory.vx * ballWorkMemory.vx > 500 * 500) {
            // very fast ball. just shoot and hope it hits or something.
            if(finalSauron.fireMissile()) {
                System.out.println("Launching missiles RAWR :>");
            }
        }

        return 100;
    }
}
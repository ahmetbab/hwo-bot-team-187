package redlynx.bots.finals.sauron;

import redlynx.pong.client.state.ClientGameState;

public class MissileCommand {
    private final FinalSauron finalSauron;

    public MissileCommand(FinalSauron finalSauron) {
        this.finalSauron = finalSauron;
    }

    void fireDefensiveMissiles(double timeLeft, ClientGameState.Ball ball) {
        // should prevent opponent from being able to fire killshot missiles at us.
    }

    double fireOffensiveMissiles(double timeForMissile, double timeLeft, ClientGameState.Ball ballWorkMemory) {

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
                    return requiredVelocity;
                }
                else {
                    if(finalSauron.fireMissile()) {
                        System.out.println("Firing killer missile at ball destination!");
                    }
                }
            }
        }


        double ball_vy = finalSauron.getLastKnownStatus().ball.vy;
        if(ball_vy * ball_vy < 50) {
            System.out.println("Want to fire missiles just for lulz! :D");
            // Ball not moving up & down much. Just fire the missiles.
            if(ballMe * ballMe < halfPaddle * halfPaddle) {
                if(finalSauron.fireMissile()) {
                    System.out.println("Launching missiles RAWR :>");
                }
            }
        }

        return 100;
    }
}
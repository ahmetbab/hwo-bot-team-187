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

    void fireOffensiveMissiles(double timeLeft, ClientGameState.Ball ballWorkMemory) {

        double ballMe = ballWorkMemory.y - finalSauron.getLastKnownStatus().left.y;
        double ballHim = ballWorkMemory.y - finalSauron.getLastKnownStatus().right.y;
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

        double timeError = timeLeft - 1.6;
        if (timeError * timeError < 0.1) {
            if (ballMe * ballMe < 15 * 15) {
                if (finalSauron.fireMissile()) {
                    System.out.println("Firing missile at ball destination!");
                }
            }
        }
    }
}
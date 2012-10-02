package redlynx.bots.finals.deflector;

import redlynx.bots.preliminaries.dataminer.DataMinerModel;
import redlynx.bots.finals.DataCollector;
import redlynx.bots.finals.sauron.FinalSauronModel;
import redlynx.bots.finals.sauron.MissileDodger;
import redlynx.bots.finals.sauron.SauronState;
import redlynx.pong.client.Pong;
import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.collisionmodel.SFSauronGeneralModel;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.*;

import java.awt.*;
import java.util.ArrayList;


public class DeflectorBot extends PongGameBot {

    private final DataCollector dataCollector;

    public DeflectorBot() {
        super();

        dataCollector = new DataCollector(new DataMinerModel(new SFSauronGeneralModel()), true);

        FinalSauronModel model = new FinalSauronModel(this);
        myModel = model; // dataCollector.getModel();
        model.tweak();

        dataCollector.learnFromFile("pongdata.txt",1);
    }

	public static void main(String[] args) {
		Pong.init(args, new DeflectorBot());
	}

    private DeflectorEvaluator evaluator = new DeflectorEvaluator();
    private SauronState myState = new SauronState();
    private final ArrayList<UILine> lines = new ArrayList<UILine>();

    double timeLeft = 10000;
    private int numWins = 0;
    private int numGames = 0;

    private void fireDefensiveMissiles(double timeLeft, ClientGameState.Ball ball) {
        // should prevent opponent from being able to fire killshot missiles at us.
    }

    private void fireOffensiveMissiles(double timeLeft, ClientGameState.Ball ballWorkMemory) {

        double ballMe = ballWorkMemory.y - lastKnownStatus.left.y;
        double ballHim = ballWorkMemory.y - lastKnownStatus.right.y;
        double paddleDistance = Math.abs(lastKnownStatus.left.y - lastKnownStatus.right.y);
        double idealDistance = 1.6 * getPaddleMaxVelocity();
        double error = paddleDistance - idealDistance;
        error *= error;

        if(ballHim * ballMe > 0 && Math.abs(ballHim) > Math.abs(ballMe)) {
            // opponent must cross us before he can reach the ball destination.
            if(error < lastKnownStatus.conf.paddleHeight * lastKnownStatus.conf.paddleHeight / 16.0) {

                double actualTimeLeft = timeLeft - getPaddleMaxVelocity() * 25;
                double actualReach = actualTimeLeft * getPaddleMaxVelocity() + lastKnownStatus.conf.paddleHeight * 0.5;
                double actualPos = lastKnownStatus.right.y + lastKnownStatus.conf.paddleHeight * 0.5;
                double botReach = actualPos - actualReach;
                double topReach = actualPos + actualReach;

                if(ballWorkMemory.y < botReach + 10 || ballWorkMemory.y > topReach - 10) {
                    if(fireMissile()) {
                        System.out.println("Firing slowdown missile!");
                    }
                }
            }
        }

        double timeError = timeLeft - 1.6;
        if(timeError * timeError < 0.1) {
            if(ballMe * ballMe < 15 * 15) {
                if(fireMissile()) {
                    System.out.println("Firing missile at ball destination!");
                }
            }
        }
    }

    @Override
    public void onGameStateUpdate(ClientGameState newStatus) {

        lines.clear();
        double ball_direction = newStatus.ball.vx;

        if(getMySide().comingTowardsMe(ball_direction)) {
            // find out impact velocity and position.
            ballWorkMemory.copy(newStatus.ball, true);
            ballWorkMemory.setVelocity(getBallVelocity());
            timeLeft = PongUtil.simulateOld(ballWorkMemory, lastKnownStatus.conf, lines, Color.green);

            fireDefensiveMissiles(timeLeft, ballWorkMemory);

            Vector2 reach = getPaddlePossibleReturns(newStatus, ballWorkMemory, PlayerSide.LEFT, timeLeft);
            double minReach = Math.max(-0.9, reach.x);
            double maxReach = Math.min(+0.9, reach.y);

            {
                // hack.. if angle is high, don't try to hit the ball with the wrong end of the paddle..
                double value = ballWorkMemory.vy * 0.1;
                double amount = Math.min(0.5, value * value * 0.4);
                if(value < 0.0 && minReach < -1+amount) {
                    minReach = -1+amount;
                }
                else if(value > 0.0 && maxReach > 1-amount) {
                    maxReach = +1-amount;
                }
            }

            // this is the expected y value when colliding against our paddle.
            Vector3 target = evaluator.offensiveEval(this, newStatus, PlayerSide.RIGHT, ballWorkMemory, ballTemp, minReach, maxReach);

            // when no winning move available, use defense
            if(target.z < 100) {
                ballWorkMemory.copy(newStatus.ball, true);
                ballWorkMemory.setVelocity(getBallVelocity());
                timeLeft = PongUtil.simulateOld(ballWorkMemory, lastKnownStatus.conf, lines, Color.green);
                target = evaluator.defensiveEval(this, lastKnownStatus, PlayerSide.RIGHT, minReach, maxReach, ballWorkMemory);
            }


            // see if should make an offensive missile shot with current plan.
            ballTemp.copy(ballWorkMemory, true);
            ballCollideToPaddle(target.y, ballTemp);
            double opponentTime = PongUtil.simulateNew(ballTemp, lastKnownStatus.conf, null, null) + timeLeft;
            fireOffensiveMissiles(opponentTime, ballTemp);

            Visualisation.visualizeOpponentReach(lines, this, opponentTime);

            double minVal = lastKnownStatus.conf.ballRadius * 2 - 2;
            double maxVal = lastKnownStatus.conf.maxHeight - lastKnownStatus.conf.paddleHeight - 2 * lastKnownStatus.conf.ballRadius + 2;
            if(target.x < minVal) {
                target.x = minVal;
            }
            if(target.x > maxVal) {
                target.x = maxVal;
            }

            double targetPos = target.x;
            double paddleTarget = target.y;

            // draw stuff on the hud.
            visualiseModel(0, lastKnownStatus.getPedal(PlayerSide.LEFT).y, minReach, maxReach);
            visualisePlan(paddleTarget, Color.red);
            visualisePlan(0, Color.green);

            // check if we need to do something.
            if(myState.catching()) {
                double myPos = lastKnownStatus.getPedal(getMySide()).y;
                double distance = (targetPos - myPos);
                if(needToReact(targetPos) || reallyShouldUpdateRegardless()) {
                    changeCourse(distance);
                }
            }


            //data collecting
            {
                if(getBallPositionHistory().isReliable()) {

                    ClientGameState.Ball  ballCollision = new ClientGameState.Ball();
                    ballCollision.copy(newStatus.ball, true);
                    ballCollision.setVelocity(getBallVelocity());
                    double time = PongUtil.simulate(ballCollision, lastKnownStatus.conf);
                    dataCollector.prepareDataCollect(target, ballCollision);
                }
                else {
                    dataCollector.setCollisionPoint(target.y);
                }
            }
        }
        else {


            {
                // data collecting.
                if(getBallPositionHistory().isReliable() && !dataCollector.isLogged()) {
                    dataCollector.updateModel(newStatus, this);
                }
            }


            // simulate twice, once there, and then back.
            ballWorkMemory.copy(newStatus.ball, true);
            ballWorkMemory.setVelocity(getBallVelocity());

            timeLeft = PongUtil.simulateOld(ballWorkMemory, lastKnownStatus.conf, lines, Color.green);
            Vector2 reach = getPaddlePossibleReturns(newStatus, ballWorkMemory, PlayerSide.RIGHT, timeLeft);

            fireOffensiveMissiles(timeLeft, ballWorkMemory);

            Visualisation.visualizeOpponentReach(lines, this, timeLeft);

            // add an extra ten percent, just to be sure.
            double minReach = reach.x - 0.1;
            double maxReach = reach.y + 0.1;

            // this is the current worst case. should try to cover that?
            Vector3 target = evaluator.offensiveEval(this, newStatus, PlayerSide.LEFT, ballWorkMemory, ballTemp, minReach, maxReach);
            double paddleTarget = target.y;

            // if no return is possible according to simulation. Then the least impossible return should be anticipated..
            if(target.z < -1000) {
                double deltaPaddle = (ballWorkMemory.y - newStatus.right.y);
                if(deltaPaddle > 0) {
                    // paddle is below ball. anticipate a high return.
                    paddleTarget = +1;
                }
                else {
                    // paddle above ball. anticipate a low return.
                    paddleTarget = -1;
                }
            }

            // draw stuff on the hud.
            visualiseModel(lastKnownStatus.conf.maxWidth, lastKnownStatus.getPedal(PlayerSide.RIGHT).y, minReach, maxReach);
            visualisePlan(paddleTarget, Color.red);
            visualisePlan(0, Color.green);

            ballCollideToPaddle(paddleTarget, ballWorkMemory);
            double timeLeftAfter = PongUtil.simulateOld(ballWorkMemory, lastKnownStatus.conf, lines, Color.red);
            timeLeft += timeLeftAfter;

            // now we are done.
            ClientGameState.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = ballWorkMemory.y - myPedal.y;

            requestChangeSpeed((float) (0.999f * diff_y / (Math.abs(diff_y) + 0.0000001)));
        }

        MissileDodger.dodge(lines, this, 0);
        getBallPositionHistory().drawLastCollision(lines);
        getPaddleVelocity().drawReachableArea(lines, newStatus.getPedal(getMySide()).y + newStatus.conf.paddleHeight * 0.5, timeLeft, newStatus.conf.paddleHeight);

        for(Avoidable avoidable : getAvoidables()) {
            Visualisation.drawCross(lines, Color.pink, avoidable.t * 300, avoidable.y);
        }
        for(Avoidable avoidable : getOffensiveMissiles()) {
            Visualisation.drawCross(lines, Color.green, lastKnownStatus.conf.maxWidth - avoidable.t * 300, avoidable.y);
        }
    }

    private void visualisePlan(double paddleTarget, Color color) {
        ballTemp.copy(ballWorkMemory, true);
        ballCollideToPaddle(paddleTarget, ballTemp);
        PongUtil.simulateOld(ballTemp, lastKnownStatus.conf, lines, color);
    }

    private void visualiseModel(double x, double y, double minReach, double maxReach) {

        double targetPos = ballWorkMemory.y - lastKnownStatus.conf.paddleHeight * 0.5;
        double paddleMaxPos = lastKnownStatus.conf.maxHeight - lastKnownStatus.conf.paddleHeight;
        double paddleMinPos = 0;

        for(int i=0; i<11; ++i) {

            double pos = (i - 5) / 5.0;
            Color color = Color.green;

            if(pos < minReach || pos > maxReach) {
                color = Color.blue;
            }

            double evaluatedPaddlePos = targetPos - pos * lastKnownStatus.conf.paddleHeight * 0.5;
            if(paddleMaxPos < evaluatedPaddlePos || paddleMinPos > evaluatedPaddlePos) {
                color = Color.red;
            }

            Vector2 ballOut = myModel.guess(pos, ballWorkMemory.vx, ballWorkMemory.vy);
            ballOut.normalize().scaled(100);

            double y_point = y + pos * lastKnownStatus.conf.paddleHeight * 0.5 + lastKnownStatus.conf.paddleHeight * 0.5;
            lines.add(new UILine(new Vector2i(x, y_point), new Vector2i(x + ballOut.x, y_point + ballOut.y), color));

        }
    }

    private void changeCourse(double distance) {

        double idealVelocity = (distance / timeLeft / getPaddleMaxVelocity()); // this aims for the centre of current target

        // run until near target.
        if(distance * distance > 2500) {
            idealVelocity = distance > 0 ? +1 : -1;
        }

        // not going faster than allowed
        if(idealVelocity * idealVelocity > 1.0) {
            if(idealVelocity > 0)
                idealVelocity = +1;
            else
                idealVelocity = -1;
        }

        idealVelocity = MissileDodger.dodge(lines, this, idealVelocity);

        if(idealVelocity != myState.velocity() || reallyShouldUpdateRegardless()) {
            requestChangeSpeed(idealVelocity);
        }
    }

    private boolean needToReact(double targetPos) {
        double myPos = lastKnownStatus.getPedal(getMySide()).y;
        double movingDistance = timeLeft * myState.velocity() * getPaddleMaxVelocity();

        double ballEndPos = ballWorkMemory.y;
        double expectedPosition = movingDistance + myPos + lastKnownStatus.conf.paddleHeight * 0.5;
        double expectedDistance = ballEndPos - expectedPosition;

        double halfPaddle = lastKnownStatus.conf.paddleHeight * 0.1;
        return expectedDistance * expectedDistance >= halfPaddle * halfPaddle || reallyShouldUpdateRegardless();
    }

    public boolean requestChangeSpeed(double v) {

        int requestedVelocity = (int)(v * 100);
        int prevVelocity = (int)(myState.velocity() * 100);
        int delta = requestedVelocity - prevVelocity;

        if(delta * delta < 1) {
            return false;
        }

        if(super.requestChangeSpeed(v)) {
            myState.setVelocity(v);
            return true;
        }

        return false;
    }


    @Override
    public void onGameOver(boolean won) {

        dataCollector.gameOver();

        myState.setToHandling();
        myState.setVelocity(0);

        ++numGames;
        if(won) {
            ++numWins;
        }

        System.out.println(getDefaultName() + " wins " + numWins + "/" + numGames + " (" + ((float)numWins / numGames) + ")");

        lastKnownStatus.reset();
        extrapolatedStatus.reset();
        extrapolatedTime = 0;
    }

    @Override
    public void onTick(double dt) {
    }

    @Override
    public String getDefaultName() {
        return "Deflector";
    }

    @Override
    public ArrayList<UILine> getDrawLines() {
        return this.lines;
    }

}

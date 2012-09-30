package redlynx.bots.finals;

import redlynx.pong.client.Pong;
import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;
import redlynx.pong.util.Vector3;

import java.awt.*;
import java.util.ArrayList;


public class FinalSauron extends PongGameBot {

    public FinalSauron() {
        super();
        myModel = new FinalSauronModel(this);
    }

	public static void main(String[] args) {
		Pong.init(args, new FinalSauron());
	}

    private FinalSauronEvaluator evaluator = new FinalSauronEvaluator();
    private SauronState myState = new SauronState();
    private final ArrayList<UILine> lines = new ArrayList<UILine>();
    private boolean shoutPlan = true;

    double timeLeft = 10000;
    private int numWins = 0;
    private int numGames = 0;

    @Override
    public void onGameStateUpdate(ClientGameState newStatus) {

        lines.clear();
        double ball_direction = newStatus.ball.vx;

        if(hasMissiles()) {
            fireMissile();
        }

        if(getMySide().comingTowardsMe(ball_direction)) {
            // find out impact velocity and position.
            ballWorkMemory.copy(newStatus.ball, true);
            ballWorkMemory.setVelocity(getBallVelocity());
            timeLeft = PongUtil.simulateOld(ballWorkMemory, lastKnownStatus.conf, lines, Color.green);

            Vector2 reach = getPaddlePossibleReturns(newStatus, ballWorkMemory, PlayerSide.LEFT, timeLeft);
            double minReach = reach.x;
            double maxReach = reach.y;

            {
                // hack.. if angle is high, don't try to hit the ball with the wrong end of the paddle..
                double value = ballWorkMemory.vy / (Math.abs(ballWorkMemory.vx) + 0.000001);
                double amount = Math.min(0.5, value * value * 0.3);
                if(value < 0.0 && minReach < -1+amount) {
                    minReach = -1+amount;
                }
                else if(value > 0.0 && maxReach > 1-amount) {
                    maxReach = +1-amount;
                }
            }

            // this is the expected y value when colliding against our paddle.
            Vector3 target = evaluator.offensiveEval(this, newStatus, PlayerSide.RIGHT, ballWorkMemory, ballTemp, minReach, maxReach);

            // when no winning move available
            if(target.z < 100) {
                ballWorkMemory.copy(newStatus.ball, true);
                ballWorkMemory.setVelocity(getBallVelocity());
                timeLeft = PongUtil.simulateOld(ballWorkMemory, lastKnownStatus.conf, lines, Color.green);
                target = evaluator.defensiveEval(this, lastKnownStatus, PlayerSide.RIGHT, minReach, maxReach, ballWorkMemory);
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
        }
        else {
            // simulate twice, once there, and then back.
            ballWorkMemory.copy(newStatus.ball, true);
            ballWorkMemory.setVelocity(getBallVelocity());

            timeLeft = PongUtil.simulateOld(ballWorkMemory, lastKnownStatus.conf, lines, Color.green);
            Vector2 reach = getPaddlePossibleReturns(newStatus, ballWorkMemory, PlayerSide.RIGHT, timeLeft);

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

        getBallPositionHistory().drawLastCollision(lines);
        getPaddleVelocity().drawReachableArea(lines, newStatus.getPedal(getMySide()).y + newStatus.conf.paddleHeight * 0.5, timeLeft, newStatus.conf.paddleHeight);
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
        // ok seems we really have to change course.
        double idealVelocity = (distance / timeLeft / getPaddleMaxVelocity()); // this aims for the centre of current target

        // run until near target.
        if(distance * distance > 250) {
            idealVelocity = distance > 0 ? +1 : -1;
        }

        // not going faster than allowed
        if(idealVelocity * idealVelocity > 1.0) {
            if(idealVelocity > 0)
                idealVelocity = +1;
            else
                idealVelocity = -1;
        }

        // avoid missiles
        if(getAvoidables().size() > 0) {
            double myPos = lastKnownStatus.left.y + 0.5 * lastKnownStatus.conf.paddleHeight;
            double minAllowedVelocity = +1000;
            double maxAllowedVelocity = -1000;

            for(int i=0; i<100; ++i) {
                for(Avoidable avoidable : getAvoidables()) {
                    double testVelocity = (i - 50) / 50.0;
                    double dPos = avoidable.t * getPaddleMaxVelocity() * testVelocity;
                    boolean inTop = myPos + dPos + 15 + 0.5 * lastKnownStatus.conf.paddleHeight > avoidable.y;
                    boolean inBot = myPos + dPos - 15 - 0.5 * lastKnownStatus.conf.paddleHeight < avoidable.y;

                    if(inBot && inTop) {
                        // not acceptable velocity.
                    }
                    else {
                        if(testVelocity < minAllowedVelocity) {
                            minAllowedVelocity = testVelocity;
                        }
                        if(testVelocity > maxAllowedVelocity) {
                            maxAllowedVelocity = testVelocity;
                        }
                    }
                }
            }

            if(idealVelocity > 0) {
                idealVelocity = maxAllowedVelocity;
            }
            else {
                idealVelocity = minAllowedVelocity;
            }
        }

        if(idealVelocity != myState.velocity()) {
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
        return "FinalSauron";
    }

    @Override
    public ArrayList<UILine> getDrawLines() {
        return this.lines;
    }

}

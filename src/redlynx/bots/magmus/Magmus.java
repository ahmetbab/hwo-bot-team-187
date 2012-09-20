package redlynx.bots.magmus;

import java.util.ArrayList;

import redlynx.pong.client.Pong;
import redlynx.pong.client.collisionmodel.LinearModel;
import redlynx.pong.client.collisionmodel.PongModel;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.client.state.PongGameBot;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

import java.awt.Color;


public class Magmus extends PongGameBot {

	public static void main(String[] args) {
		Pong.init(args, new Magmus());
	}
	
    private MagmusState myState = new MagmusState();

    private final ClientGameState.Ball myDirectionBall = new ClientGameState.Ball();
    private final ClientGameState.Ball tmpBall = new ClientGameState.Ball();

    private final ArrayList<UILine> lines = new ArrayList<UILine>();
    private final PongModel myModel = new LinearModel();

    double timeLeft = 10000;
    private int numWins = 0;
    private int numGames = 0;

    @Override
    public void onGameStateUpdate(ClientGameState newStatus) {

        lines.clear();
        double ball_direction = lastKnownStatus.ball.vx;

        if(getMySide().comingTowardsMe(ball_direction)) {
            // find out impact velocity and position.
            myDirectionBall.copy(lastKnownStatus.ball, true);
            myDirectionBall.setVelocity(getBallVelocity());
            timeLeft = PongUtil.simulate(myDirectionBall, lastKnownStatus.conf, lines, Color.green);

            Vector2 reach = getPaddlePossibleReturns(newStatus, PlayerSide.LEFT, timeLeft);
            double minReach = reach.x;
            double maxReach = reach.y;

            // this is the expected y value when colliding against our paddle.
            Vector2 target = evaluate(newStatus, PlayerSide.RIGHT, myDirectionBall, tmpBall, minReach, maxReach);
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
                if(needToReact(targetPos)) {
                    changeCourse(distance);
                }
            }
        }
        else {
            // simulate twice, once there, and then back.
            myDirectionBall.copy(lastKnownStatus.ball, true);
            myDirectionBall.setVelocity(getBallVelocity());

            timeLeft = PongUtil.simulate(myDirectionBall, lastKnownStatus.conf, lines, Color.green);
            Vector2 reach = getPaddlePossibleReturns(newStatus, PlayerSide.RIGHT, timeLeft);

            double minReach = reach.x;
            double maxReach = reach.y;

            // this is the current worst case. should try to cover that?
            Vector2 target = evaluate(newStatus, PlayerSide.LEFT, myDirectionBall, tmpBall, minReach, maxReach);
            double paddleTarget = target.y;

            // draw stuff on the hud.
            visualiseModel(lastKnownStatus.conf.maxWidth, lastKnownStatus.getPedal(PlayerSide.RIGHT).y, minReach, maxReach);
            visualisePlan(paddleTarget, Color.red);
            visualisePlan(0, Color.green);

            ballCollideToPaddle(paddleTarget, myDirectionBall);
            double timeLeftAfter = PongUtil.simulate(myDirectionBall, lastKnownStatus.conf, lines, Color.red);
            timeLeft += timeLeftAfter;

            // now we are done.
            ClientGameState.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = myDirectionBall.y - myPedal.y;

            // TODO: Create a weighted spray of bounce backs. Go to the position from which most can be caught.
            requestChangeSpeed((float) (0.99f * diff_y / Math.abs(diff_y)));
        }

        getHistory().drawLastCollision(lines);
        getPaddleVelocity().drawReachableArea(lines, newStatus.getPedal(getMySide()).y + newStatus.conf.paddleHeight * 0.5, timeLeft, newStatus.conf.paddleHeight);
    }

    private Vector2 getPaddlePossibleReturns(ClientGameState state, PlayerSide side, double timeLeft) {

        timeLeft -= 0.1;

        Vector2 ans = new Vector2();
        double paddleMid = state.getPedal(side).y + 0.5 * state.conf.paddleHeight;
        double maxReach = paddleMid + timeLeft * getPaddleMaxVelocity() + 0.5 * state.conf.paddleHeight;
        double minReach = paddleMid - timeLeft * getPaddleMaxVelocity() - 0.5 * state.conf.paddleHeight;
        maxReach -= myDirectionBall.y;
        minReach -= myDirectionBall.y;
        maxReach /= 0.5 * state.conf.paddleHeight;
        minReach /= 0.5 * state.conf.paddleHeight;

        System.out.println("maxReach: " + maxReach);
        System.out.println("minReach: " + minReach);

        maxReach = Math.min(+1, maxReach);
        minReach = Math.max(-1, minReach);
        ans.x = -maxReach;
        ans.y = -minReach;
        return ans;
    }

    private void visualisePlan(double paddleTarget, Color color) {
        tmpBall.copy(myDirectionBall, true);
        ballCollideToPaddle(paddleTarget, tmpBall);
        PongUtil.simulate(tmpBall, lastKnownStatus.conf, lines, color);
    }

    private void visualiseModel(double x, double y, double minReach, double maxReach) {
        for(int i=0; i<11; ++i) {
            double pos = (i - 5) / 5.0;

            Color color = Color.green;

            if(pos < minReach || pos > maxReach) {
                color = Color.red;
            }

            Vector2 ballOut = myModel.guess(pos, myDirectionBall.vx, myDirectionBall.vy);
            ballOut.normalize().scaled(100);

            double y_point = y + pos * lastKnownStatus.conf.paddleHeight * 0.5 + lastKnownStatus.conf.paddleHeight * 0.5;
            lines.add(new UILine(new Vector2i(x, y_point), new Vector2i(x + ballOut.x, y_point + ballOut.y), color));
        }
    }

    private void changeCourse(double distance) {
        // ok seems we really have to change course.
        double idealVelocity = (distance / timeLeft / getPaddleMaxVelocity()); // this aims for the centre of current target
        if(idealVelocity * idealVelocity > 1.0) {
            if(idealVelocity > 0)
                idealVelocity = +1;
            else
                idealVelocity = -1;
        }

        if(idealVelocity != myState.velocity()) {
            requestChangeSpeed(idealVelocity);
        }
    }

    private boolean needToReact(double targetPos) {

        double myPos = lastKnownStatus.getPedal(getMySide()).y;
        double movingDistance = timeLeft * myState.velocity() * getPaddleMaxVelocity();

        double ballEndPos = myDirectionBall.y;
        double expectedPosition = movingDistance + myPos + lastKnownStatus.conf.paddleHeight * 0.5;
        double expectedDistance = ballEndPos - expectedPosition;

        /*
        lines.add(new UILine(new Vector2i(0.0, expectedPosition + 5), new Vector2i(+5.0, expectedPosition - 5), Color.orange));
        lines.add(new UILine(new Vector2i(0.0, expectedPosition - 5), new Vector2i(+5.0, expectedPosition + 5), Color.orange));
        lines.add(new UILine(new Vector2i(15, expectedPosition), new Vector2i(15, expectedPosition + expectedDistance), Color.green));
        */

        double halfPaddle = lastKnownStatus.conf.paddleHeight * 0.1;
        return expectedDistance * expectedDistance >= halfPaddle * halfPaddle;
    }

    private Vector2 evaluate(ClientGameState state, PlayerSide catcher, ClientGameState.Ball collidingBallState, ClientGameState.Ball tmpBall, double minVal, double maxVal) {
        double targetPos = collidingBallState.y - state.conf.paddleHeight * 0.5;
        double botValue = -10000;
        double topValue = -10000;
        double paddleTargetBot = 0;
        double paddleTargetTop = 0;
        double paddleMaxPos = lastKnownStatus.conf.maxHeight - lastKnownStatus.conf.paddleHeight;
        double paddleMinPos = 0;
        {
            for(int i=10; i<90; ++i) {
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
                ballCollideToPaddle(tmpTarget, tmpBall);
                double opponentTime = PongUtil.simulate(tmpBall, state.conf);
                double opponentReach = opponentTime * getPaddleMaxVelocity() + state.conf.paddleHeight * 0.5;
                double opponentBot = state.getPedal(catcher).y - opponentReach - state.conf.paddleHeight * 0.5;
                double opponentTop = state.getPedal(catcher).y + opponentReach - state.conf.paddleHeight * 0.5;

                double tmpBotValue = -(tmpBall.y - opponentBot);
                double tmpTopValue = +(tmpBall.y - opponentTop);

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

        // bind target position inside play area
        double paddleTarget = (botValue > topValue) ? paddleTargetBot : paddleTargetTop;
        targetPos -= paddleTarget * state.conf.paddleHeight * 0.5;
        targetPos = targetPos < paddleMinPos ? paddleMinPos : targetPos;
        targetPos = targetPos > paddleMaxPos ? paddleMaxPos : targetPos;
        return new Vector2(targetPos, paddleTarget);
    }

    private void ballCollideToPaddle(double paddleRelativePos, ClientGameState.Ball ball) {
        Vector2 ballOut = myModel.guess(paddleRelativePos, ball.vx, ball.vy);
        ballOut.normalize().scaled(getBallVelocity());
        ball.vx = ballOut.x;
        ball.vy = ballOut.y;
        ball.tick(0.01f);
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

        System.out.println("Game ended, wins " + numWins + "/" + numGames + " (" + ((float)numWins / numGames) + ")");

        lastKnownStatus.reset();
        extrapolatedStatus.reset();
        extrapolatedTime = 0;
    }

    @Override
    public void onTick(double dt) {
    }

    @Override
    public String getDefaultName() {
        return "Magmus";
    }

    @Override
    public ArrayList<UILine> getDrawLines() {
        return this.lines;
    }

}

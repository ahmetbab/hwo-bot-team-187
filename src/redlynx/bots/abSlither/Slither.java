package redlynx.bots.abSlither;

import java.awt.Color;
import java.util.ArrayList;

import redlynx.bots.magmus.MagmusState;
import redlynx.pong.client.Pong;
import redlynx.pong.client.BaseBot;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.collisionmodel.LinearModel;
import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

public class Slither extends BaseBot {

    public static void main(String[] args) {
        Pong.init(args, new Slither());
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
            timeLeft = PongUtil.simulateOld(myDirectionBall, lastKnownStatus.conf, lines, Color.green);

            // this is the expected y value when colliding against our paddle.
            Vector2 target = evaluate(newStatus, PlayerSide.RIGHT, myDirectionBall, tmpBall, timeLeft);
            double targetPos = target.x;
            double paddleTarget = target.y;

            Vector2 reach = getPaddlePossibleReturns(newStatus, myDirectionBall, PlayerSide.RIGHT, timeLeft);
            double minReach = reach.x;
            double maxReach = reach.y;

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

            // this is the current worst case. should try to cover that?
            timeLeft = PongUtil.simulateOld(myDirectionBall, lastKnownStatus.conf, lines, Color.green);
            Vector2 target = evaluate(newStatus, PlayerSide.LEFT, myDirectionBall, tmpBall, timeLeft);
            double paddleTarget = target.y;

            Vector2 reach = getPaddlePossibleReturns(newStatus, myDirectionBall, PlayerSide.RIGHT, timeLeft);
            double minReach = reach.x;
            double maxReach = reach.y;

            // draw stuff on the hud.
            visualiseModel(lastKnownStatus.conf.maxWidth, lastKnownStatus.getPedal(PlayerSide.RIGHT).y, minReach, maxReach);
            visualisePlan(paddleTarget, Color.red);
            visualisePlan(0, Color.green);

            ballCollideToPaddle(paddleTarget, myDirectionBall);
            double timeLeftAfter = PongUtil.simulateOld(myDirectionBall, lastKnownStatus.conf, lines, Color.red);
            timeLeft += timeLeftAfter;

            // now we are done.
            ClientGameState.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = myDirectionBall.y - myPedal.y;

            requestChangeSpeed((float) (0.99f * diff_y / (Math.abs(diff_y) + 0.0000000000001)));
        }

        getBallPositionHistory().drawLastCollision(lines);
        getPaddleVelocity().drawReachableArea(lines, newStatus.getPedal(getMySide()).y + newStatus.conf.paddleHeight * 0.5, timeLeft, newStatus.conf.paddleHeight);
    }


    private void visualisePlan(double paddleTarget, Color color) {
        tmpBall.copy(myDirectionBall, true);
        ballCollideToPaddle(paddleTarget, tmpBall);
        PongUtil.simulateOld(tmpBall, lastKnownStatus.conf, lines, color);
    }

    private void visualiseModel(double x, double y, double minReach, double maxReach) {

        double targetPos = myDirectionBall.y - lastKnownStatus.conf.paddleHeight * 0.5;
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

        double halfPaddle = lastKnownStatus.conf.paddleHeight * 0.1;
        return expectedDistance * expectedDistance >= halfPaddle * halfPaddle;
    }

    private Vector2 evaluate(ClientGameState state, PlayerSide catcher, ClientGameState.Ball collidingBallState, ClientGameState.Ball tmpBall, double allowedTime) {

        Vector2 reach = getPaddlePossibleReturns(state, collidingBallState, PlayerSide.getOtherSide(catcher), allowedTime);
        double minVal = reach.x;
        double maxVal = reach.y;

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

        // System.out.println("Game ended, wins " + numWins + "/" + numGames + " (" + ((float)numWins / numGames) + ")");

        lastKnownStatus.reset();
        extrapolatedStatus.reset();
        extrapolatedTime = 0;
    }

    @Override
    public void onTick(double dt) {
    }

    @Override
    public String getDefaultName() {
        return "Slither";
    }

    @Override
    public ArrayList<UILine> getDrawLines() {
        return this.lines;
    }

}

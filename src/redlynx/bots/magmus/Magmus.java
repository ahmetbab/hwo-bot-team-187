package redlynx.bots.magmus;

import java.util.ArrayList;

import redlynx.pong.client.Pong;
import redlynx.pong.client.collisionmodel.LinearModel;
import redlynx.pong.client.collisionmodel.PongModel;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.client.state.PongGameBot;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.PongUtil;
import java.awt.Color;

public class Magmus extends PongGameBot {

	
	public static void main(String[] args) {
		Pong.init(args, new Magmus());
	}
	
    private MagmusState myState = new MagmusState();
    private final ClientGameState.Ball myDirectionBall = new ClientGameState.Ball();
    private final ClientGameState.Ball opponentDirectionBall = new ClientGameState.Ball();

    private final ArrayList<UILine> lines = new ArrayList<UILine>();
    private final PongModel myModel = new LinearModel();

    double timeLeft = 10000;
    private int numWins = 0;
    private int numGames = 0;

    @Override
    public void onGameStateUpdate(ClientGameState newStatus) {
        double ball_direction = lastKnownStatus.ball.vx;

        if(getMySide().comingTowardsMe(ball_direction)) {
            // find out impact velocity and position.
            myDirectionBall.copy(lastKnownStatus.ball, true);
            myDirectionBall.setVelocity(getBallVelocity());

            lines.clear();
            timeLeft = PongUtil.simulate(myDirectionBall, lastKnownStatus.conf, lines, Color.green);



            // this is the expected y value when colliding against our paddle.
            double targetPos = myDirectionBall.y - lastKnownStatus.conf.paddleHeight * 0.5;

            opponentDirectionBall.copy(myDirectionBall, true);
            opponentDirectionBall.vx *= -1;
            opponentDirectionBall.tick(0.01f);

            double opponentTime = PongUtil.simulate(opponentDirectionBall, lastKnownStatus.conf, lines, Color.green) + timeLeft;
            double opponentReach = opponentTime * getPaddleMaxVelocity() + lastKnownStatus.conf.paddleHeight * 0.5;
            double opponentBot = newStatus.getPedal(PlayerSide.RIGHT).y - opponentReach - lastKnownStatus.conf.paddleHeight * 0.5;
            double opponentTop = newStatus.getPedal(PlayerSide.RIGHT).y + opponentReach - lastKnownStatus.conf.paddleHeight * 0.5;

            // -1 try to collide with bottom,
            // +1 try to collide with top.
            double paddleTarget = 0; // by default, aim for the centre.
            if(opponentBot > lastKnownStatus.conf.maxHeight - opponentTop) {
                // best shot is at bottom corner
                double best_y = 100000;
                for(int i=10; i<90; ++i) {
                    opponentDirectionBall.copy(myDirectionBall, true);
                    opponentDirectionBall.vx *= -1;
                    opponentDirectionBall.tick(0.01f);

                    double tmpTarget = (i - 50) / 50.0;
                    opponentDirectionBall.vy += myModel.guess(tmpTarget, opponentDirectionBall.vy );
                    PongUtil.simulate(opponentDirectionBall, lastKnownStatus.conf);

                    if(opponentDirectionBall.y < best_y) {
                        best_y = opponentDirectionBall.y;
                        paddleTarget = tmpTarget;
                    }
                }


            }
            else {
                // best shot is at top corner
                double best_y = 0;
                for(int i=10; i<90; ++i) {
                    opponentDirectionBall.copy(myDirectionBall, true);
                    opponentDirectionBall.vx *= -1;
                    opponentDirectionBall.tick(0.01f);

                    double tmpTarget = (i - 50) / 50.0;
                    opponentDirectionBall.vy += myModel.guess(tmpTarget, opponentDirectionBall.vy );
                    PongUtil.simulate(opponentDirectionBall, lastKnownStatus.conf);

                    if(opponentDirectionBall.y > best_y) {
                        best_y = opponentDirectionBall.y;
                        paddleTarget = tmpTarget;
                    }
                }
            }

            // visualize my plan
            {
                opponentDirectionBall.copy(myDirectionBall, true);
                opponentDirectionBall.vx *= -1;
                opponentDirectionBall.tick(0.01f);
                opponentDirectionBall.vy += myModel.guess(paddleTarget, opponentDirectionBall.vy );
                PongUtil.simulate(opponentDirectionBall, lastKnownStatus.conf, lines, Color.red);
            }

            // alter aim so that would hit with target position.
            targetPos -= paddleTarget * lastKnownStatus.conf.paddleHeight * 0.5;

            // now we are done.
            ClientGameState.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = targetPos - myPedal.y;

            // if not hitting accurately, alter direction.


            // if moving to catch ball
            // AND not going fast enough
            // AND not going at max speed
            // request enough speed.

            if(myState.catching()) {
                boolean rightDirection = myState.velocity() * diff_y < +0.000001f;
                double myPos = extrapolatedStatus.getPedal(getMySide()).y;

                double movingDistance = timeLeft * myState.velocity() * getPaddleMaxVelocity();
                double reachableDistanceWithCurrentVelocity = Math.abs(movingDistance) + lastKnownStatus.conf.paddleHeight * 0.5;

                double distance = (targetPos - myPos);

                /*
                if(distance * distance < reachableDistanceWithCurrentVelocity * reachableDistanceWithCurrentVelocity * 0.9 && rightDirection) {
                    // all is well. going fast enough to catch ball.
                    // TODO: remember to slow down when necessary, take opponents position into account
                }
                */
                {
                    // if going to hit accurately with current velocity, do nothing
                    double expectedPosition = movingDistance + myPos;
                    double expectedDistance = targetPos - expectedPosition;
                    double halfPaddle = lastKnownStatus.conf.paddleHeight * 0.1;
                    if(expectedDistance * expectedDistance < halfPaddle * halfPaddle) {
                        // all ok. just let it play out.
                    }
                    else {
                        // ok seems we really have to change course.
                        double idealVelocity = (distance / timeLeft / getPaddleMaxVelocity()); // this aims for the centre of current target
                        if(idealVelocity * idealVelocity > 1.0) {
                            if(idealVelocity > 0)
                                idealVelocity = +1;
                            else
                                idealVelocity = -1;
                        }

                        // TODO: Replace with choice that takes opponents position into account.
                        if(idealVelocity != myState.velocity()) {
                            requestChangeSpeed(idealVelocity);
                        }
                    }
                }
            }
        }
        else {
            // simulate twice, once there, and then back.
            myDirectionBall.copy(lastKnownStatus.ball, true);
            myDirectionBall.setVelocity(getBallVelocity());

            lines.clear();
            timeLeft = PongUtil.simulate(myDirectionBall, lastKnownStatus.conf, lines, Color.red);

            myDirectionBall.vx *= -1;
            myDirectionBall.tick(0.01f);

            double timeLeftAfter = PongUtil.simulate(myDirectionBall, lastKnownStatus.conf, lines, Color.green);
            timeLeft += timeLeftAfter;

            myDirectionBall.y -= lastKnownStatus.conf.paddleHeight * 0.5;

            // now we are done.
            ClientGameState.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = myDirectionBall.y - myPedal.y;

            // TODO: Create a weighted spray of bounce backs. Go to the position from which most can be caught.
            if(myState.catching() && myPedal.vy * diff_y < +0.001f) {
                requestChangeSpeed((float) (0.99f * diff_y / Math.abs(diff_y)));
            }
        }

        getHistory().drawLastCollision(lines);
        getPaddleVelocity().drawReachableArea(lines, newStatus.getPedal(getMySide()).y + newStatus.conf.paddleHeight * 0.5, timeLeft, newStatus.conf.paddleHeight);
    }

    public void requestChangeSpeed(double v) {

        int requestedVelocity = (int)(v * 100);
        int prevVelocity = (int)(myState.velocity() * 100);
        int delta = requestedVelocity - prevVelocity;

        // don't make meaningless choices
        if(delta * delta < 10) {
            return;
        }

        super.requestChangeSpeed(v);
        myState.setVelocity(v);
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

        /*
        double dy = myDirectionBall.y - extrapolatedStatus.getPedal(getMySide()).y;
        if(dy * dy < 0.5 * extrapolatedStatus.conf.paddleHeight * extrapolatedStatus.conf.paddleHeight / 4) {
            if(myState.catching()) {
                requestChangeSpeed(0);
                myState.setToWaiting();
            }
        }
        else {
            if(!myState.catching()) {
                myState.setToHandling();
            }
        }
        */

    }

    @Override
    public ArrayList<UILine> getDrawLines() {
        return this.lines;
    }
}

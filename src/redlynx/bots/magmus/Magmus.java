package redlynx.bots.magmus;

import java.util.ArrayList;

import redlynx.pong.client.Pong;
import redlynx.pong.client.collisionmodel.LinearModel;
import redlynx.pong.client.collisionmodel.PongModel;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.client.state.PongGameBot;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.PongUtil;

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
            timeLeft = PongUtil.simulate(myDirectionBall, lastKnownStatus.conf, lines);

            // this is the expected y value when colliding against our paddle.
            myDirectionBall.y -= lastKnownStatus.conf.paddleHeight * 0.5;

            // now we are done.
            ClientGameState.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = myDirectionBall.y - myPedal.y;

            // if moving to catch ball
            // AND not going fast enough
            // AND not going at max speed
            // request enough speed.

            if(myState.catching()) {
                boolean rightDirection = myState.velocity() * diff_y < +0.000001f;
                double targetPos = myDirectionBall.y;
                double myPos = extrapolatedStatus.getPedal(getMySide()).y;

                double movingDistance = timeLeft * myState.velocity() * getPaddleMaxVelocity();
                double reachableDistanceWithCurrentVelocity = Math.abs(movingDistance) + lastKnownStatus.conf.paddleHeight * 0.5;

                double distance = (targetPos - myPos);

                if(distance * distance < reachableDistanceWithCurrentVelocity * reachableDistanceWithCurrentVelocity * 0.9 && rightDirection) {
                    // all is well. going fast enough to catch ball.
                    // TODO: remember to slow down when necessary, take opponents position into account
                }
                else {
                    // if going to hit with current velocity, do nothing
                    double expectedPosition = movingDistance + myPos;
                    double expectedDistance = targetPos - expectedPosition;
                    double halfPaddle = lastKnownStatus.conf.paddleHeight * 0.5;
                    if(expectedDistance * expectedDistance < halfPaddle * halfPaddle * 0.5) {
                        // all ok. just let it play out.
                    }
                    else {
                        // ok seems we really have to change course.
                        double idealVelocity = (distance / timeLeft / getPaddleMaxVelocity()); // this aims for the centre of the paddle.
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
            timeLeft = PongUtil.simulate(myDirectionBall, lastKnownStatus.conf, lines);

            myDirectionBall.vx *= -1;
            myDirectionBall.tick(0.01f);

            double timeLeftAfter = PongUtil.simulate(myDirectionBall, lastKnownStatus.conf, lines);
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
        super.requestChangeSpeed(v);
        myState.setVelocity(v);

        System.out.println("Changed velocity: " + (int)(v * 100) );
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
    }

    @Override
    public ArrayList<UILine> getDrawLines() {
        return this.lines;
    }
}

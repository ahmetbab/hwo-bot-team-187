package redlynx.mikabot;

import redlynx.pong.GameStatus;
import redlynx.pong.PongGameBot;
import redlynx.pong.PongGameCommunicator;

import java.util.Queue;

public class TestBot extends PongGameBot {

    public TestBot(String name, PongGameCommunicator communicator, Queue<String> serverMessageQueue) {
        super(name, communicator, serverMessageQueue);
    }

    boolean firstUpdate = true;
    public GameStatus.Ball tmpBall = new GameStatus.Ball();

    @Override
    public void onGameStateUpdate(GameStatus newStatus) {
        double ball_direction = lastKnownStatus.ball.vx;
        if(getMySide().comingTowardsMe(ball_direction)) {
            // find out impact velocity and position.
            tmpBall.copy(lastKnownStatus.ball);
            simulate(tmpBall);

            tmpBall.y -= lastKnownStatus.conf.paddleHeight * 0.5;

            // now we are done.
            GameStatus.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = tmpBall.y - myPedal.y;
            if(myPedal.vy * diff_y < +0.001f) {
                getCommunicator().sendUpdate((float) (0.99f * diff_y / Math.abs(diff_y)));
            }
        }
        else {
            // simulate twice, once there, and then back.
            tmpBall.copy(lastKnownStatus.ball);
            simulate(tmpBall);

            tmpBall.vx *= -1;
            tmpBall.tick(0.05f);

            simulate(tmpBall);

            tmpBall.y -= lastKnownStatus.conf.paddleHeight * 0.5;

            // now we are done.
            GameStatus.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = tmpBall.y - myPedal.y;
            if(myPedal.vy * diff_y < +0.001f) {
                getCommunicator().sendUpdate((float) (0.99f * diff_y / Math.abs(diff_y)));
            }
        }
    }

    private void simulate(GameStatus.Ball ball) {
        double vy = ball.vy;
        double vx = ball.vx;
        double x = ball.x;
        double y = ball.y;

        if(vx * vx < 0.00001f)
            return;

        while(x > lastKnownStatus.conf.ballRadius + lastKnownStatus.conf.paddleWidth && x < lastKnownStatus.conf.maxWidth - lastKnownStatus.conf.ballRadius - lastKnownStatus.conf.paddleWidth) {
            x += vx * 0.05;
            y += vy * 0.05;

            // if collides with walls, mirror y velocity
            if(y + lastKnownStatus.conf.ballRadius >= lastKnownStatus.conf.maxHeight) {
                vy *= -1;
            }

            if(y - lastKnownStatus.conf.ballRadius <= 0) {
                vy *= -1;
            }
        }

        ball.vy = vy;
        ball.vx = vx;
        ball.x = x;
        ball.y = y;
    }

    @Override
    public void onGameOver(boolean won) {
        if(won) {
            System.out.println("I WON :D");
        }
        else {
            System.out.println("I LOST D:");
        }

        lastKnownStatus.reset();
        extrapolatedStatus.reset();
        extrapolatedTime = 0;
    }

    @Override
    public void onTick(double dt) {
        double dy = tmpBall.y - extrapolatedStatus.getPedal(getMySide()).y;
        if(dy * dy < extrapolatedStatus.conf.paddleHeight * extrapolatedStatus.conf.paddleHeight) {
            // could slow down.

        }
    }
}

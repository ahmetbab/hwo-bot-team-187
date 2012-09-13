package redlynx.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Queue;

import redlynx.pong.client.collisionmodel.LinearModel;
import redlynx.pong.client.collisionmodel.PongModel;
import redlynx.pong.client.collisionmodel.PongModelInitializer;
import redlynx.pong.client.network.PongGameCommunicator;
import redlynx.pong.client.state.GameStatus;
import redlynx.pong.client.state.PongGameBot;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;

public class TestBot extends PongGameBot {

    private TestBotState myState;

    public GameStatus.Ball tmpBall = new GameStatus.Ball();

    // debug info
    private int verboseBallVelocity = 0;
    private boolean collisionReported = false;

    public TestBot(String name, PongGameCommunicator communicator, Queue<String> serverMessageQueue) {
        super(name, communicator, serverMessageQueue);
        PongModel myModel = new LinearModel();
        try {
            PongModelInitializer.init(myModel, new FileInputStream("pongdata.txt"));
            System.out.println("Model error: " + myModel.modelError());
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't init LinearModel!");
        }
    }

    @Override
    public void onGameStateUpdate(GameStatus newStatus) {

        if(verboseBallVelocity == 0 && lastKnownStatus.ball.x > lastKnownStatus.conf.maxWidth * 0.2) {
            verboseBallVelocity ^= 1;
            Vector2 velocity = new Vector2(lastKnownStatus.ball.vx, lastKnownStatus.ball.vy);
            System.out.println("Ball: " + velocity.normalize());
            collisionReported = true;
        }
        else if(verboseBallVelocity == 1 && lastKnownStatus.ball.x < lastKnownStatus.conf.maxWidth * 0.2) {
            verboseBallVelocity ^= 1;
            Vector2 velocity = new Vector2(lastKnownStatus.ball.vx, lastKnownStatus.ball.vy);
            System.out.println("Ball: " + velocity.normalize());
            collisionReported = false;
        }

        double ball_direction = lastKnownStatus.ball.vx;
        if(getMySide().comingTowardsMe(ball_direction)) {
            // find out impact velocity and position.
            tmpBall.copy(lastKnownStatus.ball);

            PongUtil.simulate(tmpBall, lastKnownStatus.conf);

            tmpBall.y -= lastKnownStatus.conf.paddleHeight * 0.5;

            // now we are done.
            GameStatus.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = tmpBall.y - myPedal.y;
            if(myState != TestBotState.STOPPED && myPedal.vy * diff_y < +0.001f) {
                getCommunicator().sendUpdate((float) (0.99f * diff_y / Math.abs(diff_y)));
            }
        }
        else {
            // simulate twice, once there, and then back.
            tmpBall.copy(lastKnownStatus.ball);

            PongUtil.simulate(tmpBall, lastKnownStatus.conf);

            tmpBall.vx *= -1;
            tmpBall.tick(0.05f);

            PongUtil.simulate(tmpBall, lastKnownStatus.conf);

            tmpBall.y -= lastKnownStatus.conf.paddleHeight * 0.5;

            // now we are done.
            GameStatus.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = tmpBall.y - myPedal.y;
            if(myState != TestBotState.STOPPED && myPedal.vy * diff_y < +0.001f) {
                getCommunicator().sendUpdate((float) (0.99f * diff_y / Math.abs(diff_y)));
            }
        }
    }

    @Override
    public void onGameOver(boolean won) {

        myState = TestBotState.HANDLING;
        collisionReported = false;
        verboseBallVelocity = 0;


        if(won) {
            // System.out.println("I WON :D");
        }
        else {
            // System.out.println("I LOST D:");
        }

        lastKnownStatus.reset();
        extrapolatedStatus.reset();
        extrapolatedTime = 0;
    }

    @Override
    public void onTick(double dt) {

        double dy = tmpBall.y - extrapolatedStatus.getPedal(getMySide()).y;
        if(dy * dy < extrapolatedStatus.conf.paddleHeight * extrapolatedStatus.conf.paddleHeight / 4) {

            if(myState != TestBotState.STOPPED) {
                getCommunicator().sendUpdate(0);
                myState = TestBotState.STOPPED;
            }
            else if(!collisionReported && lastKnownStatus.getPedal(getMySide()).vy == 0) {
                System.out.println("Collision: " + (2 * dy / extrapolatedStatus.conf.paddleHeight));
                collisionReported = true;
            }

        }
        else {
            if(myState == TestBotState.STOPPED) {
                myState = TestBotState.HANDLING;
            }
        }
    }
}

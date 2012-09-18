package redlynx.bots.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import redlynx.pong.client.Pong;
import redlynx.pong.client.collisionmodel.LinearModel;
import redlynx.pong.client.collisionmodel.PongModel;
import redlynx.pong.client.collisionmodel.PongModelInitializer;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.client.state.PongGameBot;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.PongUtil;
import java.awt.Color;

public class TestBot extends PongGameBot {

	public static void main(String[] args) {
		Pong.init(args, new TestBot());
	}
	
	
    private TestBotState myState;
    private final ClientGameState.Ball tmpBall = new ClientGameState.Ball();
    private final ArrayList<UILine> lines = new ArrayList<UILine>();
    private final PongModel myModel = new LinearModel();

    private double timeLeft = 10000; // this tells us how many seconds we have left until we lose.

    public TestBot() {
        //super(name, communicator);
        try {
            PongModelInitializer.init(myModel, new FileInputStream("pongdata.txt"));
            System.out.println("Model error: " + myModel.modelError());
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't init LinearModel!");
        }
    }
    
    
    
    

    @Override
    public void onGameStateUpdate(ClientGameState newStatus) {

        double ball_direction = lastKnownStatus.ball.vx;
        if(getMySide().comingTowardsMe(ball_direction)) {
            // find out impact velocity and position.
            tmpBall.copy(lastKnownStatus.ball, true);
            lines.clear();
            timeLeft = PongUtil.simulate(tmpBall, lastKnownStatus.conf, lines, Color.green);

            // this is the expected y value when colliding against our paddle.
            tmpBall.y -= lastKnownStatus.conf.paddleHeight * 0.5;

            // now we are done.
            ClientGameState.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = tmpBall.y - myPedal.y;
            if(myState != TestBotState.STOPPED && myPedal.vy * diff_y < +0.001f) {
                requestChangeSpeed((float) (0.99f * diff_y / Math.abs(diff_y)));
            }
        }
        else {
            // simulate twice, once there, and then back.
            tmpBall.copy(lastKnownStatus.ball, true);

            lines.clear();
            timeLeft = PongUtil.simulate(tmpBall, lastKnownStatus.conf, lines, Color.red);

            tmpBall.vx *= -1;
            tmpBall.tick(0.05f);

            timeLeft += PongUtil.simulate(tmpBall, lastKnownStatus.conf, lines, Color.green);
            tmpBall.y -= lastKnownStatus.conf.paddleHeight * 0.5;

            // now we are done.
            ClientGameState.Player myPedal = lastKnownStatus.getPedal(getMySide());
            double diff_y = tmpBall.y - myPedal.y;
            if(myState != TestBotState.STOPPED && myPedal.vy * diff_y < +0.001f) {
                requestChangeSpeed((float) (0.99f * diff_y / Math.abs(diff_y)));
            }
        }

        getHistory().drawLastCollision(lines);
        getPaddleVelocity().drawReachableArea(lines, newStatus.getPedal(getMySide()).y + newStatus.conf.paddleHeight * 0.5, timeLeft, newStatus.conf.paddleHeight);
    }

    @Override
    public void onGameOver(boolean won) {

        myState = TestBotState.HANDLING;

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
                requestChangeSpeed(0);
                myState = TestBotState.STOPPED;
            }
        }
        else {
            if(myState == TestBotState.STOPPED) {
                myState = TestBotState.HANDLING;
            }
        }
    }

    @Override
    public String getDefaultName() {
        return "TestBot";
    }

    @Override
    public ArrayList<UILine> getDrawLines() {
        return this.lines;
    }
}

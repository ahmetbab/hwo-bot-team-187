package redlynx.bots.finals.sauron;

import redlynx.bots.finals.DataCollector;
import redlynx.bots.finals.dataminer.DataMinerModel;
import redlynx.bots.semifinals.SFSauronModel;
import redlynx.pong.client.Pong;
import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.collisionmodel.SFSauronGeneralModel;
import redlynx.pong.ui.UILine;
import redlynx.pong.ui.UIString;
import redlynx.pong.util.*;

import java.awt.*;
import java.util.ArrayList;


public class FinalSauron extends PongGameBot {

    private final MissileCommand missileCommand = new MissileCommand(this);
    private final GameOverHandler gameOverHandler = new GameOverHandler(this);
    private final SauronVisualiser sauronVisualiser = new SauronVisualiser(this);
    private final DecisionMaker decisionMaker = new DecisionMaker(this);
    private final DataCollector dataCollector;
    public FinalSauron() {
        super();
        dataCollector = new DataCollector(new DataMinerModel(new SFSauronGeneralModel()), false);
        dataCollector.learnFromFile("miner1.txt", 1);
        myModel = dataCollector.getModel();
    }

	public static void main(String[] args) {
		Pong.init(args, new FinalSauron());
	}

    private FinalSauronEvaluator evaluator = new FinalSauronEvaluator();
    private SauronState myState = new SauronState();

    @Override
    public void onGameStateUpdate(ClientGameState newStatus) {

        lines.clear();
        double ball_direction = newStatus.ball.vx;

        double timeLeft = 0;
        if(getMySide().comingTowardsMe(ball_direction)) {
            timeLeft = decisionMaker.decisionMakerMyTurn(newStatus);
        }
        else {
            timeLeft = decisionMaker.decisionMakerOpponentsTurn(newStatus);
        }

        MissileDodger.dodge(lines, this, 0); // visualisation only
        getBallPositionHistory().drawLastCollision(lines);
        getPaddleVelocity().drawReachableArea(lines, newStatus.getPedal(getMySide()).y + newStatus.conf.paddleHeight * 0.5, timeLeft, newStatus.conf.paddleHeight);
    }

    public void changeCourse(double distance, double timeLeft) {

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

    public boolean needToReact(double targetPos, double timeLeft) {
        double myPos = lastKnownStatus.getPedal(getMySide()).y;
        double movingDistance = timeLeft * myState.velocity() * getPaddleMaxVelocity();

        double ballEndPos = ballWorkMemory.y;
        double expectedPosition = movingDistance + myPos + lastKnownStatus.conf.paddleHeight * 0.5;
        double expectedDistance = ballEndPos - expectedPosition;

        double halfPaddle = lastKnownStatus.conf.paddleHeight * 0.1;
        return expectedDistance * expectedDistance >= halfPaddle * halfPaddle || reallyShouldUpdateRegardless();
    }

    public boolean requestChangeSpeed(double v) {
        if(super.requestChangeSpeed(v)) {
            myState.setVelocity(v);
            return true;
        }
        return false;
    }

    @Override
    public void onGameOver(boolean won) {
        gameOverHandler.onGameOver(won);
        missileCommand.unCommit();
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

    public SauronState getMyState() {
        return myState;
    }

    public ClientGameState.Ball getBallWorkMemory() {
        return ballWorkMemory;
    }

    public ArrayList<UILine> getLines() {
        return lines;
    }

    public MissileCommand getMissileCommand() {
        return missileCommand;
    }

    public SauronVisualiser getSauronVisualiser() {
        return sauronVisualiser;
    }

    public PongModel getMyModel() {
        return myModel;
    }
}

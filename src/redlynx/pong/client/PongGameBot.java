package redlynx.pong.client;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import redlynx.bots.finals.sauron.MissileCommand;
import redlynx.pong.client.network.Communicator;
import redlynx.pong.client.network.MessageLimiter;
import redlynx.pong.client.network.PongMessageListener;
import redlynx.pong.client.network.PongMessageParser;
import redlynx.pong.client.state.BallPositionHistory;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.client.state.GameStateAccessor;
import redlynx.pong.client.state.GameStatusSnapShot;
import redlynx.pong.client.state.MissileState;
import redlynx.pong.client.state.PaddleVelocityStorage;
import redlynx.pong.client.ui.LineVisualizer;
import redlynx.pong.collisionmodel.LinearModel;
import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.PongVisualizer;
import redlynx.pong.ui.UILine;
import redlynx.pong.ui.UIString;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.SoftVariable;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;
import redlynx.pong.util.Visualisation;

public abstract class PongGameBot implements PongMessageListener, PongMessageParser.Handler, LineVisualizer {


    //private final CollisionStorage collisionStorage = new CollisionStorage();

    private double totalTime = 0;
    private final ArrayList<Avoidable> avoidables = new ArrayList<Avoidable>();
    private final ArrayList<Avoidable> offensiveMissiles = new ArrayList<Avoidable>();
    private final BallPositionHistory ballPositionHistory = new BallPositionHistory();
    private final PaddleVelocityStorage paddleVelocity = new PaddleVelocityStorage();
    private final SoftVariable ballVelocity = new SoftVariable(50);

    public final ArrayList<UIString> strings = new ArrayList<UIString>();
    public final ArrayList<UILine> lines = new ArrayList<UILine>();

    public final MissileCommand missileCommand = new MissileCommand(this);
    private final MessageLimiter messageLimiter = new MessageLimiter();
    public PongModel myModel = new LinearModel();

    private int numWins = 0;
    private int numGames = 0;
    private boolean ballEndPosHistoryMarker = true; // for tracking ball target history.

    public final ClientGameState.Ball ballWorkMemory = new ClientGameState.Ball();
    public final ClientGameState.Ball ballTemp = new ClientGameState.Ball();
    private long lastMissileFiredTime = 0;

    public void setName(String name) {
        this.name = name;
    }

    public void setCommunicator(Communicator communicator) {
        this.communicator = communicator;
    }

    public void setVisualizer(PongVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    public void messageReceived(String msg) {
        handleMessage(msg);
    }

    private synchronized void handleMessage(String serverMessage) {
        messageParser.onReceivedJSONString(serverMessage);
    }

    public ArrayList<Avoidable> getAvoidables() {
        return avoidables;
    }

    public ArrayList<Avoidable> getOffensiveMissiles() {
        return offensiveMissiles;
    }

    public void setExtrapolatedTime(double extrapolatedTime) {
        this.extrapolatedTime = extrapolatedTime;
    }

    public int getMissileCount() {
        return availableMissiles.size();
    }

    public static class Avoidable {

        public Avoidable(double y, double t) {
            this.y = y;
            this.t = t;
        }

        public void tick(double dt) {
            t -= dt;
        }

        public boolean active() {
            return t > 0;
        }
        public double y;
        public double t;

    }

    public static enum PlayerSide {
        LEFT(-1),
        RIGHT(+1);

        private final float side;

        PlayerSide(float side) {
            this.side = side;
        }

        public boolean comingTowardsMe(double ball_direction) {
            return side * ball_direction >= 0;
        }

        public static PlayerSide getOtherSide(PlayerSide catcher) {
            if(catcher == LEFT)
                return RIGHT;
            return LEFT;
        }
    }

    private PongVisualizer visualizer;

    private Queue<Long> availableMissiles;
    private Communicator communicator;
    private final PongMessageParser messageParser;
    private String name;
    private PlayerSide mySide;

    public final ClientGameState lastKnownStatus = new ClientGameState();
    public final ClientGameState extrapolatedStatus = new ClientGameState();
    public double extrapolatedTime = 0.0;

    private long currentTime = System.currentTimeMillis();

    public PaddleVelocityStorage getPaddleVelocity() {
        return paddleVelocity;
    }

    public double getPaddleMaxVelocity() {
        return paddleVelocity.estimate;
    }

    public void setMySide(PlayerSide side) {
        mySide = side;
    }

    public PlayerSide getMySide() {
        return mySide;
    }

    private GameStateAccessor accessor;

    public PongGameBot() {
        availableMissiles = new ArrayDeque<Long>();
        messageParser = new PongMessageParser(this);
        accessor = new GameStateAccessor(this);
        //collisionStorage.read(getName());
    }

    public Vector2 getPaddlePossibleReturns(ClientGameState state, ClientGameState.Ball ball, PlayerSide side, double timeLeft) {
        Vector2 ans = new Vector2();
        double paddleMid = state.getPedal(side).y + 0.5 * state.conf.paddleHeight;
        double maxReach = paddleMid + timeLeft * getPaddleMaxVelocity();
        double minReach = paddleMid - timeLeft * getPaddleMaxVelocity();
        maxReach -= ball.y;
        minReach -= ball.y;
        maxReach /= 0.5 * state.conf.paddleHeight;
        minReach /= 0.5 * state.conf.paddleHeight;
        maxReach = Math.min(+1, maxReach);
        minReach = Math.max(-1, minReach);
        ans.x = -maxReach;
        ans.y = -minReach;
        return ans;
    }

    public GameStateAccessorInterface getGameStateAccessor() {
    	return accessor;
    }

    // quite accurate approximation of the ball velocity
    public double getBallVelocity() {
        return ballVelocity.value();
    }
   
    @Override
	public void missileReady(long missileId) {
        System.out.println("Got missile!");
    	availableMissiles.add(missileId);
    }

    public boolean hasMissiles() {
    	return availableMissiles.size() > 0;
    }

    public boolean fireMissile() {
    	  if(messageLimiter.canSend() && hasMissiles() && System.currentTimeMillis() - lastMissileFiredTime > 200) {
              getCommunicator().sendFireMissile(availableMissiles.remove());
              messageLimiter.send();
              lastMissileFiredTime = System.currentTimeMillis();
              return true;
          }
          return false;
    }
    
    @Override
	public void missileLaunched(MissileState missile) {

        // probably no point keeping track of missiles we have fired.
        // NOTE: We assume here that we are always playing on the left side.
        if(missile.vel.x > 0) {
            double missileVelocityX = Math.abs(1000 * missile.vel.x / lastKnownStatus.conf.tickInterval); // assumes 20ms physics step size.
            double positionX = missile.pos.x;
            double time = (lastKnownStatus.conf.maxWidth - positionX) / missileVelocityX;
            missileCommand.setMissileTime(time);
            offensiveMissiles.add(new Avoidable(missile.pos.y, time));
        }
        else {
            // find out how many seconds we have until missile hits.
            double missileVelocityX = Math.abs(1000 * missile.vel.x / lastKnownStatus.conf.tickInterval); // assumes 20ms physics step size.
            double positionX = missile.pos.x;
            double time = positionX / missileVelocityX;
            avoidables.add(new Avoidable(missile.pos.y, time));
            missileCommand.setMissileTime(time);
        }
    }
    

    @Override
   	public void gameStateUpdate(GameStatusSnapShot snap) {

        ClientGameState gameStatus = new ClientGameState(snap);

    	// TODO encapsulate inside missile handler or something
    	{
            double dt = (gameStatus.time - lastKnownStatus.time) * 0.001;
			for (int i=0; i<avoidables.size(); ++i) {
                Avoidable avoidable = avoidables.get(i);
                avoidable.tick(dt);
                if(!avoidable.active()) {
                    System.out.println("removing avoidable");
                    avoidables.remove(avoidable);
                    --i;
                }
			}

            for (int i=0; i<offensiveMissiles.size(); ++i) {
                Avoidable avoidable = offensiveMissiles.get(i);
                avoidable.tick(dt);
                if(!avoidable.active()) {
                    System.out.println("removing offensive missile");
                    offensiveMissiles.remove(avoidable);
                    --i;
                }
            }
    	}

        if(ballPositionHistory.isReliable()) {
            if(ballEndPosHistoryMarker && lastKnownStatus.ball.vx < 0) {
                ClientGameState.Ball ballCopy = new ClientGameState.Ball();
                ballCopy.copy(lastKnownStatus.ball, true);
                PongUtil.simulate(ballCopy, lastKnownStatus.conf);
                //collisionStorage.push(new CollisionStorage.EndPointCollision(ballCopy));
                ballEndPosHistoryMarker = false;
            }
            else if(!ballEndPosHistoryMarker && lastKnownStatus.ball.vx > 0) {
                ClientGameState.Ball ballCopy = new ClientGameState.Ball();
                ballCopy.copy(lastKnownStatus.ball, true);
                PongUtil.simulate(ballCopy, lastKnownStatus.conf);
                //collisionStorage.push(new CollisionStorage.EndPointCollision(ballCopy));
                ballEndPosHistoryMarker = true;
            }
        }

        paddleVelocity.update(gameStatus.getPedal(mySide).y, gameStatus.time);
        ballPositionHistory.update(gameStatus.ball.getPosition());
        double step_dt = (gameStatus.time - lastKnownStatus.time) / 1000.0;

        if(ballPositionHistory.isReliable() && step_dt != 0) {
            lastKnownStatus.update(gameStatus, true);
            lastKnownStatus.copy(gameStatus, true);
            extrapolatedStatus.copy(gameStatus, true);
            ballVelocity.update(gameStatus.ball.getVelocity().length());
        }
        else {
            Vector2 collisionPoint = ballPositionHistory.getLastCollisionPoint();
            double collisionTime = ballPositionHistory.getLastCollisionTime();
            double dt = totalTime - collisionTime;

            if(collisionPoint != null && dt > 0.3) {
                Vector2 directionVelocity = new Vector2();
                directionVelocity.copy(gameStatus.ball.getPosition().minus(collisionPoint));
                directionVelocity.scaled(1.0 / (dt + 0.0000001));

                lastKnownStatus.update(gameStatus, false);
                lastKnownStatus.copy(gameStatus, true);
                lastKnownStatus.ball.vx = directionVelocity.x;
                lastKnownStatus.ball.vy = directionVelocity.y;
                extrapolatedStatus.copy(lastKnownStatus, true);
            }
            else {
                lastKnownStatus.update(gameStatus, false);
                lastKnownStatus.copy(gameStatus, true);
                lastKnownStatus.ball.vx = extrapolatedStatus.ball.vx;
                lastKnownStatus.ball.vy = extrapolatedStatus.ball.vy;
                extrapolatedStatus.copy(lastKnownStatus, true);
            }
        }

        extrapolatedTime = 0;


        long startTime = System.nanoTime();
        onGameStateUpdate(lastKnownStatus);
        long time = System.nanoTime() - startTime;
        float decisionTime = (time / 1000000.0f);


        for(Avoidable avoidable : getAvoidables()) {
            Visualisation.drawCross(lines, Color.pink, avoidable.t * 300, avoidable.y);
        }
        for(Avoidable avoidable : getOffensiveMissiles()) {
            Visualisation.drawCross(lines, Color.green, lastKnownStatus.conf.maxWidth - avoidable.t * 300, avoidable.y);
        }

        if(hasMissiles()) {
            Visualisation.drawSquare(lines, Color.green, 100, 20);
        }
        
        if (visualizer != null) {
        	visualizer.render();
        }
    }

    @Override
	public void gameStart(String player1, String player2) {
        // Server does not swap name orders. Always select left manually.
    	if(true || player1.equals(name)) {
            // System.out.println("I'm left");
            setMySide(PlayerSide.LEFT);
        }
    	else {
            // System.out.println("I'm right");
            setMySide(PlayerSide.RIGHT);
        }
    }

    @Override
	public void gameOver(String winner) {
    	gameOver(winner.equals(name));
    }
   

    public void gameOver(boolean won) {

    	/*
        if(lastKnownStatus.ball.x > 40 && lastKnownStatus.ball.x < lastKnownStatus.conf.maxWidth - 40) {
            System.out.println("Game ended by missile.");
            collisionStorage.clear();
        }
        else {
            collisionStorage.end(won);
            collisionStorage.write(getName());
        }
        */

        ballPositionHistory.reset();
        lastKnownStatus.reset();
        extrapolatedStatus.reset();
        ballVelocity.reset(150);
        paddleVelocity.reset(100);
        avoidables.clear();
        offensiveMissiles.clear();
        availableMissiles.clear();
        onGameOver(won);

        ++numGames;
        if(won) {
            ++numWins;
        }

        strings.clear();
        strings.add(new UIString("" + numWins, new Vector2i(lastKnownStatus.conf.maxWidth * 0.5 - 75, lastKnownStatus.conf.maxHeight + 20), Color.green));
        strings.add(new UIString("" + (numGames - numWins), new Vector2i(lastKnownStatus.conf.maxWidth * 0.5 + 75, lastKnownStatus.conf.maxHeight + 20), Color.red));
    }

    public abstract void onGameStateUpdate(ClientGameState newStatus);
    public abstract void onGameOver(boolean won);
    public abstract void onTick(double dt);
    public abstract String getDefaultName();

    public void start() {
        while(true) {

        	synchronized (this) {
        		long newTime = System.currentTimeMillis();
                tick(newTime - currentTime);
                currentTime = newTime;	
			}
            
            try {
                //only tick max 100 times per second. (A bit extra never hurt anyone).
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // if interrupted, exit program.
                break;
            }
        }
    }

    private void tick(long time) {
        double dt = time * 0.001;

        if(extrapolatedStatus.extrapolate(dt)) {
            ballPositionHistory.storeCollision(extrapolatedStatus.ball.getPosition(), totalTime);
        }

        totalTime += dt;
        extrapolatedTime += dt;

        messageLimiter.tick(dt);

        onTick(dt);
        if (visualizer != null)
        	visualizer.render();
    }

    public String getName() {
        return name;
    }

	public void ballCollideToPaddle(double paddleRelativePos, ClientGameState.Ball ball) {
		Vector2 ballOut = myModel.guess(paddleRelativePos, ball.vx, ball.vy);
        ballOut.normalize().scaled(getBallVelocity() + 15);
        ball.vx = ballOut.x;
        ball.vy = ballOut.y;
        ball.tick(0.02f);
    }

    public boolean requestChangeSpeed(double v) {
        if(messageLimiter.canSend()) {
            paddleVelocity.update(v);
            getCommunicator().sendUpdate((float) v);
            messageLimiter.send();
            return true;
        }
        return false;
    }

    // if have not sent an update in a long time, then update.
    public boolean reallyShouldUpdateRegardless() {
        return messageLimiter.shouldUpdate();
    }

    private Communicator getCommunicator() {
        return communicator;
    }

    public ClientGameState getLastKnownStatus() {
        return lastKnownStatus;
    }

    public ClientGameState getExtrapolatedStatus() {
        return extrapolatedStatus;
    }

    public BallPositionHistory getBallPositionHistory() {
        return ballPositionHistory;
    }

    @Override
    public ArrayList<UIString> getDrawStrings() {
        return this.strings;
    }
}

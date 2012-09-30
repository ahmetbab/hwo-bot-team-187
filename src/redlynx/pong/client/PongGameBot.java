package redlynx.pong.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import redlynx.pong.client.network.Communicator;
import redlynx.pong.client.network.MessageLimiter;
import redlynx.pong.client.network.PongMessageListener;
import redlynx.pong.client.network.PongMessageParser;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.client.state.GameStateAccessor;
import redlynx.pong.client.state.GameStatusSnapShot;
import redlynx.pong.client.state.BallPositionHistory;
import redlynx.pong.client.state.MissileState;
import redlynx.pong.client.state.PaddleVelocityStorage;
import redlynx.pong.client.ui.LineVisualizer;
import redlynx.pong.collisionmodel.LinearModel;
import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.PongVisualizer;
import redlynx.pong.util.SoftVariable;
import redlynx.pong.util.Vector2;

public abstract class PongGameBot implements PongMessageListener, PongMessageParser.Handler, LineVisualizer {

    private double totalTime = 0;
    private final ArrayList<Avoidable> avoidables = new ArrayList<Avoidable>();
    private final BallPositionHistory ballPositionHistory = new BallPositionHistory();
    private final PaddleVelocityStorage paddleVelocity = new PaddleVelocityStorage();
    private final SoftVariable ballVelocity = new SoftVariable(50);
    private final MessageLimiter messageLimiter = new MessageLimiter();

    public PongModel myModel = new LinearModel();
    public final ClientGameState.Ball ballWorkMemory = new ClientGameState.Ball();

    public final ClientGameState.Ball ballTemp = new ClientGameState.Ball();

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
        messageParser.onReceivedJSONString(msg);
    }

    public ArrayList<Avoidable> getAvoidables() {
        return avoidables;
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

    private Queue<Long> missiles;
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
        missiles = new ArrayDeque<Long>();
        messageParser = new PongMessageParser(this);
        accessor = new GameStateAccessor(this);
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
    	missiles.add(missileId);
    }

    public boolean hasMissiles() {
    	return missiles.size() > 0;
    }

    public boolean fireMissile() {
    	  if(messageLimiter.canSend() && hasMissiles()) {
              getCommunicator().sendFireMissile(missiles.remove());
              messageLimiter.send();
              return true;
          }
          return false;
    }
    
    @Override
	public void missileLaunched(MissileState missile) {

        // probably no point keeping track of missiles we have fired.
        // NOTE: We assume here that we are always playing on the left side.
        if(missile.vel.x > 0)
            return;

        // find out how many seconds we have until missile hits.
        double missileVelocityX = (1000 * missile.vel.x / 20); // assumes 20ms physics step size.
        double positionX = missile.pos.x;
        double time = positionX / missileVelocityX;
        avoidables.add(new Avoidable(missile.pos.y, time));
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
                if(!avoidable.active())
                    avoidables.remove(avoidable);
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
        ballPositionHistory.reset();
        lastKnownStatus.reset();
        extrapolatedStatus.reset();
        ballVelocity.reset(150);
        paddleVelocity.reset(100);
        avoidables.clear();
        missiles.clear();
        onGameOver(won);
    }

    public abstract void onGameStateUpdate(ClientGameState newStatus);
    public abstract void onGameOver(boolean won);
    public abstract void onTick(double dt);
    public abstract String getDefaultName();

    private synchronized void handleMessage(String serverMessage) {
    	messageParser.onReceivedJSONString(serverMessage);
    }

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
}

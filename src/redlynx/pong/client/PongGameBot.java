package redlynx.pong.client;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import redlynx.pong.client.collisionmodel.LinearModel;
import redlynx.pong.client.collisionmodel.PongModel;
import redlynx.pong.client.network.MessageLimiter;
import redlynx.pong.client.state.*;
import redlynx.pong.util.SoftVariable;
import redlynx.pong.client.BaseBot;
import redlynx.pong.client.network.Communicator;
import redlynx.pong.client.network.PongMessageParser;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.PongVisualizer;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.Vector2;

public abstract class PongGameBot implements BaseBot, PongMessageParser.ParsedMessageListener {

    private double totalTime = 0;
    private final History history = new History();
    private final PaddleVelocityStorage paddleVelocity = new PaddleVelocityStorage();
    private final SoftVariable ballVelocity = new SoftVariable(50);
    private final MessageLimiter messageLimiter = new MessageLimiter();
    public final PongModel myModel = new LinearModel();

    public final ClientGameState.Ball ballWorkMemory = new ClientGameState.Ball();
    public final ClientGameState.Ball ballTemp = new ClientGameState.Ball();

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

    private final Queue<String> serverMessageQueue;
    private Communicator communicator;
    private final PongMessageParser handler;
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
    	//this.name = name;
        this.serverMessageQueue = new ConcurrentLinkedQueue<String>();
        this.handler = new PongMessageParser(this);
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

    public void setName(String name) {
    	this.name = name;
    } 

    public void setCommunicator(Communicator comm) {
    	this.communicator = comm;
    }

    // quite accurate approximation of the ball velocity
    public double getBallVelocity() {
        return ballVelocity.value();
    }

    @Override
    public void messageReceived(String msg) {
    	serverMessageQueue.add(msg);
    }
    
    public void setVisualizer(PongVisualizer visualizer) {
    	this.visualizer = visualizer;
    }

    @Override
   	public void gameStateUpdate(GameStatusSnapShot snap) {
       ClientGameState gameStatus = new ClientGameState(snap);       

        paddleVelocity.update(gameStatus.getPedal(mySide).y, gameStatus.time);
        history.update(gameStatus.ball.getPosition());
        double step_dt = (gameStatus.time - lastKnownStatus.time) / 1000.0;

        if(history.isReliable() && step_dt != 0) {
            lastKnownStatus.update(gameStatus, true);
            lastKnownStatus.copy(gameStatus, true);
            extrapolatedStatus.copy(gameStatus, true);
            ballVelocity.update(gameStatus.ball.getVelocity().length());
        }
        else {
            Vector2 collisionPoint = history.getLastCollisionPoint();
            double collisionTime = history.getLastCollisionTime();
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

        // to avoid any confusion later..
        onGameStateUpdate(lastKnownStatus);

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
        history.reset();
        lastKnownStatus.reset();
        extrapolatedStatus.reset();
        ballVelocity.reset(50);
        onGameOver(won);
    }

    public abstract void onGameStateUpdate(ClientGameState newStatus);
    public abstract void onGameOver(boolean won);
    public abstract void onTick(double dt);
    public abstract String getDefaultName();
    public abstract ArrayList<UILine> getDrawLines();

    @Override
    public void start() {
        while(true) {
        	while (!serverMessageQueue.isEmpty()) {
        		handler.onReceivedJSONString(serverMessageQueue.remove());
        	}

            long newTime = System.currentTimeMillis();
            tick(newTime - currentTime);
            currentTime = newTime;

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // if interrupted, exit program.
                break;
            }
        }
    }

    private void tick(long time) {
        double dt = time * 0.001;

        if(extrapolatedStatus.extrapolate(dt)) {
            history.storeCollision(extrapolatedStatus.ball.getPosition(), totalTime);
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
        ballOut.normalize().scaled(getBallVelocity());
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

    public History getHistory() {
        return history;
    }
}

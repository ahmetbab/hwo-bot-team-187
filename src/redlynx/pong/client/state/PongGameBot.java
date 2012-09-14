package redlynx.pong.client.state;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import redlynx.pong.client.BaseBot;
import redlynx.pong.client.network.Communicator;
import redlynx.pong.client.network.PongMessageParser;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.PongVisualizer;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;

public abstract class PongGameBot implements BaseBot {

    private double totalTime = 0;
    private final History history = new History();
    private final VelocityStorage storage = new VelocityStorage();

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
    }

    private PongVisualizer visualizer;
    
    private final Queue<String> serverMessageQueue;
    private Communicator communicator;
    private final PongMessageParser handler;
    private String name;
    private PlayerSide mySide;

    public final GameStatus lastKnownStatus = new GameStatus();
    public final GameStatus extrapolatedStatus = new GameStatus();
    public double extrapolatedTime = 0.0;

    private long currentTime = System.currentTimeMillis();

   
    
    public VelocityStorage getStorage() {
        return storage;
    }

    public double getPaddleMaxVelocity() {
        return getStorage().estimate;
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
        this.serverMessageQueue = new ConcurrentLinkedQueue<String>();;
        //this.communicator = communicator;
        this.handler = new PongMessageParser(this);
        accessor = new GameStateAccessor(this);
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
    
    @Override
    public void messageReceived(String msg) {
    	serverMessageQueue.add(msg);
    }
    
    public void setVisualizer(PongVisualizer visualizer) {
    	this.visualizer = visualizer;
    }

    public void gameStateUpdate(GameStatus gameStatus) {

        double squareError = PongUtil.pointDistance2Line(extrapolatedStatus.ball.getPosition(), extrapolatedStatus.ball.getNextPosition(), gameStatus.ball.getPosition());

        storage.update(gameStatus.getPedal(mySide).y, gameStatus.time);
        history.update(gameStatus.ball.getPosition());

        if(history.isReliable()) {
            lastKnownStatus.update(gameStatus, true);
            lastKnownStatus.copy(gameStatus, true);
            extrapolatedStatus.copy(gameStatus, true);
        }
        else {
            Vector2 collisionPoint = history.getLastCollisionPoint();
            double collisionTime = history.getLastCollisionTime();
            double dt = totalTime - collisionTime;

            if(collisionPoint != null && dt > 0.1) {
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

        onGameStateUpdate(gameStatus);
        if (visualizer != null) {
        	visualizer.render();
        }

    }

    public void gameOver(boolean won) {
        history.reset();
        lastKnownStatus.reset();
        extrapolatedStatus.reset();
        onGameOver(won);
    }

    public abstract void onGameStateUpdate(GameStatus newStatus);
    public abstract void onGameOver(boolean won);
    public abstract void onTick(double dt);
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

        onTick(dt);
        if (visualizer != null)
        	visualizer.render();
    }

    public String getName() {
        return name;
    }

    public void requestChangeSpeed(double v) {
        storage.update(v);
        getCommunicator().sendUpdate((float) v);
    }

    private Communicator getCommunicator() {
        return communicator;
    }

    public GameStatus getLastKnownStatus() {
        return lastKnownStatus;
    }

    public GameStatus getExtrapolatedStatus() {
        return extrapolatedStatus;
    }

    public History getHistory() {
        return history;
    }
}

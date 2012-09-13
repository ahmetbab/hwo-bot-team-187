package redlynx.pong.client.state;

import java.util.Queue;

import redlynx.pong.client.network.Communicator;
import redlynx.pong.client.network.PongGameCommunicator;
import redlynx.pong.client.network.PongMessageParser;
import redlynx.pong.ui.PongVisualizer;

public abstract class PongGameBot {

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
    private final Communicator communicator;
    private final PongMessageParser handler;
    private final String name;
    private PlayerSide mySide;

    public final GameStatus lastKnownStatus = new GameStatus();
    public final GameStatus extrapolatedStatus = new GameStatus();
    public double extrapolatedTime = 0.0;

    private long currentTime = System.currentTimeMillis();

    public void setMySide(PlayerSide side) {
        mySide = side;
    }

    public PlayerSide getMySide() {
        return mySide;
    }

    public PongGameBot(String name, Communicator communicator, Queue<String> serverMessageQueue) {
    	this.name = name;
        this.serverMessageQueue = serverMessageQueue;
        this.communicator = communicator;
        this.handler = new PongMessageParser(this);
    }
    
    public void setVisualizer(PongVisualizer visualizer) {
    	this.visualizer = visualizer;
    }

    public void gameStateUpdate(GameStatus gameStatus) {
        lastKnownStatus.update(gameStatus);
        lastKnownStatus.copy(gameStatus);

        double error_x = lastKnownStatus.ball.x - extrapolatedStatus.ball.x;
        double error_y = lastKnownStatus.ball.y - extrapolatedStatus.ball.y;

        System.out.println("Error (" + extrapolatedTime + "ms): " + (error_x * error_x + error_y * error_y));

        extrapolatedStatus.copy(gameStatus);
        extrapolatedTime = 0;
        
       
        
        onGameStateUpdate(gameStatus);
        if (visualizer != null)
        	visualizer.render();
        
    }

    public abstract void onGameStateUpdate(GameStatus newStatus);
    public abstract void onGameOver(boolean won);
    public abstract void onTick(double dt);

    public void start() {

        //System.out.println("Sending join");
        //this.communicator.sendJoin(name);

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
        extrapolatedStatus.extrapolate(dt);
        extrapolatedTime += dt;
        // try to collect statistics
        // extrapolatedStatus.hits(PlayerSide.LEFT, extrapolatedStatus.ball);
        // extrapolatedStatus.hits(PlayerSide.RIGHT, extrapolatedStatus.ball);

        onTick(dt);
        if (visualizer != null)
        	visualizer.render();
    }

    public String getName() {
        return name;
    }

    public Communicator getCommunicator() {
        return communicator;
    }



    public GameStatus getLastKnownStatus() {
        return lastKnownStatus;
    }

    public GameStatus getExtrapolatedStatus() {
        return extrapolatedStatus;
    }
}

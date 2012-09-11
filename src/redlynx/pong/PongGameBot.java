package redlynx.pong;

import java.util.Queue;

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

    private final Queue<String> serverMessageQueue;
    private final PongGameCommunicator communicator;
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

    public PongGameBot(String name, PongGameCommunicator communicator, Queue<String> serverMessageQueue) {
    	this.name = name;
        this.serverMessageQueue = serverMessageQueue;
        this.communicator = communicator;
        this.handler = new PongMessageParser(this);
    }

    public void gameStateUpdate(GameStatus gameStatus) {
        lastKnownStatus.update(gameStatus);
        extrapolatedStatus.copy(gameStatus);
        extrapolatedTime = 0;
        onGameStateUpdate(gameStatus);
    }

    public abstract void onGameStateUpdate(GameStatus newStatus);
    public abstract void onGameOver(boolean won);
    public abstract void onTick(double dt);

    public void start() {

        System.out.println("Sending join");
        this.communicator.sendJoin(name);

        while(true) {
        	while (!serverMessageQueue.isEmpty()) {
        		handler.onReceivedJSONString(serverMessageQueue.remove());
        	}

            long newTime = System.currentTimeMillis();
            tick(currentTime - newTime);
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
        onTick(dt);
    }

    public String getName() {
        return name;
    }

    public PongGameCommunicator getCommunicator() {
        return communicator;
    }
}

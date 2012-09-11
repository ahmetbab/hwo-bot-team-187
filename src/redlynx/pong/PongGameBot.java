package redlynx.pong;

import java.util.Queue;

public abstract class PongGameBot {

    private final Queue<String> serverMessageQueue;
    private final PongGameCommunicator communicator;
    private final PongMessageParser handler;
    private final String name;

    private final GameStatus lastKnownStatus = new GameStatus();
    private final GameStatus extrapolatedStatus = new GameStatus();
    private double extrapolatedTime = 0.0;

    public PongGameBot(String name, PongGameCommunicator communicator, Queue<String> serverMessageQueue) {
    	this.name = name;
        this.serverMessageQueue = serverMessageQueue;
        this.communicator = communicator;
        this.handler = new PongMessageParser(this);
    }

    public void gameStateUpdate(GameStatus gameStatus) {
        lastKnownStatus.update(gameStatus);
        extrapolatedStatus.copy(gameStatus);
        onGameStateUpdate(gameStatus);
        extrapolatedTime = 0;
    }

    public abstract void onGameStateUpdate(GameStatus newStatus);
    public abstract void onGameOver(boolean won);

    public void start() {

        this.communicator.sendJoin(name);

        while(true) {
        	while (!serverMessageQueue.isEmpty()) {
        		handler.onReceivedJSONString(serverMessageQueue.remove());
        	}

            tick(System.currentTimeMillis());

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // if interrupted, exit program.
                break;
            }
        }
    }

    private void tick(long time) {
        extrapolatedStatus.extrapolate(time * 0.001);
    }

    public String getName() {
        return name;
    }
}

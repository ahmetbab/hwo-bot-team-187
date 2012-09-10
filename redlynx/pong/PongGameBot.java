package redlynx.pong;

import java.util.concurrent.ArrayBlockingQueue;

public abstract class PongGameBot {

    private final ArrayBlockingQueue<String> serverMessageQueue;
    private final PongGameCommunicator communicator;

    protected abstract String getName();

    // TODO: Create base implementation. Abstraction should happen on a more detailed level.
    protected abstract void onReceivedJSONString(String serverMessage);

    // TODO: Create basic upkeep of game state. Add an abstract tick call, in case bot wants
    // TODO: to add some actions to the basic upkeep phase.
    protected abstract void onTick(long time);

    public PongGameBot(PongGameCommunicator communicator, ArrayBlockingQueue<String> serverMessageQueue) {
        this.serverMessageQueue = serverMessageQueue;
        this.communicator = communicator;
    }


    public void start() {

        this.communicator.sendJoin(getName());

        while(true) {
            String serverMessage = serverMessageQueue.poll();
            if(serverMessage != null) {
                onReceivedJSONString(serverMessage);
            }

            onTick(System.currentTimeMillis());

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                // if interrupted, exit program.
                break;
            }
        }
    }

}

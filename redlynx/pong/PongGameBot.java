package redlynx.pong;

import java.util.concurrent.ArrayBlockingQueue;

public abstract class PongGameBot {

    private final ArrayBlockingQueue<String> serverMessageQueue;
    private final PongGameCommunicator communicator;

    protected abstract String getName();
    protected abstract void onReceivedJSONString(String serverMessage);
    protected abstract void onTick(long time);

    public PongGameBot(PongGameCommunicator communicator, ArrayBlockingQueue<String> serverMessageQueue) {
        this.serverMessageQueue = serverMessageQueue;
        this.communicator = communicator;

        communicator.sendJoin(getName());
    }


    public void start() {
        while(true) {
            String serverMessage = serverMessageQueue.poll();
            if(serverMessage != null) {
                onReceivedJSONString(serverMessage);
            }

            onTick(System.currentTimeMillis());

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // if interrupted, exit program.
                break;
            }
        }
    }

}

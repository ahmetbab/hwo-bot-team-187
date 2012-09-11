package redlynx.mikabot;

import redlynx.pong.GameStatus;
import redlynx.pong.PongGameBot;
import redlynx.pong.PongGameCommunicator;

import java.util.Queue;

public class TestBot extends PongGameBot {

    public TestBot(String name, PongGameCommunicator communicator, Queue<String> serverMessageQueue) {
        super(name, communicator, serverMessageQueue);
    }

    @Override
    public void onGameStateUpdate(GameStatus newStatus) {
        // :D
    }

    @Override
    public void onGameOver(boolean won) {
        // :D
    }
}

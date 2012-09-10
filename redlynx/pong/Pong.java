package redlynx.pong;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

public class Pong {
    private final Socket connection;
    private final InputStream netInput;
    private final OutputStream netOutput;

    private final PrintStream out;
    private final ArrayBlockingQueue<String> serverMessagesQueue = new ArrayBlockingQueue<String>(20);

    private final PongListenerThread listenerThread;
    private final PongGameBot gameData;
    private final PongGameCommunicator communicator;

    public Pong(String name, String host, int port) throws IOException {
        connection = new Socket(host, port);
        netInput = connection.getInputStream();
        netOutput = connection.getOutputStream();
        out = new PrintStream(netOutput);
        communicator = new PongGameCommunicator(out);

        // start server message listener
        listenerThread = new PongListenerThread(netInput, serverMessagesQueue);
        listenerThread.run();

        // start game state loop
        gameData = new PongGameBot(communicator, serverMessagesQueue);
        gameData.start();

        // when game is over, shut down listener thread
        listenerThread.interrupt();
    }

}
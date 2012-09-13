package redlynx.pong;

import redlynx.test.TestBot;
import redlynx.pong.network.PongGameCommunicator;
import redlynx.pong.network.PongListenerThread;
import redlynx.pong.state.PongGameBot;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Pong {
    private  Socket connection;
    private final InputStream netInput;

    
    private final PrintStream out;
    private final Queue<String> serverMessageQueue;
    private final PongListenerThread listenerThread;
    private final PongGameBot pongBot;
    private final PongGameCommunicator communicator;

    public Pong(String name, String host, int port) throws IOException {
        connection = new Socket(host, port);
        serverMessageQueue = new ConcurrentLinkedQueue<String>();
        netInput = connection.getInputStream();
        out = new PrintStream(connection.getOutputStream());
        communicator = new PongGameCommunicator(out);

        // start server message listener
        listenerThread = new PongListenerThread(netInput, serverMessageQueue);
        listenerThread.start();

        // start game state loop
        pongBot = new TestBot(name, communicator, serverMessageQueue);
        pongBot.start();

        // when game is over, shut down listener thread
        listenerThread.interrupt();
    }

    public static void main(String[] args) {
    	
    	if (args.length != 3) {
    		System.err.println("Invalid arguments: USAGE: Pong name host port");
    		System.exit(-1);
    	}
    	
    	String name = args[0];
    	String host = args[1];
    	String port = args[2];

        System.out.println("name: " + name);
        System.out.println("host: " + host);
        System.out.println("port: " + port);

        try {
			new Pong(name, host, Integer.parseInt(port));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	
    }
    
}
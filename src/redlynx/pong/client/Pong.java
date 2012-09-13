package redlynx.pong.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JFrame;

import redlynx.pong.client.network.PongGameCommunicator;
import redlynx.pong.client.network.PongListenerThread;
import redlynx.pong.client.state.GameStateAccessor;
import redlynx.pong.client.state.PongGameBot;
import redlynx.pong.client.ui.PongClientFrame;
import redlynx.pong.ui.PongVisualizer;
import redlynx.pong.util.WinTimerHack;
import redlynx.test.TestBot;

public class Pong {
    private  Socket connection;
    private final InputStream netInput;

    
    private final PrintStream out;
    private final Queue<String> serverMessageQueue;
    private final PongListenerThread listenerThread;
    private final PongGameBot pongBot;
    private final PongGameCommunicator communicator;

    private PongVisualizer visualizer;
    
    public Pong(String name, String host, int port, boolean visualize) throws IOException {
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
        
        if (visualize) {
        	GameStateAccessor accessor = new GameStateAccessor(pongBot);
        	visualizer = new PongVisualizer(accessor);
        	JFrame frame = new PongClientFrame(name, visualizer, accessor);
        }
        pongBot.setVisualizer(visualizer);
        
        pongBot.start();

        
        
        // when game is over, shut down listener thread
        listenerThread.interrupt();
    }

    public static void main(String[] args) {
    	
    	if (args.length < 3) {
    		System.err.println("Invalid arguments: USAGE: Pong name host port [-vis]");
    		System.exit(-1);
    	}
    	WinTimerHack.fixTimerAccuracy();
    	
    	String name = args[0];
    	String host = args[1];
    	String port = args[2];
    	boolean visualize = args.length == 4 && args[3].equals("-vis");

        System.out.println("name: " + name);
        System.out.println("host: " + host);
        System.out.println("port: " + port);

        try {
			new Pong(name, host, Integer.parseInt(port), visualize);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	
    }
    
}
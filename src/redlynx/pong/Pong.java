package redlynx.pong;

import redlynx.mikabot.TestBot;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketAddress;
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

        System.out.println("Starting");

        connection = new Socket(host, port);
        serverMessageQueue = new ConcurrentLinkedQueue<String>();
        netInput = connection.getInputStream();
        out = new PrintStream(connection.getOutputStream());
        
        
        /*
        String testdata = 
        	 "{\"msgType\":\"joined\",\"data\":\"http://boris.helloworldopen.fi/visualize.html#game_id\"}\n"+
        	 "{\"msgType\":\"gameStarted\",\"data\":[\"JohnMcEnroe\", \"BorisBecker\"]}\n"+
        		
        	 
    		"{ \"msgType\":\"gameIsOn\", "+
    		"\"data\": { \"time\":1336219278079,            " +
    		"\"left\":{\"y\":186.0,\"playerName\":\"JohnMcEnroe\"}," +
    		"\"right\":{\"y\":310.0,\"playerName\":\"BorisBecker\"}," +
    		"\"ball\":{\"pos\":{\"x\":291.0,\"y\":82.0}}," +
    		"\"conf\":{\"maxWidth\":640,\"maxHeight\":480,\"paddleHeight\":50,\"paddleWidth\":10,\"ballRadius\":5,\"tickInterval\":15}}}\n"+
 			 
 			 
 			 "{\"msgType\":\"gameIsOver\",\"data\":\"JohnMcEnroe\"}\n"
        	 ;
        	 
        netInput = new ByteArrayInputStream(testdata.getBytes());
        
        
        out = System.out;
        */
        
        communicator = new PongGameCommunicator(out);

        // start server message listener
        listenerThread = new PongListenerThread(netInput, serverMessageQueue);
        listenerThread.start();

        // start game state loop
        pongBot = new TestBot(name,communicator, serverMessageQueue);
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
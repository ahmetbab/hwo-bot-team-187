package redlynx.pong.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;

import redlynx.pong.client.network.Communicator;
import redlynx.pong.client.network.NullCommunicator;
import redlynx.pong.client.network.PongGameCommunicator;
import redlynx.pong.client.network.PongListenerThread;
import redlynx.pong.client.ui.PongClientFrame;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.PongVisualizer;
import redlynx.pong.util.WinTimerHack;
import redlynx.test.TestBot;

public class Pong {
    private  Socket connection;
    private final InputStream netInput;

    
    private final PrintStream out;
    private final PongListenerThread listenerThread;
 
    private final Communicator communicator;
    private final Communicator devNull;
    private PongVisualizer visualizer;
    
    public Pong(String name, String host, int port, BaseBot bot, boolean visualize, boolean manual) throws IOException {
        connection = new Socket(host, port);
        netInput = connection.getInputStream();
        out = new PrintStream(connection.getOutputStream());
        communicator = new PongGameCommunicator(out);
        
        devNull = new NullCommunicator();
        
        // start game state loop
 
        bot.setName(name);
        bot.setCommunicator(manual?devNull:communicator);
        
        
        // start server message listener
        listenerThread = new PongListenerThread(netInput, bot);
        listenerThread.start();
        
        if (visualize) {
        	GameStateAccessorInterface accessor = bot.getGameStateAccessor(); //new GameStateAccessor(pongBot);
        	visualizer = new PongVisualizer(accessor);
        	new PongClientFrame(name, visualizer, accessor, manual?communicator:devNull);
        	bot.setVisualizer(visualizer);
        }

        
        System.out.println("Sending join");
        communicator.sendJoin(name);

        bot.start();
      
        // when game is over, shut down listener thread
        listenerThread.interrupt();
    }

    public static void init(String[] args, BaseBot bot) {

    	if (args.length < 3) {
    		System.err.println("Invalid arguments: USAGE: Pong name host port [-vis]");
    		System.exit(-1);
    	}
    	WinTimerHack.fixTimerAccuracy();
    	
    	String name = args[0];
    	String host = args[1];
    	String port = args[2];
    	boolean visualize = args.length == 4 && (args[3].equals("-vis") || args[3].equals("-manual"));
    	boolean manual = args.length == 4 && (args[3].equals("-manual"));
        System.out.println("name: " + name);
        System.out.println("host: " + host);
        System.out.println("port: " + port);

        try {
        	new Pong(name, host, Integer.parseInt(port), bot, visualize, manual);    	
        } catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static void main(String[] args) {
    	
    	init(args, new TestBot());
 	
    	
    }
    
}
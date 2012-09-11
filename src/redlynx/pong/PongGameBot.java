package redlynx.pong;

import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PongGameBot {

    private final Queue<String> serverMessageQueue;
    private final PongGameCommunicator communicator;

    String name;
    
    

    // TODO: Create base implementation. Abstraction should happen on a more detailed level.
    protected void onReceivedJSONString(String serverMessage) {
    	
    	//TODO capsulate json inside something
    	try {
			JSONObject json = new JSONObject(serverMessage);
			String type = json.getString("msgType");
			
			if ("gameIsOn".equals(type)) {
				System.out.println("GAME ON!!");
				JSONObject gameState = json.getJSONObject("data");
				GameStatus status = new GameStatus();
				
				status.time = gameState.getLong("time");
				
				JSONObject player1 = gameState.getJSONObject("left");
				JSONObject player2 = gameState.getJSONObject("right");
				status.left.y    = player1.getDouble("y");
				status.left.name = player1.getString("playerName");
				status.right.y    = player2.getDouble("y");
				status.right.name = player2.getString("playerName");
				
				
				JSONObject ball = gameState.getJSONObject("ball");
				JSONObject ballpos = ball.getJSONObject("pos");
				status.ballPos.x = ballpos.getDouble("x");
				status.ballPos.y = ballpos.getDouble("y");
				
				JSONObject conf = gameState.getJSONObject("conf");
				
				status.conf.maxWidth = conf.getInt("maxWidth");
				status.conf.maxHeight = conf.getInt("maxHeight");
				status.conf.paddleHeight = conf.getInt("paddleHeight");
				status.conf.paddleWidth = conf.getInt("paddleWidth");
				status.conf.ballRadius = conf.getInt("ballRadius");
				status.conf.tickInterval = conf.getInt("tickInterval");
				
				//TODO update bot runner calculations based on the status update
				System.out.println("status debug: "+status.toString());
			}
			else if ("gameJoined".equals(type)) {
				String host = json.getString("data");
				System.out.println("GAME joined!! visualization at: "+host);
			}
			else if ("gameStarted".equals(type)) {
				System.out.println("GAME Started!!");
				JSONArray players = json.getJSONArray("data");
				if (players.length() == 2)
					System.out.println("players "+players.getString(0) +" and "+players.getString(1));
			}
			else if ("gameIsOver".equals(type)) {
				String winner = json.getString("data");
				System.out.println("GAME OVER!! winner: "+winner);
			}
			else {
				//unexpected message
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    // TODO: Create basic upkeep of game state. Add an abstract tick call, in case bot wants
    // TODO: to add some actions to the basic upkeep phase.
    //protected abstract void onTick(long time);

    public PongGameBot(String name, PongGameCommunicator communicator, Queue<String> serverMessageQueue) {
    	this.name = name;
        this.serverMessageQueue = serverMessageQueue;
        this.communicator = communicator;
    }


    public void start() {

        this.communicator.sendJoin(name);

        while(true) {
        	while (!serverMessageQueue.isEmpty()) {
        		onReceivedJSONString(serverMessageQueue.remove());
        	}
            //onTick(System.currentTimeMillis());

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // if interrupted, exit program.
                break;
            }
        }
    }

}

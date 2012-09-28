package redlynx.pong.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class ServerPlayer implements Runnable{

	private final InputStreamReader input;
    private final PrintStream out;
    private final PongServer server;
	private final Socket connection;
	private int id;
	private boolean joined;
	private String name;
	private Deque<Long> messageLimiter;
	private int messageLimitPerSecond = 10;
	private static Random rand = new Random(); 
	private HashSet<Long> missiles;
	
	ServerPlayer(int id, Socket con, PongServer server) throws IOException {
		missiles = new HashSet<Long>();
		connection = con;
		joined = false;
		this.id = id;
		this.server = server;
	 	out = new PrintStream(connection.getOutputStream());
	 	input = new InputStreamReader(connection.getInputStream());
	 	messageLimiter = new ArrayDeque<Long>();

	}
	public String getName() {
		return name;
	}
	public boolean hasJoined() {
		return joined;
	}
	
	public void addMissile() {
		long missileId = rand.nextLong()%100000000000L;
		while (missiles.contains(missileId)) {
			missileId = rand.nextLong() %100000000000L;
		}
		missiles.add(missileId);
		sendMissileReadyMessage(missileId);
	}
	
	private void sendMissileReadyMessage(long missileId) {
		try {
			JSONObject json = new JSONObject();
			json.put("msgType", "missileReady");
			json.put("data", missileId);
			sendMessageLagSimulated(json.toString());
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean launchMissile(long missileId) {
		return missiles.remove(missileId);
	}
	
	public void sendMessage(String formattedMessage) {
		out.println(formattedMessage);
		out.flush();
	}
	private void sendMessageLagSimulated(String formattedMessage) {
		server.getLagSimulator().send(this, formattedMessage);
	}
	
	public void sendStartedMessage(String other) {
		
		try {
			JSONObject json = new JSONObject();
			json.put("msgType", "gameStarted");
			JSONArray array = new JSONArray();
			array.put(name);
			array.put(other);
			json.put("data", array);
			sendMessageLagSimulated(json.toString());
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendJoinedMessage() {
		try {
			JSONObject json = new JSONObject();
			json.put("msgType", "joined");
			json.put("data", "http://localhost/test.html");
			sendMessageLagSimulated(json.toString());
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	public void sendEndMessage(String name) {
		try {
			JSONObject json = new JSONObject();
			json.put("msgType", "gameIsOver");
			json.put("data", name);
			sendMessageLagSimulated(json.toString());
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendGameState(GameState gameState) {
		sendMessageLagSimulated(gameState.toJSONString(id));
	}
	
	private void messageReceivedLagSimulated(String msg) {
		long time = System.nanoTime();
		messageLimiter.add(time);
		while (messageLimiter.size() >= messageLimitPerSecond) {
			System.out.println("Messages per second ("+name+") "+(10*1000000000.0/((time - messageLimiter.peekFirst()))));
			if (time - messageLimiter.removeFirst() < 1000000000) {
				System.out.println("Too many Messages for player: "+name);
			} 
		}
		server.getLagSimulator().receive(this, msg);
	}
	
	public void messageReceived(String msg) {
		//System.out.println("r"+id+":"+msg);
		try {	
			JSONObject json = new JSONObject(msg);
			String type = json.getString("msgType");
			
			if ("join".equals(type)) {
				joined = true;
				name = json.getString("data");
				server.updatePlayer(id, name);
				sendJoinedMessage();
			}
			if (joined) {
				if ("changeDir".equals(type)) {
					server.changeDir(id, json.getDouble("data"));
				}
				else if ("launchMissile".equals(type)) {
					server.launchMissile(id, json.getLong("data"));
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
        StringBuilder msg = new StringBuilder();
        int depth = 0;
        while(true) {
            try {
            	
            	char c = (char) input.read();
            	if (c == 65535) {//end of stream
            		break;
            	}
            	msg.append(c);
            	switch(c) {
            	case '{':
            		depth++;
            		break;
            	case '}':
            		depth--;
            		if (depth == 0) {
            			
            			messageReceivedLagSimulated(msg.toString());
            			msg = new StringBuilder();
            			
            		}
            	}
                
            } catch (IOException e) {
                // if io exception, quit?
                break;
            }
        }
        //connection closed, remove player from game
        server.kickPlayer(id);
	}
	public void disconnect() {
		try {
			connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

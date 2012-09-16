package redlynx.pong.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

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
	ServerPlayer(int id, Socket con, PongServer server) throws IOException {
		connection = con;
		joined = false;
		this.id = id;
		this.server = server;
	 	out = new PrintStream(connection.getOutputStream());
	 	input = new InputStreamReader(connection.getInputStream());

	}
	public String getName() {
		return name;
	}
	public boolean hasJoined() {
		return joined;
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
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
        String msg = "";
        int depth = 0;
        while(true) {
            try {
            	
            	char c = (char) input.read();
            	if (c == 65535) {//end of stream
            		break;
            	}
            	msg += c;
            	switch(c) {
            	case '{':
            		depth++;
            		break;
            	case '}':
            		depth--;
            		if (depth == 0) {
            			
            			messageReceivedLagSimulated(msg);
            			msg = "";
            			
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

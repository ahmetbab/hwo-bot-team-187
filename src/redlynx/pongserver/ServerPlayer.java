package redlynx.pongserver;

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
	Socket connection;
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
	public void sendStartedMessage(String other) {
		
		try {
			JSONObject json = new JSONObject();
			json.put("msgType", "gameStarted");
			JSONArray array = new JSONArray();
			array.put(name);
			array.put(other);
			json.put("data", array);
			out.println(json.toString());
			out.flush();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendJoinedMessage() {
		try {
			JSONObject json = new JSONObject();
			json.put("msgType", "joined");
			json.put("data", "http://localhost/test.html");
			out.println(json.toString());
			out.flush();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	public void sendEndMessage(String name) {
		try {
			JSONObject json = new JSONObject();
			json.put("msgType", "gameIsOver");
			json.put("data", name);
			out.println(json.toString());
			out.flush();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendGameState(GameState gameState) {
		out.println(gameState.toJSONString(id));
		out.flush();
	}
	
	private void messageReceived(String msg) throws JSONException {
		//System.out.println("r"+id+":"+msg);
		
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
            			try {
            				messageReceived(msg);
            			}
            			finally {
            				msg = "";
            			}
            		}
            	}
                
            } catch (IOException e) {
                // if io exception, quit?
                break;
            } catch (JSONException e) {
				e.printStackTrace();
			}
        }
	}
	public void disconnect() {
		try {
			connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

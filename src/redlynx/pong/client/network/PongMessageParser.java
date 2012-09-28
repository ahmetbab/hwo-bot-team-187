package redlynx.pong.client.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import redlynx.pong.client.state.GameStatusSnapShot;
import redlynx.pong.client.state.MissileState;
import redlynx.pong.util.Vector2;

public class PongMessageParser {

	public interface ParsedMessageListener {
		public void gameStart(String player1, String player2);
		public void gameOver(String winner);
		public void gameStateUpdate(GameStatusSnapShot status);
		public void missileReady(long missileId);
		public void missileLaunched(MissileState missile);
	} 
	
    //private final PongGameBot bot;
	private ParsedMessageListener listener;

    public PongMessageParser(ParsedMessageListener listener) {
        this.listener = listener;
    }

    private void onGameStart(JSONArray players) throws JSONException {
        if (players.length() == 2) {
            	listener.gameStart(players.getString(0), players.getString(1));
        }
    }

    private void onGameOver(String winner) {
    	listener.gameOver(winner);
     }

    private void onGameUpdate(JSONObject gameState) {

        GameStatusSnapShot status = new GameStatusSnapShot();
        try {
            status.time = gameState.getLong("time");

            JSONObject player1 = gameState.getJSONObject("left");
            JSONObject player2 = gameState.getJSONObject("right");
            status.left.y    = player1.getDouble("y");
            status.left.name = player1.getString("playerName");
            status.right.y    = player2.getDouble("y");
            status.right.name = player2.getString("playerName");


            JSONObject ball = gameState.getJSONObject("ball");
            JSONObject ballpos = ball.getJSONObject("pos");
            status.ball.x = ballpos.getDouble("x");
            status.ball.y = ballpos.getDouble("y");

            JSONObject conf = gameState.getJSONObject("conf");

            status.conf.screenArea.x = conf.getInt("maxWidth");
            status.conf.screenArea.y = conf.getInt("maxHeight");
            status.conf.paddleDimension.y = conf.getInt("paddleHeight");
            status.conf.paddleDimension.x = conf.getInt("paddleWidth");
            status.conf.ballRadius = conf.getInt("ballRadius");
            status.conf.tickInterval = conf.getInt("tickInterval");

            listener.gameStateUpdate(status);
        }
        catch (JSONException e) {
            // ignore bad data.
        }
    }
    
    private void onMissileReady(long missileId) {
    	listener.missileReady(missileId);
    }
    private void onMissileLaunched(JSONObject missileState) {

        
        try {
            
        	Vector2 pos = new Vector2();
        	Vector2 vel = new Vector2();
        	
        	

            JSONObject jpos = missileState.getJSONObject("pos");
            pos.x = jpos.getDouble("x");
            pos.y = jpos.getDouble("y");

            jpos = missileState.getJSONObject("speed");
            vel.x = jpos.getDouble("x");
            vel.y = jpos.getDouble("y");
            long time = missileState.getLong("launchTime");
            String code = missileState.getString("code");
            
            MissileState status = new MissileState(pos, vel, time, code);
            listener.missileLaunched(status);
        }
        catch (JSONException e) {
            // ignore bad data.
        }
    }
    
    

    public void onReceivedJSONString(String serverMessage) {
        try {
            JSONObject json = new JSONObject(serverMessage);
            String type = json.getString("msgType");

            if ("gameIsOn".equals(type)) {
                onGameUpdate(json.getJSONObject("data"));
            }
            else if ("joined".equals(type)) {
                String host = json.getString("data");
                System.out.println("GAME joined!! visualization at: " + host);
            }
            else if ("gameStarted".equals(type)) {
                onGameStart(json.getJSONArray("data"));
            }
            else if ("gameIsOver".equals(type)) {
                onGameOver(json.getString("data"));
            }
            else if ("missileReady".equals(type)) {
            	onMissileReady(json.getLong("data"));
            }
            else if ("missileLaunched".equals(type)) {
            	onMissileLaunched(json.getJSONObject("data"));
            }

            else {
                // unexpected message
                System.out.println("Unexpected message: " + serverMessage);
            }
        } catch (JSONException e) {
            System.out.println("Not a JSON message: " + serverMessage);
        }
    }
}

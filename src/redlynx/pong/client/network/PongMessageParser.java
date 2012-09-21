package redlynx.pong.client.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import redlynx.pong.client.state.GameStatusSnapShot;

public class PongMessageParser {

	public interface ParsedMessageListener {
		public void gameStart(String player1, String player2);
		public void gameOver(String winner);
		public void gameStateUpdate(GameStatusSnapShot status);
	} 
	
    //private final PongGameBot bot;
	private ParsedMessageListener listener;

    public PongMessageParser(ParsedMessageListener listener) {
        this.listener = listener;
    }

    private void onGameStart(JSONArray players) throws JSONException {
    	
        if (players.length() == 2) {
            	// System.out.println("Game Start : "+players.getString(0)+" : "+players.getString(1));
            	listener.gameStart(players.getString(0), players.getString(1));
        }
        else throw new JSONException("Wrong number of players");
    }

    private void onGameOver(String winner) {
    	// System.out.println("Game Over");
    	listener.gameOver(winner);
     }

    private void onGameUpdate(JSONObject gameState) throws JSONException {

        GameStatusSnapShot status = new GameStatusSnapShot();
    
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

        //System.out.println("y: "+status.left.y+" scr "+status.conf.screenArea.x+","+status.conf.screenArea.y+" paddleSize "+status.conf.paddleDimension.y);
        listener.gameStateUpdate(status);
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
            else {
                // unexpected message
                System.out.println("Unexpected message: " + serverMessage);
            }
        } catch (JSONException e) {
            System.out.println("Not a JSON message: " + serverMessage);
        }
    }
}

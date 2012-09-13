package redlynx.pong.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import redlynx.pong.state.GameStatus;
import redlynx.pong.state.PongGameBot;

public class PongMessageParser {

    private final PongGameBot bot;

    public PongMessageParser(PongGameBot bot) {
        this.bot = bot;
    }

    public void onGameStart(JSONArray players) {
    	
        if (players.length() == 2) {
        	
            try {
            	System.out.println("Game Start : "+players.getString(0)+" : "+players.getString(1));
                if(bot.getName().equals(players.getString(0))) {
                    bot.setMySide(PongGameBot.PlayerSide.LEFT);
                }
                else {
                    bot.setMySide(PongGameBot.PlayerSide.RIGHT);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void onGameOver(String winner) {
    	System.out.println("Game Over");
        bot.onGameOver(winner.equals(bot.getName()));
    }

    public void onGameUpdate(JSONObject gameState) {

        GameStatus status = new GameStatus();

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

            status.conf.maxWidth = conf.getInt("maxWidth");
            status.conf.maxHeight = conf.getInt("maxHeight");
            status.conf.paddleHeight = conf.getInt("paddleHeight");
            status.conf.paddleWidth = conf.getInt("paddleWidth");
            status.conf.ballRadius = conf.getInt("ballRadius");
            status.conf.tickInterval = conf.getInt("tickInterval");

            
            //System.out.println("y: "+status.left.y+" scr "+status.conf.maxWidth+","+status.conf.maxHeight+" paddleSize "+status.conf.paddleHeight);
            
            bot.gameStateUpdate(status);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        //TODO update bot runner calculations based on the status update
        //System.out.println("status debug: "+status.toString());
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

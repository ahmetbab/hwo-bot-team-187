package redlynx.pong;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PongMessageParser {

    private final PongGameBot bot;

    public PongMessageParser(PongGameBot bot) {
        this.bot = bot;
    }

    public void onGameStart(JSONArray players) {
        System.out.println("GAME Started!!");
        if (players.length() == 2) {
            try {
                System.out.println("players " + players.getString(0) +" and " + players.getString(1));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void onGameOver(String winner) {
        bot.onGameOver(winner.equals(bot.getName()));
    }

    public void onGameUpdate(JSONObject gameState) {

        System.out.println("GAME ON!!");
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

            bot.gameStateUpdate(status);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        //TODO update bot runner calculations based on the status update
        System.out.println("status debug: "+status.toString());
    }

    public void onReceivedJSONString(String serverMessage) {
        try {
            JSONObject json = new JSONObject(serverMessage);
            String type = json.getString("msgType");

            if ("gameIsOn".equals(type)) {
                onGameUpdate(json.getJSONObject("data"));
            }
            else if ("gameJoined".equals(type)) {
                String host = json.getString("data");
                System.out.println("GAME joined!! visualization at: "+host);
            }
            else if ("gameStarted".equals(type)) {
                onGameStart(json.getJSONArray("data"));
            }
            else if ("gameIsOver".equals(type)) {
                onGameOver(json.getString("data"));
            }
            else {
                // unexpected message
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

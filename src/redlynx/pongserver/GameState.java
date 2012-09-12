package redlynx.pongserver;

import org.json.JSONObject;

public class GameState {
	public class Paddle {
		double y;
		double vel;
		String name;
	}
	public class BallConfig {
		double speed; //?
		int radius;
		BallConfig() {
			radius = 5;
			speed = 1;
		}
	}
	public class Ball {
		Ball() {
			conf = new BallConfig();
		}
		double x;
		double y;
		double dx;
		double dy;
		BallConfig conf;
	}
	
	public class PaddleConfig {
		PaddleConfig() {
			maxSpeed = 1;
			width = 10;
			height = 50;
		}
		double maxSpeed;
		int width;
		int height;
	}
	
	public Paddle [] paddle = new Paddle[2];
	public Ball ball = new Ball();
	public PaddleConfig paddleConfig = new PaddleConfig();
	
	int screenWidth;
	int screenHeight;
	int tickInterval;
	
	private boolean gameEnded;
	private int winner;
	public boolean hasEnded() {
		return gameEnded;
	}
	public int getWinner() {
		return winner;
	}
	public void setPlayers(String player1, String player2) {
		paddle[0].name = player1;
		paddle[1].name = player2;
	}
	
	GameState() {
		for (int i = 0; i < 2; i++) {
			paddle[i] = new Paddle();
		}
		screenWidth = 640;
		screenHeight = 480;
		tickInterval = 15;
		
	}
	
	public void resetGame() {
		paddle[0].y = paddle[1].y = (screenHeight / 2); //TODO parameter to randomize?
		paddle[0].vel = paddle[1].vel = 0;
		ball.x = 10;
		ball.y = 10;
		ball.dx = 1;
		ball.dy = 1;	
		gameEnded = false;
		winner = -1;
	}
	
	public synchronized void tickGame() {
		for (int i = 0; i < paddle.length; i++) {
			paddle[i].y += paddle[i].vel*paddleConfig.maxSpeed; //should this be done every tick or timed 
			
			if (paddle[i].y < 0) {
				//paddle bounce
				paddle[i].y = -paddle[i].y;
				paddle[i].vel = -paddle[i].vel; 
			}
			else if (paddle[i].y + paddleConfig.height > screenHeight) {
				//paddle bounce
				paddle[i].y = (screenHeight-paddleConfig.height) - (paddle[i].y + paddleConfig.height - screenHeight);
				paddle[i].vel = -paddle[i].vel; 
			}
			
		}
		System.out.println("paddles: "+paddle[0].y+" : "+paddle[1].y);
		
		ball.x += ball.dx*ball.conf.speed;
		ball.y += ball.dy*ball.conf.speed;
		
		if (ball.y-ball.conf.radius < 0) { 
			ball.y = ball.conf.radius-(ball.y-ball.conf.radius);
			ball.dy = -ball.dy;
		}
		else if (ball.y+ball.conf.radius > screenHeight) {
			ball.y = screenHeight-ball.conf.radius - (ball.y+ball.conf.radius - screenHeight);
			ball.dy = -ball.dy;
		}
		
		if (ball.x - ball.conf.radius <= paddleConfig.width) {
			
			//TODO more accurate collision check?
			if (ball.y+ball.conf.radius >= paddle[0].y 
				&& ball.y-ball.conf.radius <= paddle[0].y+paddleConfig.height) {
				//bounce on left
				
				ball.x = paddleConfig.width + ball.conf.radius - ((ball.x - ball.conf.radius) - paddleConfig.width);
				
				ball.dx = -ball.dx; //TODO deflect
			}
		}
		else {
			if (ball.x + ball.conf.radius >= screenWidth-paddleConfig.width) {
				
				//TODO more accurate collision check?
				if (ball.y+ball.conf.radius >= paddle[1].y 
					&& ball.y-ball.conf.radius <= paddle[1].y+paddleConfig.height) {

					//bounce on right
					ball.x = (screenWidth-paddleConfig.width)-ball.conf.radius 
					-((ball.x + ball.conf.radius) - (screenWidth-paddleConfig.width));
										
					ball.dx = -ball.dx; //TODO deflect
				}
			}
		}
		if (ball.x - ball.conf.radius < 0) {
			gameEnded = true;
			winner = 1;
		}
		else if (ball.x + ball.conf.radius > screenWidth) {
			gameEnded = true;
			winner = 0;
		}
		
	}
	public long getTickInterval() {
		
		return tickInterval;
	}
	public synchronized void changeDir(int id, double dir) {

		if (dir < -1)
			dir = -1;
		else if (dir > 1)
			dir = 1;
		paddle[id].vel = dir;
		
	}
	
	public synchronized String toJSONString(int id) {
		
		//TODO flip board for player 1
		try {
			JSONObject stateMessage  = new JSONObject();
			stateMessage.put("msgType", "gameIsOn");
			JSONObject data = new JSONObject();
			data.put("time", System.currentTimeMillis());
			JSONObject player1 = new JSONObject();
			player1.put("y", paddle[id].y);
			player1.put("playerName", paddle[id].name);
			JSONObject player2 = new JSONObject();
			player2.put("y", paddle[-(id-1)].y);
			player2.put("playerName", paddle[-(id-1)].name);
			data.put("left", player1);
			data.put("right", player2);
			JSONObject ball = new JSONObject();
			JSONObject ballPos = new JSONObject();
			ballPos.put("x", id == 1 ? screenWidth-this.ball.x: this.ball.x);
			ballPos.put("y", this.ball.y);
			ball.put("pos", ballPos);
			data.put("ball", ball);
			JSONObject conf = new JSONObject();
			conf.put("maxWidth", screenWidth);
			conf.put("maxHeight", screenHeight);
			conf.put("paddleHeight", paddleConfig.height);
			conf.put("paddleWidth", paddleConfig.width);
			conf.put("ballRadius",  this.ball.conf.radius);
			conf.put("tickInterval", tickInterval);
			
			data.put("conf", conf);
			stateMessage.put("data", data);
			return stateMessage.toString();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}
}

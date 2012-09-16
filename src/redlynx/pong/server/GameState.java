package redlynx.pong.server;

import java.awt.Color;
import java.util.ArrayList;

import org.json.JSONObject;

import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.UILine;
import redlynx.pong.ui.UIString;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

public class GameState implements GameStateAccessorInterface {
	
	
	
	public class Paddle {
		public double y;
		public double vel;
		public String name;
	}
	public class BallConfig {
		public double speed; //?
		public int radius;
		BallConfig() {
			radius = 5;
			speed = 5;
		}
	}
	public class Ball {
		Ball() {
			conf = new BallConfig();
		}
		public double x;
		public double y;
		public double dx;
		public double dy;
		public BallConfig conf;
	}
	
	public class PaddleConfig {
		PaddleConfig() {
			maxSpeed = 1;
			width = 10;
			height = 50;
		}
		public double maxSpeed;
		public int width;
		public int height;
	}
	
	public Paddle [] paddle = new Paddle[2];
	public Ball ball = new Ball();
	public PaddleConfig paddleConfig = new PaddleConfig();
	
	public int screenWidth;
	public int screenHeight;
	public int tickInterval;
	
	private boolean gameEnded;
	private int winner;
	public int deflectionMode;
	public int[] deflectionValue = new int[2];
	
	
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
		ball.x = screenWidth /2;
		ball.y = screenHeight / 2;
		paddle[0].y = paddle[1].y = (screenHeight / 2);
		gameEnded = false;
		winner = -1;
		deflectionMode = 0;
		deflectionValue[0] = 10;
		deflectionValue[1] = 20;
		
	}
	
	public void resetGame() {
		paddle[0].y = paddle[1].y = (screenHeight / 2); //TODO parameter to randomize?
		paddle[0].vel = paddle[1].vel = 0;
		ball.x = 10;
		ball.y = 10 + (Math.random() * screenHeight - 20);
		ball.dx = Math.random() + 0.5;
		ball.dy = Math.random() + 0.5;
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
		//System.out.println("paddles: "+paddle[0].y+" : "+paddle[1].y);
		
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
				double dy = ball.y - paddle[0].y - (paddleConfig.height * 0.5);
                dy /= paddleConfig.height * 0.5;

                ball.x = paddleConfig.width + ball.conf.radius - ((ball.x - ball.conf.radius) - paddleConfig.width);
				ball.dx = -ball.dx; //TODO deflect

                ball.dy += dy * (deflectionValue[0]/100.0f);
                ball.dy += (deflectionValue[1]/20.0f)*(Math.random() - 0.5); // testing
			}
		}
		else {
			if (ball.x + ball.conf.radius >= screenWidth-paddleConfig.width) {
				
				//TODO more accurate collision check?
				if (ball.y+ball.conf.radius >= paddle[1].y 
					&& ball.y-ball.conf.radius <= paddle[1].y+paddleConfig.height) {

                    double dy = ball.y - paddle[1].y - (paddleConfig.height * 0.5);
                    dy /= paddleConfig.height * 0.5;

					//bounce on right
					ball.x = (screenWidth-paddleConfig.width)-ball.conf.radius 
					-((ball.x + ball.conf.radius) - (screenWidth-paddleConfig.width));
										
					ball.dx = -ball.dx; //TODO deflect
                    ball.dy += dy * (deflectionValue[0]/100.0f);
                    ball.dy += (deflectionValue[1]/20.0f)*(Math.random() - 0.5); // testing
				}
			}
		}
		if (ball.x - ball.conf.radius < 0) {
			endGame(1);
		}
		else if (ball.x + ball.conf.radius > screenWidth) {
			endGame(0);
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
		
		try {
			
			//System.out.println(this.toString());
			
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
	public String toString() {
		String str = "time"+ System.currentTimeMillis();
		str += "\nplayerName "+  paddle[0].name+" y "+paddle[0].y;
		str += "\nplayerName "+  paddle[1].name+" y "+paddle[1].y;
		str += "\nball ("+ball.x+", "+ball.y+")";
		str += "\nmaxWidth"+ screenWidth;
		str += "\nmaxHeight"+ screenHeight;
		str += "\npaddleHeight"+ paddleConfig.height;
		str += "\npaddleWidth"+ paddleConfig.width;
		str += "\nballRadius" +this.ball.conf.radius;
		str += "\ntickInterval"+ tickInterval;
		return str;
	}
	
	public synchronized void setBallSpeed(double speed) {
		ball.conf.speed = speed;	
	}
	public synchronized void setBallRadius(int radius) {
		ball.conf.radius = radius;	
	}
	public synchronized void setPaddleHeight(int height) {
		paddleConfig.height = height;	
	}
	public synchronized void setPaddleWidth(int width) {
		paddleConfig.width = width;	
	}
	public synchronized void setPaddleSpeed(double speed) {
		paddleConfig.maxSpeed = speed;	
	}
	
	public synchronized void setScreenSize(int width, int height) {
		screenHeight = height;
		screenWidth = width;
	}
	public void endGame(int winner) {
		gameEnded = true;
		this.winner = winner;
		
	}
	public synchronized void setTickInterval(int value) {
		tickInterval = value;
		
	}
	public void setDeflectionMode(int value) {
		deflectionMode = value;
		
	}
	public void setDeflectionValue(int id, int value) {
		deflectionValue[id] = value;
	}
	
	
	//accessor
	
	@Override
	public int getNumberOfStatesToRender() {return 1;}
	@Override
	public Color getRenderColor(int stateIdx) {return Color.white;};
	@Override
	public void setRenderState(int stateIdx) {}
	
	@Override
	public double getPedalY(int id) {
		return paddle[id].y;
	}
	@Override
	public String getPlayerName(int id) {
		return paddle[id].name;
	}
	@Override
	public int getBallRadius() {
		return ball.conf.radius;
	}
	@Override
	public Vector2 getBallPos() {
		return new Vector2(ball.x, ball.y);
	}
	@Override
	public Vector2i getAreaDimensions() {
		return new Vector2i(screenWidth, screenHeight);
	}
	@Override
	public Vector2i getPedalDimensions() {
		return new Vector2i(paddleConfig.width, paddleConfig.height);
	}
	@Override
	public ArrayList<UILine> getExtraLines() {
		return null;
	}
	@Override
	public ArrayList<UIString> getExtraStrings() {
		return null;
	}
	
	
	
	
	
	
}

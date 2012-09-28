package redlynx.pong.server;

import java.awt.Color;
import java.util.ArrayList;

import org.json.JSONObject;

import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.collisionmodel.SFSauronGeneralModel;
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
		public boolean newMissile;
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
			maxSpeed = 3;
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
	public int missileStartPos;
	public int missileSpeed;
	
	
	private boolean gameEnded;
	private int winner;
	public int deflectionMode;
	public int[] deflectionValue = new int[2];
	PongModel model;
	
	private class Missile {
		Vector2 pos;
		Vector2 vel;
		boolean newMissile;
		public Missile(Vector2 pos, Vector2 vel) {
			this.pos = pos;
			this.vel = vel;
			newMissile = true;
		}
		boolean isNew() {
			return newMissile;
		}
		
		
		public String toJSONString(int playerId) {
			try {
				
				//System.out.println(this.toString());
				
				JSONObject stateMessage  = new JSONObject();
				stateMessage.put("msgType", "missileLaunched");
				JSONObject data = new JSONObject();
				data.put("launchTime", System.currentTimeMillis());
				JSONObject jpos = new JSONObject();
				jpos.put("x", playerId==0?pos.x: screenWidth-pos.x);
				jpos.put("y", pos.y);
				data.put("pos", jpos);
				JSONObject jvel = new JSONObject();
				jvel.put("x", playerId==0?vel.x: screenWidth-vel.x);
				jvel.put("y", vel.y);
				data.put("speed", jvel);
				data.put("code", "destroyer");
				stateMessage.put("data", data);
				return stateMessage.toString();
			}catch(Exception e) {
				e.printStackTrace();
			}
			return "";
		}
	}
	ArrayList<Missile> missiles;
	
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
	
	public void clearMissiles() {
		missiles.clear();
	}
	
	GameState() {
		missiles = new ArrayList<Missile>();
		for (int i = 0; i < 2; i++) {
			paddle[i] = new Paddle();
		}
		screenWidth = 640;
		screenHeight = 480;
		tickInterval = 30;
		ball.x = screenWidth /2;
		ball.y = screenHeight / 2;
		paddle[0].y = paddle[1].y = (screenHeight / 2);
		gameEnded = false;
		winner = -1;
		deflectionMode = 0;
		deflectionValue[0] = 10;
		deflectionValue[1] = 20;
		model = new SFSauronGeneralModel();
		
		missileStartPos = screenWidth/2;
		missileSpeed = 5;
		
	}
	
	public void resetGame() {
		missiles.clear();
		paddle[0].y = paddle[1].y = (screenHeight / 2); //TODO parameter to randomize?
		paddle[0].vel = paddle[1].vel = 0;
		paddle[0].newMissile = paddle[1].newMissile = false;
		ball.x = 10;
		ball.y = 10 + (Math.random() * (screenHeight - 20));
		ball.dx = Math.random() + 0.5;
		ball.dy = 0.5*Math.random() + 0.5;
		gameEnded = false;
		winner = -1;
	}
	
	public synchronized void tickGame() {
		
		
		
		for (int i = 0; i < missiles.size(); i++) {
			Missile m = missiles.get(i); 
			m.newMissile = false;
			m.pos.x += m.vel.x;
			m.pos.y += m.vel.y;
		}
		
		
		for (int i = 0; i < paddle.length; i++) {
			if (paddle[i].newMissile) {
				missiles.add(new Missile(
						new Vector2(i == 0 ? missileStartPos : screenWidth-missileStartPos, paddle[i].y+paddleConfig.height/2), 
						new Vector2(i == 0 ? missileSpeed    : -missileSpeed, 0)));
				paddle[i].newMissile = false;
			}
			
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
		
		for (int i = missiles.size()-1; i >= 0; i--) {
			Missile m = missiles.get(i);
			if (m.pos.x < paddleConfig.width) {
				if (m.pos.y >= paddle[0].y 
					&& m.pos.y <= paddle[0].y+paddleConfig.height) {
					endGame(1);
				}
			
				if (m.pos.x < 0)
					missiles.remove(i);
			}
			else if (m.pos.x > screenWidth-paddleConfig.width) {
				if (m.pos.y >= paddle[1].y 
					&& m.pos.y <= paddle[1].y+paddleConfig.height) {
					endGame(0);
				}
				if (m.pos.x > screenWidth)
					missiles.remove(i);
			}
			
			
		}
		if (gameEnded) //missile killed a player already
			return;
		
		//System.out.println("paddles: "+paddle[0].y+" : "+paddle[1].y);
		
		Vector2 oldPos = new Vector2(ball.x, ball.y);
		Vector2 ballVel = new Vector2(ball.dx*ball.conf.speed, ball.dy*ball.conf.speed);
		
		
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
		
		if (ballVel.x < 0) {
			if (ball.x - ball.conf.radius <= paddleConfig.width) {
				double time = (oldPos.x-(paddleConfig.width+ball.conf.radius))/ballVel.x;
				double collisionY = oldPos.y+time*ballVel.y;
				
				
				
				if (collisionY+ball.conf.radius < paddle[0].y || collisionY-ball.conf.radius > paddle[0].y+paddleConfig.height) {
					endGame(1);
				}
				else {
					
					double paddleCollision =(collisionY-(paddle[0].y+paddleConfig.height/2))/(paddleConfig.height/2.0);
					
					
				
					
					Vector2 deflected = model.guess(paddleCollision, ballVel.x, ballVel.y);
					
					double ballspeed = ballVel.length();
					ballVel.x = deflected.x;
					ballVel.y = deflected.y;
					ballVel.normalize();
					
					Vector2 col = new Vector2((paddleConfig.width+ball.conf.radius), collisionY);
					Vector2 dist = oldPos.minus(col);
					double speedLeft = ballspeed - dist.length();
					ball.x = col.x+speedLeft*ballVel.x;
					ball.y = col.y+speedLeft*ballVel.y;
					ball.dx = ballVel.x*ballspeed*1.05/ball.conf.speed;
					ball.dy = ballVel.y*ballspeed*1.05/ball.conf.speed;
				}
			}
			
		} 
		else {
			if (ball.x + ball.conf.radius >= screenWidth-paddleConfig.width) {
				double time = (screenWidth-paddleConfig.width - ball.conf.radius- oldPos.x)/ballVel.x;
				double collisionY = oldPos.y+time*ballVel.y;
				
				
				
				if (collisionY+ball.conf.radius < paddle[1].y || collisionY-ball.conf.radius > paddle[1].y+paddleConfig.height) {
					endGame(0);
				}
				else {
					
					double paddleCollision =(collisionY-(paddle[1].y+paddleConfig.height/2))/(paddleConfig.height/2.0);
					
					Vector2 deflected = model.guess(paddleCollision, ballVel.x, ballVel.y);
					
					double ballspeed = ballVel.length();
					ballVel.x = deflected.x;
					ballVel.y = deflected.y;
					ballVel.normalize();
					
					Vector2 col = new Vector2((screenWidth-paddleConfig.width-ball.conf.radius), collisionY);
					Vector2 dist = oldPos.minus(col);
					double speedLeft = ballspeed - dist.length();
					ball.x = col.x+speedLeft*ballVel.x;
					ball.y = col.y+speedLeft*ballVel.y;
					ball.dx = ballVel.x*ballspeed*1.05/ball.conf.speed;
					ball.dy = ballVel.y*ballspeed*1.05/ball.conf.speed;
				}
			}
		}
		
		/*
		if (ball.x - ball.conf.radius <= paddleConfig.width) {
			
			
			//TODO more accurate collision check?
			if (ball.y+ball.conf.radius >= paddle[0].y 
				&& ball.y-ball.conf.radius <= paddle[0].y+paddleConfig.height) {
				
				//bounce on left
				double dy = ball.y - paddle[0].y - (paddleConfig.height * 0.5);
                dy /= paddleConfig.height * 0.5;

                ball.x = paddleConfig.width + ball.conf.radius - ((ball.x - ball.conf.radius) - paddleConfig.width);
				ball.dx = -ball.dx*1.01; //TODO deflect

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
										
					ball.dx = -ball.dx*1.01; //TODO deflect
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
		*/
		
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
	
	public synchronized void launchMissile(int id) {
		paddle[id].newMissile = true;
	}
	 
	
	
	public synchronized String toJSONString(int id) {
		
		try {
			
			//System.out.println(this.toString());
			
			String missilesString = "";
			for (int i = 0; i < missiles.size(); i++) {
				Missile m = missiles.get(i);
				if (m.isNew())
					missilesString += m.toJSONString(id);
			}
			
			
			
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
			return stateMessage.toString()+missilesString;
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
	public ArrayList<Vector2> getMissilePositions() {
		ArrayList<Vector2> mpos = new ArrayList<Vector2>();
		for (int i = 0; i < missiles.size(); i++) {
			mpos.add(missiles.get(i).pos);
		}
		return mpos;
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

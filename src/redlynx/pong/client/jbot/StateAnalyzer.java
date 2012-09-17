package redlynx.pong.client.jbot;

import redlynx.pong.client.state.GameStatusSnapShot;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

public class StateAnalyzer {

	
	
	HistoryBuffer history;
	
	Vector2 ballPos;
	
	Vector2 ballVel;
	Vector2 nextOpponentCollision;
	double opponentCollisionTime; 
	Vector2 nextHomeCollision;
	double homeCollisionTime;
	double ballSpeed = 0;
	
	int radius;
	public StateAnalyzer() {
		history = new HistoryBuffer();
		ballVel = new Vector2();
		ballPos = new Vector2();
		nextHomeCollision = new Vector2();
		nextOpponentCollision = new Vector2();
		radius = 0;
	}
	void resetMatch() {
		history.reset();
		ballVel.x = 0;
		ballVel.y = 0;
		ballPos.x = 0;
		ballPos.y = 0;
		
	}
	
	private Vector2 flip(Vector2 p, int dir, int radius, Vector2i screenDimensions) {
		switch(dir) {
		case 0: //up
			return new Vector2(p.x, radius-(p.y-radius));
		case 1: //down
			return new Vector2(p.x, screenDimensions.y-radius+((screenDimensions.y-radius) - p.y));
		case 2: //right
		case 3: //left
		}
		return p;
	}
	
	private void calculateBallVelocity() {
		GameStatusSnapShot status =history.getStatus(0); 
		ballPos = status.ball;
		int radius = status.conf.ballRadius;
		
		if (history.getHistorySize() >= 3) {
			Vector2 prevVel = ballVel;
			
			Vector2 p1 = history.getStatus(0).ball;
			Vector2 p2 = history.getStatus(1).ball;
			Vector2 p3 = history.getStatus(2).ball;
			double timeDiff = (history.getStatus(0).time -history.getStatus(2).time)/1000.0; 
			if (timeDiff == 0)
				timeDiff = 0.001;
			double err = PongUtil.pointDistance2Line(p1, p2, p3);
			if (err < 2) {
				ballVel = p1.minus(p3);
				ballVel.x /= timeDiff;
				ballVel.y /= timeDiff;
				ballSpeed = (3*ballSpeed+ballVel.length())/4;
				ballVel.setLength(ballSpeed);
			}
			else {
				for (int i = 0 ; i < 2; i++) {
					Vector2 p3f = flip(p3, i, radius, status.conf.screenArea);
					err = PongUtil.pointDistance2Line(p1, p2, p3f);
					System.out.println("err " +err);
					if (err < 2) {
						ballVel = p1.minus(p3f);
						ballVel.x /= timeDiff;
						ballVel.y /= timeDiff;
						ballSpeed = (3*ballSpeed+ballVel.length())/4;
						ballVel.setLength(ballSpeed);
						return;
					}
					Vector2 p2f = flip(p2, i, radius, status.conf.screenArea);
					err = PongUtil.pointDistance2Line(p1, p2f, p3f);
					if (err < 2) {
						ballVel = p1.minus(p3f);
						ballVel.x /= timeDiff;
						ballVel.y /= timeDiff;
						
						ballSpeed = (3*ballSpeed+ballVel.length())/4;
						ballVel.setLength(ballSpeed);
						
						return;
					}
				}
				//TODO paddle collisions
				//estimate paddle collision based on old position and vel
				//new vel is collsion point - new point
				/*
				if (prevVel.x > 0) {
					ballVel = history.getStatus(0).ball.minus(nextOpponentCollision);
					ballVel.setLength(ballSpeed);
					
				}
				else {
					ballVel = history.getStatus(0).ball.minus(nextHomeCollision);
					ballVel.setLength(ballSpeed);
					
				}
				*/
				
				
				ballVel = history.getStatus(0).ball.minus(history.getStatus(1).ball);
				ballVel.x /= timeDiff;
				ballVel.y /= timeDiff;
				ballSpeed = (3*ballSpeed+ballVel.length())/4;
				ballVel.setLength(ballSpeed);
				
			}
			
			
			
		}
		else if (history.getHistorySize() == 2) {
			double timeDiff = (history.getStatus(0).time -history.getStatus(1).time)/1000.0; 
			if (timeDiff == 0)
				timeDiff = 0.001;
			ballVel = history.getStatus(0).ball.minus(history.getStatus(1).ball);
			ballVel.x /= timeDiff;
			ballVel.y /= timeDiff;
			ballSpeed = ballVel.length();
		}
		else {
			ballVel.x = 0;
			ballVel.y = 0;
		}
	}
	
	private void calculateNextCollisions() {
		
		if (ballVel.x > 0) { //going away
			
			Vector2i screenDim = history.getStatus(0).conf.screenArea;
			Vector2i paddleDim = history.getStatus(0).conf.paddleDimension;
			int radius = history.getStatus(0).conf.ballRadius;
			
			 double time = ((screenDim.x-paddleDim.x-radius) - ballPos.x) / ballVel.x;
			 Vector2 collision = new Vector2(screenDim.x-paddleDim.x-radius,ballPos.y+ time * ballVel.y);
			
			 
			 while (collision.y < radius || collision.y >= screenDim.y-radius) {
				 if (collision.y < radius) {
					 collision = flip(collision, 0, radius, screenDim);
				 }
				 if (collision.y >= screenDim.y-radius) {
					 collision = flip(collision, 1, radius, screenDim);
				 }
			 }
			 
			 opponentCollisionTime = time;
			 nextOpponentCollision = collision;
			
		}
		else { //coming our way
			Vector2i screenDim = history.getStatus(0).conf.screenArea;
			Vector2i paddleDim = history.getStatus(0).conf.paddleDimension;
			int radius = history.getStatus(0).conf.ballRadius;
			
			 double time = ((paddleDim.x+radius) - ballPos.x) / ballVel.x;
			 Vector2 collision = new Vector2(paddleDim.x+radius,ballPos.y+time * ballVel.y);
			
			 
			 while (collision.y < radius || collision.y >= screenDim.y-radius) {
				 if (collision.y < radius) {
					 collision = flip(collision, 0, radius, screenDim);
				 }
				 if (collision.y >= screenDim.y-radius) {
					 collision = flip(collision, 1, radius, screenDim);
				 }
			 }
			 homeCollisionTime = time;
			 nextHomeCollision = collision;
		}
		
		
	} 
	
	public void addState(GameStatusSnapShot status) {
		history.addSnapShot(status);
		calculateBallVelocity();
		calculateNextCollisions();
		//status.left.y
	}
	void addCommand(double value) {
		
	}
	
	
	public Vector2 getLastBallPos() {
		return ballPos;
	}
	public Vector2 getLastBallVel() {
		 return ballVel;			
	}
	public Vector2 getNextOpponentCollision() {
		return nextOpponentCollision;
	}
	public Vector2 getNextHomeCollision() {
		return nextHomeCollision;
	}
	

	
}

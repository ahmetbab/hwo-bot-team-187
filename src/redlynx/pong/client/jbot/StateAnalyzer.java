package redlynx.pong.client.jbot;

import redlynx.pong.client.state.GameStatusSnapShot;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

public class StateAnalyzer {

	public class Collision {
		public Vector2 pos;
		public Vector2 dir;
		public double time;
		
		Collision() {
			pos = new Vector2();
			dir = new Vector2();
		}
		public String toString() {
			return "t:"+time+" "+pos+" "+dir;
		}
	}
	
	HistoryBuffer history;
	
	Vector2 ballPos;
	
	Vector2 ballVel;
	Collision nextOpponentCollision;
	Collision prevOpponentCollision;
	 
	Collision nextHomeCollision;
	Collision prevHomeCollision;
	
	double ballSpeed = 0;
	double tickInterval = 0;
	
	int radius;
	public StateAnalyzer() {
		history = new HistoryBuffer();
		ballVel = new Vector2();
		ballPos = new Vector2();
		nextHomeCollision = new Collision();
		nextOpponentCollision = new  Collision();
		prevHomeCollision = new Collision();
		prevHomeCollision.time = -1000000;
		prevOpponentCollision = new Collision();
		prevOpponentCollision.time = -1000000;
		radius = 0;
	}
	void resetMatch() {
		history.reset();
		ballVel.x = 0;
		ballVel.y = 0;
		ballPos.x = 0;
		ballPos.y = 0;
		
	}
	
	private static Vector2 flip(Vector2 p, int dir, int radius, Vector2i screenDimensions) {
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
	
	public static int foldToScreen(Vector2 p, int radius, Vector2i screenDim) {
		
		int folds = 0;
		 while (p.y < radius || p.y >= screenDim.y-radius) {
			 if (p.y < radius) {
				 p.y = flip(p, 0, radius, screenDim).y;
				 folds++;
			 }
			 if (p.y >= screenDim.y-radius) {
				 p.y = flip(p, 1, radius, screenDim).y;
				 folds++;
			 }
		 }
		 return folds;
	}
	
	private void calculateBallVelocity() {
		GameStatusSnapShot status =history.getStatus(0); 
		ballPos = status.ball;
		int radius = status.conf.ballRadius;
		
		
		if (history.getHistorySize() >= 3) {
			Vector2 prevVel = ballVel;
			
			GameStatusSnapShot current = history.getStatus(0);
			GameStatusSnapShot prev = history.getStatus(1);
			GameStatusSnapShot older = history.getStatus(2);
			
			Vector2 p1 = current.ball;
			Vector2 p2 = prev.ball;
			Vector2 p3 = older.ball;
			
			
			//System.out.println("time diff c-p "+(current.time - prev.time));
			//System.out.println("nopTime  "+(1000*nextOpponentCollision.time));
			if (current.time - prev.time > 1000*nextOpponentCollision.time) {
				p2 = nextOpponentCollision.pos;
				ballVel = p1.minus(p2);
				ballVel.setLength(ballSpeed);
				prevOpponentCollision.pos = nextOpponentCollision.pos;
				prevOpponentCollision.time = nextOpponentCollision.time;
				return;
			}
			else if (current.time - prev.time > 1000*nextHomeCollision.time) { 
				p2 = nextHomeCollision.pos;
				ballVel = p1.minus(p2);
				ballVel.setLength(ballSpeed);
				prevHomeCollision.pos = nextHomeCollision.pos;
				prevHomeCollision.time = nextHomeCollision.time;
				return;
			}
			//System.out.println("time diff c-o "+(current.time - older.time));
			//System.out.println("popTime  "+(-1000*prevOpponentCollision.time));
			if (current.time - older.time > -1000*prevOpponentCollision.time) {
				p3 = prevOpponentCollision.pos;
			}
			else if (current.time - older.time > -1000*prevHomeCollision.time) {
				p3 = prevHomeCollision.pos;
			}
			
			
			
			double timeDiff = (current.time -older.time)/1000.0; 
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
					Vector2 p3f = flip(p3, i, radius, current.conf.screenArea);
					err = PongUtil.pointDistance2Line(p1, p2, p3f);
					//System.out.println("err " +err);
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
		if (history.getHistorySize() >=2) {
			prevHomeCollision.time -= (history.getStatus(0).time - history.getStatus(1).time)/1000.0;
			prevOpponentCollision.time -= (history.getStatus(0).time - history.getStatus(1).time)/1000.0;
		}
		
		
		Vector2i screenDim = history.getStatus(0).conf.screenArea;
		Vector2i paddleDim = history.getStatus(0).conf.paddleDimension;
		int radius = history.getStatus(0).conf.ballRadius;
		double screenTravelTime = Math.abs((screenDim.x-2*paddleDim.x-2*radius)/ballVel.x);
		
		if (ballVel.x > 0) { //going away
		
			 double time = ((screenDim.x-paddleDim.x-radius) - ballPos.x) / ballVel.x;
			
			 Vector2 collision = new Vector2(screenDim.x-paddleDim.x-radius,ballPos.y+ time * ballVel.y);
			 
			 int folds = foldToScreen(collision, radius, screenDim);
			 			 
			 nextOpponentCollision.pos = collision;
			 nextOpponentCollision.time = time;
			 nextOpponentCollision.dir.x = ballVel.x;
			 nextOpponentCollision.dir.y = (folds%2==0?1:-1)*ballVel.y;
			 
			
			 
			 nextHomeCollision.time = time + screenTravelTime;
			 Vector2 homeCollision = new Vector2(collision.x-ballVel.x*screenTravelTime, collision.y+(folds%2==0?1:-1)*ballVel.y*screenTravelTime);
			 folds = foldToScreen(homeCollision, radius, screenDim);
			 nextHomeCollision.pos =  homeCollision;
			 nextHomeCollision.dir.x = -nextOpponentCollision.dir.x;
			 nextHomeCollision.dir.y = (folds%2==0?1:-1)*nextOpponentCollision.dir.y;
		}
		else if (ballVel.x < 0) { //coming our way
		
			
			 double time = ((paddleDim.x+radius) - ballPos.x) / ballVel.x;
			 Vector2 collision = new Vector2(paddleDim.x+radius,ballPos.y+time * ballVel.y);
			 
			 int folds = foldToScreen(collision, radius, screenDim);
			 
			 nextHomeCollision.pos = collision;
			 nextHomeCollision.time = time;
			 nextHomeCollision.dir.x = ballVel.x;
			 nextHomeCollision.dir.y = (folds%2==0?1:-1)*ballVel.y;
			 
			 nextOpponentCollision.time = time + screenTravelTime;
			 Vector2 oppCollision = new Vector2(collision.x- nextHomeCollision.dir.x*screenTravelTime, collision.y+ nextHomeCollision.dir.y*screenTravelTime);
			 folds = foldToScreen(oppCollision, radius, screenDim);
			 nextOpponentCollision.pos = oppCollision;
			 nextOpponentCollision.dir.x = -nextHomeCollision.dir.y;
			 nextOpponentCollision.dir.y = (folds%2==0?1:-1)*nextHomeCollision.dir.y;
			 //System.out.println("next opp "+nextOpponentCollision.pos);
		}
		else {
			nextHomeCollision.time = 1000000;
			nextOpponentCollision.time = 1000000;
		}
		
		
	} 
	
	public void addState(GameStatusSnapShot status) {
		history.addSnapShot(status);
		if (history.getHistorySize() == 2) {
			tickInterval = history.getStatus(0).time-history.getStatus(1).time;
		}
		else if (history.getHistorySize() >= 3) {
			long diff = history.getStatus(0).time-history.getStatus(1).time;
			tickInterval = (tickInterval*4+diff) / 5; //smoothed estimate 
		}
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
	public Collision getNextOpponentCollision() {
		return nextOpponentCollision;
	}
	public Collision getNextHomeCollision() {
		return nextHomeCollision;
	}
	
	public double getTickIntervalEstimate() {
		return tickInterval;
	}

	
}

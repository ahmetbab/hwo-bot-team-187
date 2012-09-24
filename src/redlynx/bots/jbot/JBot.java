package redlynx.bots.jbot;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import redlynx.pong.client.BaseBot;
import redlynx.pong.client.Pong;
import redlynx.pong.client.collisionmodel.LinearModel;
import redlynx.pong.client.collisionmodel.PongModel;
import redlynx.pong.client.network.Communicator;
import redlynx.pong.client.network.PongMessageParser;
import redlynx.pong.client.state.GameStatusSnapShot;
import redlynx.pong.client.state.PaddleVelocityStorage;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.PongVisualizer;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.Vector2;

public class JBot implements BaseBot, PongMessageParser.ParsedMessageListener 
{


	public static void main(String[] args) {
		Pong.init(args, new JBot());
	}
	
    private final PongModel collisionModel = new LinearModel();

	private double prevCommand;
	private long lastCommandTimeMillis;
	
	private String name;
	private PongMessageParser parser;
	private PongVisualizer visualizer;
	private Communicator comm;
	protected StateAnalyzer analyser;
	protected PaddleVelocityStorage paddleVelocity = new PaddleVelocityStorage();

	private class Targeting {
		public int paddleHitPixel;
		public Vector2 deflectionVector;
		public Vector2 aimedTarget;
		public Targeting() {
			deflectionVector = new Vector2();
			aimedTarget = new Vector2();
			
		}
	}
	private Targeting targeting;
	
	private Deque<Long> messageLimiter;

    public JBot() {
		parser = new PongMessageParser(this);
		analyser = new StateAnalyzer();
		messageLimiter = new ArrayDeque<Long>();
		prevCommand = 0;
		targeting = new Targeting();
	}

    public StateAnalyzer getAnalyzer() {
		return analyser;
	}
	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setVisualizer(PongVisualizer visualizer) {
		this.visualizer = visualizer;	
	}
	@Override
	public void setCommunicator(Communicator comm) {
		this.comm = comm;
	}

	@Override
	public GameStateAccessorInterface getGameStateAccessor() {
		return new JBotStateAccessor(this);
	}

	
	
	@Override
	public void messageReceived(String msg) {
		parser.onReceivedJSONString(msg);
	}
	
	@Override
	public void gameStart(String player1, String player2) {
		analyser.resetMatch();
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameOver(String winner) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameStateUpdate(GameStatusSnapShot status) {
		
		synchronized (this) {
			analyser.addState(status);
			paddleVelocity.update(status.left.y, status.time);
			act();
			if (visualizer != null)
				visualizer.render();
		}
		
	}
	
	
	private ArrayList<Vector2> deflectedVectors = new ArrayList<Vector2>();
	
	private ArrayList<Vector2> attackers = new ArrayList<Vector2>();
	
	private double defend() {
		GameStatusSnapShot status = analyser.history.getStatus(0);
		StateAnalyzer.Collision col = analyser.getNextOpponentCollision(); 
		
		int ph = status.conf.paddleDimension.y;
		int screenWidth = status.conf.screenArea.x-2*status.conf.paddleDimension.x-2*status.conf.ballRadius;
		
		double totalWeight = 0;
		double totalY = 0;
		double minTime = 1000000;
		for (int i = 0; i < ph; i++) {
			double scaledPaddleHitPosition = (i -(ph/2))/(ph/2.0);
			
			Vector2 deflected = collisionModel.guess(scaledPaddleHitPosition, col.dir.x, col.dir.y);
					
		
			
			double time = Math.abs(screenWidth/deflected.x);
			if (time < minTime)
				minTime = time;
			double y = col.pos.y+ time*deflected.y;
			double weight = 1/time;
			totalY += y*weight;
			totalWeight += weight;
			
			if (visualizer != null) {
				Vector2 defCopy = new Vector2();
				defCopy.copy(deflected);
				deflectedVectors.add(defCopy);
				Vector2 debug = new Vector2(status.conf.paddleDimension.x+status.conf.ballRadius, y);
				StateAnalyzer.foldToScreen(debug, status.conf.ballRadius, status.conf.screenArea);	
				attackers.add(debug);
			}
		}
		//System.out.println("max paddleSpeed "+paddleVelocity.estimate);
		double bestY = totalY / totalWeight;
		Vector2 best = new Vector2(status.conf.paddleDimension.x+status.conf.ballRadius, bestY);
		int folds = StateAnalyzer.foldToScreen(best, status.conf.ballRadius, status.conf.screenArea);
		
		double y = best.y;
		
		double safeTime = minTime*0.90 - analyser.getTickIntervalEstimate()*5/1000.0;
		if (safeTime < 0)
			safeTime = 0;
		
		double borderSafeZone = status.conf.paddleDimension.y/2+ paddleVelocity.estimate*safeTime;
		if (borderSafeZone > status.conf.screenArea.y /2)
			borderSafeZone = status.conf.screenArea.y /2;
		if (y < borderSafeZone) 
			y = borderSafeZone;
		else if (y > status.conf.screenArea.y-borderSafeZone)
			y = status.conf.screenArea.y-borderSafeZone;
			
		return y;
	} 
	
	private static final double max_eval = 100000000000.0; //certain win

	//Vector2 hit = new Vector2();
	private double evalMax(double y, Vector2 hitVector, double paddleLeftY, double paddleRightY) {
		GameStatusSnapShot status = analyser.history.getStatus(0);
		int ph = status.conf.paddleDimension.y;
		
		 
		
		int screenWidth = status.conf.screenArea.x-2*status.conf.paddleDimension.x-2*status.conf.ballRadius;
		double maxPaddleSpeed = paddleVelocity.estimate;
		
		int startPixel = Math.min(status.conf.paddleDimension.y/10, 10); // TODO remove pixels that cannot be used due to corner or time*speed  
		int endPixel  = status.conf.paddleDimension.y-startPixel;
		
		Vector2 hit = new Vector2();
		
		double maxScore = 0;
		
		
		double angle = collisionModel.getAngle( hitVector.x, hitVector.y);
		for (int i = startPixel; i < endPixel; i+=4) {
			double scaledPaddleHitPosition = (i -(ph/2))/(ph/2.0);
			
			Vector2 deflected = collisionModel.guess(scaledPaddleHitPosition, hitVector.x, hitVector.y, angle);
					
		
			
			double time = Math.abs(screenWidth/deflected.x);
			double coly = y+ time*deflected.y;
			hit.y = coly;
			int folds = analyser.foldToScreen(hit, status.conf.ballRadius, status.conf.screenArea);
			//hitVector.x = deflected.x;
			//hitVector.y = folds%2 == 0? deflected.y:deflected.y;
			
			double opponentDistance = Math.abs(hit.y-paddleRightY);
			double timeToBlock = opponentDistance / maxPaddleSpeed;
			
			if (timeToBlock > time ) {
				//score += max_eval;
				maxScore = max_eval;
				
			}
			else {
				
				double s = 1-(time - timeToBlock); 
				if (s > maxScore)
					maxScore = s;
				
				
			}

		}
		return maxScore;
	
		
	}
	
	private double evalMinMax(double y, Vector2 hitVector,double timeForHit, double paddleLeftY, double paddleRightY) {

		GameStatusSnapShot status = analyser.history.getStatus(0);
		int ph = status.conf.paddleDimension.y;
		
		 
		
		int screenWidth = status.conf.screenArea.x-2*status.conf.paddleDimension.x-2*status.conf.ballRadius;
		double maxPaddleSpeed = paddleVelocity.estimate;
		
		int startPixel = Math.min(status.conf.paddleDimension.y/10, 10); // TODO remove pixels that cannot be used due to corner or time*speed  
		int endPixel  = status.conf.paddleDimension.y-startPixel;
		
		
		Vector2 hit = new Vector2();
		Vector2 hitV = new Vector2();
		
		double minScore = 1000000000000.0;

		{
			double opponentDistance = Math.abs(y-paddleRightY);
			double timeToBlock = opponentDistance / maxPaddleSpeed;
			if (timeToBlock > timeForHit) {
				return max_eval+(timeToBlock-timeForHit);
			}
		}
		
		for (int i = startPixel; i < endPixel; i++) {
			double scaledPaddleHitPosition = (i -(ph/2))/(ph/2.0);
			
			Vector2 deflected = collisionModel.guess(scaledPaddleHitPosition, hitVector.x, hitVector.y);
					
		
			
			double time = Math.abs(screenWidth/deflected.x);
			double coly = y+ time*deflected.y;
			
			hit.y = coly;
			
			
			int folds = analyser.foldToScreen(hit, status.conf.ballRadius, status.conf.screenArea);
			hitV.x = deflected.x;
			hitV.y = folds%2 == 0? deflected.y:deflected.y;
			
			
			double myDistance = Math.abs(coly-paddleLeftY);
			double timeToBlock = myDistance / maxPaddleSpeed;
			
			if (timeToBlock > time ) {
				minScore = -max_eval-(timeToBlock-timeForHit);
				
			}
			else {
				
				
				double testScore = evalMax( coly, hitVector, paddleLeftY, paddleRightY);//TODO estimate paddle positions
				if (testScore < minScore) {
					minScore = testScore;
				}
				
				
				
			}

		}
		
	
		return minScore;
		
	
	}
	private double attack() {
		GameStatusSnapShot status = analyser.history.getStatus(0);
		double myPaddleY = status.left.y+status.conf.paddleDimension.y/2;
		double opponentPaddleY = status.right.y+status.conf.paddleDimension.y/2;
		StateAnalyzer.Collision col = analyser.getNextHomeCollision();
		
		int ph = status.conf.paddleDimension.y;
		int screenWidth = status.conf.screenArea.x-2*status.conf.paddleDimension.x-2*status.conf.ballRadius;
		double maxPaddleSpeed = paddleVelocity.estimate;

		Vector2 hit = new Vector2();
		Vector2 hitVector = new Vector2();
		hit.x = status.conf.screenArea.x-status.conf.paddleDimension.x-status.conf.ballRadius;
		
		double minTime = 100000000; 
		int bestDeflectionIx = 0;
		Vector2 targetVector = new Vector2();
		Vector2 targetCollision = new Vector2();
		
		int startPixel = Math.min(status.conf.paddleDimension.y/10, 5); 
		int endPixel  = status.conf.paddleDimension.y-startPixel;
		
		
		double estimatedPosLeft = status.conf.screenArea.y / 2;
		double estimatedPosRight = status.conf.screenArea.y / 2;
		
		double maxScore = -100000000000000.0;
	
		
		for (int i = startPixel; i < endPixel; i++) {
			double scaledPaddleHitPosition = (i -(ph/2))/(ph/2.0);
			
			Vector2 deflected = collisionModel.guess(scaledPaddleHitPosition, col.dir.x, col.dir.y);
					
		
			
			double time = Math.abs(screenWidth/deflected.x);
			double y = col.pos.y+ time*deflected.y;
			
			hit.y = y;
			double opponentDistance = Math.abs(y-opponentPaddleY);
			double timeToBlock = opponentDistance / maxPaddleSpeed;
			
			int folds = analyser.foldToScreen(hit, status.conf.ballRadius, status.conf.screenArea);
			
			hitVector.x = deflected.x;
			hitVector.y = folds%2 == 0? deflected.y:deflected.y;
			
			if (visualizer != null) {
				Vector2 defCopy = new Vector2();
				defCopy.copy(deflected);
				deflectedVectors.add(defCopy);
				Vector2 debug = new Vector2();
				debug.copy(hit);	
				attackers.add(debug);
			}
			double value = evalMinMax(hit.y, hitVector, time, myPaddleY, opponentPaddleY);
			
			//System.out.println("value "+value);
			if (value > maxScore) {
				maxScore = value;
		
				//System.out.println("maxScore updated");
		
			//if (time - timeToBlock  < minTime) {
				bestDeflectionIx = i;
				minTime = (time-timeToBlock);
				targeting.aimedTarget.copy(hit);
				targeting.deflectionVector.copy(deflectedVectors.get(deflectedVectors.size()-1));
				
			}
			
			
		
		}
		targeting.paddleHitPixel = bestDeflectionIx;
		
		return col.pos.y-(targeting.paddleHitPixel-status.conf.paddleDimension.y/2);
		
	}
	
	
	private void act() {
		long timer = System.nanoTime();
		GameStatusSnapShot status = analyser.history.getStatus(0);
		deflectedVectors.clear();
		attackers.clear();
		if (analyser.getLastBallVel().x > 0) {
	
			double y = defend();
			moveTo(y, analyser.getNextOpponentCollision().time);
		}
		else {
			double y = attack();
			//System.out.println("Collision: "+analyser.getNextHomeCollision());
			
			//moveTo(analyser.getNextHomeCollision().pos.y, analyser.getNextHomeCollision().time);
			moveTo(y, analyser.getNextHomeCollision().time);
		}
		System.out.println("time "+((System.nanoTime()-timer)/1000000.0));
	}
	
	private void moveDir(float dir) {
		
		
		long timeMillis = System.nanoTime()/1000000;
		//System.out.println("timedif "+(time - lastMoveCommandTime)+" dirDiff "+ Math.abs(dir- prevCommand));
		if (Math.abs(dir- prevCommand) > 0.1 || 
			timeMillis - lastCommandTimeMillis > 200) {
			
			if (messageLimiter.size() >= 10) {
				//System.out.println("limiter "+(timeMillis - messageLimiter.peekFirst()));
				if (timeMillis - messageLimiter.peekFirst() < 1200) { //max 10 messages in 1.2 seconds
					return; 
				}
				messageLimiter.remove();
			} 
			messageLimiter.add(timeMillis);
		
			paddleVelocity.update(dir);
			prevCommand = dir;
			lastCommandTimeMillis = timeMillis;
			comm.sendUpdate(dir);
		}
	}
	
	public void moveTo(double y, double inTime) {
		GameStatusSnapShot status = analyser.history.getStatus(0);
		y -= status.conf.paddleDimension.y/2;
		if (y < 0)
			y = 0;
		
		if (y > status.conf.screenArea.y-status.conf.paddleDimension.y)
			y = status.conf.screenArea.y-status.conf.paddleDimension.y;
		
		
			
		double paddlePos = status.left.y; 
		double maxPaddleSpeed = paddleVelocity.estimate == 0?50:paddleVelocity.estimate;

		double dist = (y-paddlePos);
		
		//System.out.println("tickInterval "+analyser.getTickIntervalEstimate());
		//System.out.println("dist "+Math.abs(dist)+" "+analyser.getTickIntervalEstimate()/1000.0f*0.1*maxPaddleSpeed);
		double tickIntervalInSeconds = analyser.getTickIntervalEstimate()/1000.0f;
		double absDist = Math.abs(dist);
		double lagEstimate = tickIntervalInSeconds/2;
		if (lagEstimate > inTime)
			inTime -= lagEstimate;
		
		
		if (absDist < tickIntervalInSeconds/2*0.1*maxPaddleSpeed && absDist < status.conf.paddleDimension.y/2) {
			moveDir(0);
			
		}
		else if (absDist > 5*tickIntervalInSeconds*maxPaddleSpeed) {
			if (paddlePos < y)
				moveDir(1);
			else
				moveDir(-1);
		}
		else {
		
			double distanceInTime = (maxPaddleSpeed*inTime)+0.001;
				
			float vel = (float)(dist / distanceInTime);
			if (vel < -1)
				vel = -1;
			else if (vel > 1)
				vel = 1;
			
			
			//System.out.println(" dist "+dist+" "+maxPaddleSpeed+" "+distanceInTime+" vel "+vel+" in time "+inTime);
			moveDir( vel );
		}
	}
	

	
	@Override
	public void start() {
		try {
			while(true) {
					Thread.sleep(100);	
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    @Override
    public String getDefaultName() {
        return "JBot";
    }

    
	public ArrayList<UILine> getExtraLines() {
		
		ArrayList<UILine> lines = new ArrayList<UILine>();
		
		StateAnalyzer.Collision col = null;
		if (analyser.getLastBallVel().x > 0)
			col = analyser.getNextOpponentCollision();
		else 
			col = analyser.getNextHomeCollision();
		for (int i = 0;i < deflectedVectors.size(); i++) {
			lines.add(UILine.createFromDirection(col.pos, deflectedVectors.get(i), 1, Color.blue));
		}
		for (int i = 0; i < attackers.size(); i++) {
		
			Vector2 pos = attackers.get(i);
			lines.add(new UILine(pos.x-1,pos.y,pos.x+1,pos.y, Color.cyan));
		}
		
		
		lines.add(new UILine(targeting.aimedTarget.x-1,targeting.aimedTarget.y,targeting.aimedTarget.x+1,targeting.aimedTarget.y, Color.red));
		if (analyser.getLastBallVel().x < 0)
			lines.add(UILine.createFromDirection(col.pos, targeting.deflectionVector, 1, Color.red));
		
		
		return lines;
	}

}

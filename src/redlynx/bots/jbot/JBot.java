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
		
		double borderSafeZone = status.conf.paddleDimension.y/2+ paddleVelocity.estimate*minTime;
		if (borderSafeZone > status.conf.screenArea.y /2)
			borderSafeZone = status.conf.screenArea.y /2;
		if (y < borderSafeZone) 
			y = borderSafeZone;
		else if (y > status.conf.screenArea.y-borderSafeZone)
			y = status.conf.screenArea.y-borderSafeZone;
			
		return y;
	} 
	
	private double attack() {
		GameStatusSnapShot status = analyser.history.getStatus(0);
		double opponentPaddleY = status.right.y+status.conf.paddleDimension.y/2;
		StateAnalyzer.Collision col = analyser.getNextHomeCollision();
		
		int ph = status.conf.paddleDimension.y;
		int screenWidth = status.conf.screenArea.x-2*status.conf.paddleDimension.x-2*status.conf.ballRadius;
		double maxPaddleSpeed = paddleVelocity.estimate;

		Vector2 hit = new Vector2();
		hit.x = status.conf.screenArea.x-status.conf.paddleDimension.x-status.conf.ballRadius;
		
		double minTime = 100000000; 
		int bestDeflectionIx = 0;
		Vector2 targetVector = new Vector2();
		Vector2 targetCollision = new Vector2();
		
		int startPixel = Math.min(status.conf.paddleDimension.y/10, 5); 
		int endPixel  = status.conf.paddleDimension.y-startPixel;
		
		for (int i = startPixel; i < endPixel; i++) {
			double scaledPaddleHitPosition = (i -(ph/2))/(ph/2.0);
			
			Vector2 deflected = collisionModel.guess(scaledPaddleHitPosition, col.dir.x, col.dir.y);
					
		
			
			double time = Math.abs(screenWidth/deflected.x);
			double y = col.pos.y+ time*deflected.y;
			
			hit.y = y;
			
			int folds = analyser.foldToScreen(hit, status.conf.ballRadius, status.conf.screenArea);
			
			double opponentDistance = Math.abs(y-opponentPaddleY);
			double timeToBlock = opponentDistance / maxPaddleSpeed;
			if (time - timeToBlock  < minTime) {
				bestDeflectionIx = i;
				minTime = (timeToBlock-time);
				targeting.aimedTarget.copy(hit);
				targeting.deflectionVector.copy(deflected);
				
			}
			
			if (visualizer != null) {
				Vector2 defCopy = new Vector2();
				defCopy.copy(deflected);
				deflectedVectors.add(defCopy);
				Vector2 debug = new Vector2();
				debug.copy(hit);	
				attackers.add(debug);
			}
		}
		targeting.paddleHitPixel = bestDeflectionIx;
		
		return col.pos.y-(targeting.paddleHitPixel-status.conf.paddleDimension.y/2);
		
	}
	
	
	private void act() {
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
		
		if (absDist < tickIntervalInSeconds*0.1*maxPaddleSpeed) {
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

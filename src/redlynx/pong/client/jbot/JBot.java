package redlynx.pong.client.jbot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Vector;

import redlynx.pong.client.BaseBot;
import redlynx.pong.client.Pong;
import redlynx.pong.client.collisionmodel.LinearModel;
import redlynx.pong.client.collisionmodel.PongModel;
import redlynx.pong.client.network.Communicator;
import redlynx.pong.client.network.PongMessageParser;
import redlynx.pong.client.state.GameStatusSnapShot;
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

	double prevCommand;
	
	private String name;
	private PongMessageParser parser;
	private PongVisualizer visualizer;
	private Communicator comm;
	protected StateAnalyzer analyser;

    public JBot() {
		parser = new PongMessageParser(this);
		analyser = new StateAnalyzer();
		prevCommand = 0;
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
			act();
			if (visualizer != null)
				visualizer.render();
		}
		
	}
	
	
	private ArrayList<Vector2> deflectedVectors = new ArrayList<Vector2>();
	
	private ArrayList<Vector2> attackers = new ArrayList<Vector2>();
	
	private void act() {
		GameStatusSnapShot status = analyser.history.getStatus(0);
		deflectedVectors.clear();
		attackers.clear();
		if (analyser.getLastBallVel().x > 0) {
			
			StateAnalyzer.Collision col = analyser.getNextOpponentCollision(); 
			
			int ph = status.conf.paddleDimension.y;
			int screenWidth = status.conf.screenArea.x-2*status.conf.paddleDimension.x-2*status.conf.ballRadius;
			
			double totalWeight = 0;
			double totalY = 0;
			for (int i = 0; i < ph; i++) {
				double scaledPaddleHitPosition = (i -(ph/2))/(ph/2.0);
				
				Vector2 deflected = new Vector2();
				deflected.copy(collisionModel.guess(scaledPaddleHitPosition, col.dir.x, col.dir.y));
				deflectedVectors.add(deflected);
				
				double time = Math.abs(screenWidth/deflected.x);
				double y = col.pos.y+ time*deflected.y;
				double weight = 1/time;
				totalY += y*weight;
				totalWeight += weight;
				
				if (visualizer != null) {
					Vector2 debug = new Vector2(status.conf.paddleDimension.x+status.conf.ballRadius, y);
					analyser.foldToScreen(debug, status.conf.ballRadius, status.conf.screenArea);	
					attackers.add(debug);
				}
			}
			double bestY = totalY / totalWeight;
			Vector2 best = new Vector2(status.conf.paddleDimension.x+status.conf.ballRadius, bestY);
			int folds = analyser.foldToScreen(best, status.conf.ballRadius, status.conf.screenArea);
			
			double y = best.y;
			if (y < status.conf.screenArea.y/4) //TODO fix hard coding with reasonable values based on paddle speed
				y = status.conf.screenArea.y/4;
			else if (y > 3*status.conf.screenArea.y/4)
				y = 3*status.conf.screenArea.y/4;
				
			
			//moveTo(status.conf.screenArea.y/2-status.conf.paddleDimension.y/2, 0);
			moveTo(y, 0);
		}
		else {
			moveTo(analyser.getNextHomeCollision().pos.y, 0);
		}
	}
	
	private void moveDir(float dir) {
		
		//TODO enforce 10 commands / sec limit
		if (dir != prevCommand) {
			prevCommand = dir;
			comm.sendUpdate(dir);
		}
	}
	
	public void moveTo(double y, double inTime) {
		GameStatusSnapShot status = analyser.history.getStatus(0);
		double paddlePos = status.left.y; 
		if (paddlePos + status.conf.paddleDimension.y -2< y) {
			moveDir(1);
		}
		else if (paddlePos + 2> y){
			moveDir(-1);
		}
		else {
			moveDir(0);
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
		StateAnalyzer.Collision col = analyser.getNextOpponentCollision(); 
		for (int i = 0;i < deflectedVectors.size(); i++) {
			lines.add(UILine.createFromDirection(col.pos, deflectedVectors.get(i), 4, Color.blue));
		}
		for (int i = 0; i < attackers.size(); i++) {
		
			Vector2 pos = attackers.get(i);
			lines.add(new UILine(pos.x-1,pos.y,pos.x+1,pos.y, Color.cyan));
		}
		
		
		
		
		return lines;
	}

}

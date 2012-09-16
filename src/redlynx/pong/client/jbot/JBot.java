package redlynx.pong.client.jbot;

import redlynx.pong.client.BaseBot;
import redlynx.pong.client.Pong;
import redlynx.pong.client.network.Communicator;
import redlynx.pong.client.network.PongMessageParser;
import redlynx.pong.client.state.GameStatusSnapShot;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.PongVisualizer;

public class JBot implements BaseBot, PongMessageParser.ParsedMessageListener 
{


	public static void main(String[] args) {
		Pong.init(args, new JBot());
	}
	
	private String name;
	private PongMessageParser parser;
	private PongVisualizer visualizer;
	private Communicator comm;
	private StateAnalyzer analyser;
	public JBot() {
		parser = new PongMessageParser(this);
		analyser = new StateAnalyzer();
		
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
	
	
	
	private void act() {
		GameStatusSnapShot status = analyser.history.getStatus(0);
		if (analyser.getLastBallVel().x > 0)
			moveTo(status.conf.screenArea.y/2-status.conf.paddleDimension.y/2);
		else {
			moveTo(analyser.getNextHomeCollision().y);
		}
	}
	private void moveTo(double y) {
		GameStatusSnapShot status = analyser.history.getStatus(0);
		double paddlePos = status.left.y; 
		if (paddlePos + status.conf.paddleDimension.y < y) {
			comm.sendUpdate(1);
		}
		else if (paddlePos > y){
			comm.sendUpdate(-1);
		}
		else {
			comm.sendUpdate(0);
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
		



}

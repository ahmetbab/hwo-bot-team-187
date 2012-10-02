package redlynx.pong.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import redlynx.pong.server.ui.PongServerFrame;
import redlynx.pong.ui.PongVisualizer;
import redlynx.pong.util.WinTimerHack;

public class PongServer {
	ServerSocket providerSocket;
	Socket connection = null;
	boolean gameRunning;
	boolean paused;
	int messageInterval;
	public int missileInterval = 100;
	
	String message;
	ServerPlayer[] players;
	Thread[] playerThreads;
	
	GameState gameState;
	PongVisualizer visualizer;
	private LagSimulator lagSimulator;
	
	
	int serverPort;
	PongServer(){
		gameState = new GameState();
		messageInterval = 0;
		players = new ServerPlayer[2];
		playerThreads = new Thread[2];
	}
	public LagSimulator getLagSimulator() {
		return lagSimulator;
	}
	public void changeDir(int id, double dir) {
		//System.out.println("player"+id+": dir: "+dir);
		if (!Double.isInfinite(dir) && !Double.isNaN(dir))
			gameState.changeDir(id, dir);
	}
	public void launchMissile(int id, long missileId) {
		boolean canLaunch = players[id].launchMissile(missileId);
		if (canLaunch) {
			gameState.launchMissile(id);
		}
	}
	
	
	void connectPlayers()
	{
		
		try{
			providerSocket = new ServerSocket(serverPort, 10);
			while(true) {
				int id = -1;
				for (int i = 0; i < 2; i++) {
					if (players[i] == null) {
						id = i;
						break;
					}
						
				}
				if (id == -1)
					break;
			
				System.out.println("Waiting for connection "+id);
				Socket connection = providerSocket.accept();
				System.out.println("Connection received from " + connection.getInetAddress().getHostName());
				players[id] = new ServerPlayer(id, connection, this);
				playerThreads[id] = new Thread(players[id]);
				playerThreads[id].start();
		
			}
		
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			
			try{
				providerSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}

	public synchronized void updatePlayer(int id, String name) {
		gameState.setPlayers(players[0]==null?null:players[0].getName(),players[1]==null?null: players[1].getName());
		visualizer.render();
		
	}
	
	public void waitForPlayersToJoin() {
		int count = 0;
		int timeout = 100; //100*10+ ms >  1 s 
		try {
			
		
			//TODO better wait logic that allows disconnection
		boolean waitMore = true;
		do {
			synchronized (this) {
				waitMore = (players[0] != null && players[1] != null &&
						(!players[0].hasJoined() || !players[1].hasJoined()) && count < timeout);	
			}
			count++;
			visualizer.render();
			Thread.sleep(10);
		} while(waitMore);
		
	
		if (count >= 2000) {
			System.out.println("players did not send join message in time!!!");
		}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
		}
	
	} 
	
	public synchronized void startGame() {
		gameState.resetGame();
		System.out.println("Starting game with: "+players[0].getName()+" and "+players[1].getName());
		
		players[0].sendStartedMessage(players[1].getName());
		players[1].sendStartedMessage(players[0].getName());
	}
	public synchronized void endGame(int winner) {
		String winnerName = winner < 0? "None": players[winner].getName();
		if (players[0] != null) 
			players[0].sendEndMessage(winnerName);
		if (players[1] != null)
			players[1].sendEndMessage(winnerName);
	}
	public synchronized void sendGameState() {
		if (players[0] != null && players[1] != null) {
			players[0].sendGameState(gameState);
			players[1].sendGameState(gameState);
		}
	}
	
	public void run(int port) {
		
		WinTimerHack.fixTimerAccuracy();
		visualizer = new PongVisualizer(gameState);
		
		new PongServerFrame(visualizer, gameState, this);
		
		this.serverPort = port;
		lagSimulator = new LagSimulator();
		lagSimulator.start();
		
		
		
		try {
			
			do {
				
				
				do {
					gameRunning = true;
					connectPlayers();
					waitForPlayersToJoin();
					if (players[0] != null && !players[0].hasJoined())
						kickPlayer(0);
					if (players[1] != null && !players[1].hasJoined())
						kickPlayer(1);
				}while(!gameRunning);
				
				gameState.setPlayers(players[0].getName(), players[1].getName());
				startGame();
				int ticksSinceMessage = 0;
				int ticksSinceMissile = 0;
				while(gameRunning) {
					if (!paused) {
						gameState.tickGame();
						visualizer.render();
						if (ticksSinceMessage >= messageInterval) {
							sendGameState(); 
							ticksSinceMessage = 0;
						}
						else {
							ticksSinceMessage++;
						}
						if (ticksSinceMissile >= missileInterval) {
							players[0].addMissile();
							players[1].addMissile();
							ticksSinceMissile = 0;
						}
						else {
							ticksSinceMissile++;
						}
						
						if (gameState.hasEnded()) {
							endGame(gameState.getWinner());
							//TODO keep track of winners
							Thread.sleep(100);
							startGame();
							ticksSinceMessage = 0;
						}
					}
					
					//TODO change speed during continuous game play
					Thread.sleep(gameState.getTickInterval());
					
				}
				if (players[0] != null) {
					endGame(players[1] == null?0:-1);
				}
				else if (players[1] != null) {
					endGame(players[0] == null?1:-1);
				}
				visualizer.render();
			}while(true);
		}catch(Exception e){
			e.printStackTrace();
			
		} 
		//TODO stuff	
	}
	
	public static void main(String args[])
	{
		try {
			int port = 9090;
			
			PongServer server = new PongServer();
			server.run(port);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	public void resetGame() {
		gameState.endGame(-1);
	}
	public synchronized void kickPlayer(int id) {
		if (players[id] != null 
			//&& players[id].hasJoined()
			) {
			players[id].disconnect();
			players[id] = null;
			gameRunning = false;
			updatePlayer(id, null);
		}
	}

	public void setPaused(boolean b) {
		paused = b;
	}

	public void setMessageInterval(int value) {
		messageInterval = value;
		
	}
	public void setMissileInterval(int value) {
		missileInterval = value;
	}

	public void setOutputLag(int value) {
		lagSimulator.setOutputLag(value);
		
	}

	public void setInputLag(int value) {
		lagSimulator.setInputLag(value);
	}
	
}

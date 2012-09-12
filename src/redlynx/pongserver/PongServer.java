package redlynx.pongserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import redlynx.pongserver.ui.PongVisualizer;

public class PongServer {
	ServerSocket providerSocket;
	Socket connection = null;
	boolean gameRunning;
	boolean paused;
	int messageInterval;
	
	String message;
	ServerPlayer[] players;
	Thread[] playerThreads;
	
	GameState gameState;
	PongVisualizer visualizer;
	
	int serverPort;
	PongServer(){
		gameState = new GameState();
		messageInterval = 0;
		players = new ServerPlayer[2];
		playerThreads = new Thread[2];
	}
	
	public void changeDir(int id, double dir) {
		//System.out.println("player"+id+": dir: "+dir);
		
		gameState.changeDir(id, dir);
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
	
	public synchronized void waitForPlayers() {
		int count = 0;
		try {
			
			//TODO better wait logic that allows disconnection
		while (players[0] != null && players[1] != null &&
				(!players[0].hasJoined() || !players[1].hasJoined()) && count < 2000) {
			count++;
			visualizer.render();
			Thread.sleep(5);
		}
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
		visualizer = new PongVisualizer(gameState, this);
		this.serverPort = port;
		
		
		
		
		try {
			
			do {
				connectPlayers();
				waitForPlayers();
				gameRunning = true;
				gameState.setPlayers(players[0].getName(), players[1].getName());
				startGame();
				int ticksSinceMessage = 0;
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
			int port = Integer.parseInt(args[0]);
			
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
		if (players[id] != null && players[id].hasJoined()) {
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

	public void setOutputLag(int value) {
		// TODO Auto-generated method stub
		
	}

	public void setInputLag(int value) {
		// TODO Auto-generated method stub
		
	}
}

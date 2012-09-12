package redlynx.pongserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PongServer {
	ServerSocket providerSocket;
	Socket connection = null;
	
	String message;
	ServerPlayer[] players;
	Thread[] playerThreads;
	
	GameState gameState;
	PongVisualizer visualizer;
	PongServer(){
		gameState = new GameState();
	}
	
	public void changeDir(int id, double dir) {
		//System.out.println("player"+id+": dir: "+dir);
		
		gameState.changeDir(id, dir);
	}
	void connectPlayers(int port)
	{
		players = new ServerPlayer[2];
		playerThreads = new Thread[2];
		try{
			providerSocket = new ServerSocket(port, 10);
			for (int i = 0; i < 2; i++) {
				System.out.println("Waiting for connection "+i);
				Socket connection = providerSocket.accept();
				System.out.println("Connection received from " + connection.getInetAddress().getHostName());
				players[i] = new ServerPlayer(i, connection, this);
				playerThreads[i] = new Thread(players[i]);
				playerThreads[i].start();
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

	public void waitForPlayers() {
		int count = 0;
		try {
		while ((!players[0].hasJoined() || !players[1].hasJoined()) && count < 2000) {
			count++;
			
			Thread.sleep(5);
		}
		if (count >= 2000) {
			System.out.println("players did not send join message in time!!!");
		}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
		}
	
	} 
	
	public void startGame() {
		gameState.resetGame();
		System.out.println("Starting game with: "+players[0].getName()+" and "+players[1].getName());
		
		players[0].sendStartedMessage(players[1].getName());
		players[1].sendStartedMessage(players[0].getName());
	}
	public void endGame(int winner) {
		players[0].sendEndMessage(players[winner].getName());
		players[1].sendEndMessage(players[winner].getName());
	}
	public void sendGameState() {
		players[0].sendGameState(gameState);
		players[1].sendGameState(gameState);
	}
	
	public void run(int port) {
		
		gameState.resetGame();
		visualizer = new PongVisualizer(gameState);
		
		connectPlayers(port);
		waitForPlayers();
		
		gameState.setPlayers(players[0].getName(), players[1].getName());
		
		startGame();
		try {
			while(true) {
				
				gameState.tickGame();
				visualizer.render();
				sendGameState(); //TODO more flexible state sending interval
				if (gameState.hasEnded()) {
					endGame(gameState.getWinner());
					//TODO keep track of winners
					Thread.sleep(100);
					startGame();
				}
				
				//TODO change speed during continuous game play
				Thread.sleep(gameState.getTickInterval());
				
			}
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
}

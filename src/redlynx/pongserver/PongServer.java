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
	PongServer(){}
	
	public void changeDir(int id, double dir) {
		System.out.println("player"+id+": dir: "+dir);
	}
	void connectPlayers()
	{
		players = new ServerPlayer[2];
		playerThreads = new Thread[2];
		try{
			providerSocket = new ServerSocket(2004, 10);
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
		players[0].sendStartedMessage(players[1].getName());
		players[1].sendStartedMessage(players[0].getName());
	}
	public static void main(String args[])
	{
		PongServer server = new PongServer();
		
		server.connectPlayers();
		server.waitForPlayers();
		server.startGame();
		//TODO stuff	
		
		
	}
}

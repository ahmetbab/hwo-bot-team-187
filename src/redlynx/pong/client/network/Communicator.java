package redlynx.pong.client.network;

public interface Communicator {
	 public void sendJoin(String data);  
	 public void sendUpdate(float data);
}

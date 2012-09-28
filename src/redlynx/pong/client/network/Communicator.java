package redlynx.pong.client.network;

public interface Communicator {
    public void sendJoin(String data);
    public void sendUpdate(float data);
    public void sendRequestMatch(String name, String matchBot);
	public void sendFireMissile(long remove);
}

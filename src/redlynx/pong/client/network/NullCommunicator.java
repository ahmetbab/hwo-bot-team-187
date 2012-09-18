package redlynx.pong.client.network;

public class NullCommunicator implements Communicator {

    @Override
    public void sendJoin(String data) {
    }

    @Override
    public void sendUpdate(float data) {
    }

    @Override
    public void sendRequestMatch(String name, String matchBot) {
    }
}

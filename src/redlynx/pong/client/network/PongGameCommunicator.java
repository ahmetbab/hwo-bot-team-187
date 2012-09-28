package redlynx.pong.client.network;

import java.io.PrintStream;

public class PongGameCommunicator implements Communicator {

    private final PrintStream out;

    public PongGameCommunicator(PrintStream out) {
        this.out = out;
    }

    public void sendJoin(String data) {
        out.print("{\"msgType\":\"join\",\"data\":\"" + data + "\"}\n");
        out.flush();
    }

    public void sendUpdate(float data) {
        out.print("{\"msgType\":\"changeDir\",\"data\":" + data + "}\n");
        out.flush();
    }

    @Override
    public void sendRequestMatch(String name, String matchBot) {
        out.print("{\"msgType\":\"requestDuel\",\"data\":[\"" + name + "\",\""+ matchBot + "\"]}\n");
        out.flush();
    }

	@Override
	public void sendFireMissile(long id) {
		out.print("{\"msgType\":\"launchMissile\",\"data\":" + id + "}\n");
		out.flush();
		
	}
}

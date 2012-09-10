package redlynx.pong;

import java.io.PrintStream;

public class PongGameCommunicator {

    private final PrintStream out;

    public PongGameCommunicator(PrintStream out) {
        this.out = out;
    }

    public void sendJoin(String data) {
        out.print("{\"msgType\":\"join\",\"data\":\"" + data + "\"}");
        out.flush();
    }

    public void sendUpdate(float data) {
        out.print("{\"msgType\":\"changeDir\",\"data\":\"" + data + "\"}");
        out.flush();
    }
}

package redlynx.pong;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ArrayBlockingQueue;

public class PongListenerThread extends Thread {

    private final InputStreamReader inputStream;
    private final ArrayBlockingQueue<String> outputQueue;

    public PongListenerThread(InputStream netInput, ArrayBlockingQueue<String> outputQueue) {
        this.inputStream = new InputStreamReader(netInput);
        this.outputQueue = outputQueue;
    }

    public final void run() {
        String msg = "";
        while(true) {
            try {
                char c = (char) inputStream.read();
                if(c == '\n') {
                    // msg complete, offer to game.
                    // if game not handling messages as fast as server sending them
                    // will discard overflow messages.
                    outputQueue.offer(msg);
                    msg = new String("");
                } else {
                    msg += c;
                }
            } catch (IOException e) {
                // if io exception, quit?
                break;
            }
        }
    }
}

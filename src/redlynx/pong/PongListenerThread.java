package redlynx.pong;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Queue;

public class PongListenerThread extends Thread {


    private final InputStreamReader inputReader;
    private final Queue<String> outputQueue;

    public PongListenerThread(InputStream input, Queue<String> outputQueue) {
        this.inputReader = new InputStreamReader(input);
        this.outputQueue = outputQueue;
    }


    public final void run() {

        String msg = "";
        int depth = 0;
        while(true) {
            try {

                int cInt = inputReader.read();
                char c = (char) cInt;
                if (cInt == 65535) //end of stream
                    break;
                msg += c;
                switch(c) {
                    case '{':
                        depth++;
                        break;
                    case '}':
                        depth--;
                        if (depth == 0) {
                            try {
                                outputQueue.add(msg);
                            }
                            finally {
                                msg = "";
                            }
                        }
                        else if (depth < 0) {
                            System.err.println("Invalid server message");
                            msg = "";
                            depth = 0;
                        }
                }

            } catch (IOException e) {
                // if io exception, quit?
                break;
            }
        }

    }
}

package redlynx.pong.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PongListenerThread extends Thread {


    private final InputStreamReader inputReader;
    
    private final PongMessageListener listener;


    public PongListenerThread(InputStream input, PongMessageListener listener) {
        this.inputReader = new InputStreamReader(input);
        this.listener = listener;

    }


    public final void run() {
        StringBuilder msg = new StringBuilder();
        int depth = 0;
        while(true) {
            try {
                char c = (char) inputReader.read();
                if (c == 65535) //end of stream
                    break;

                msg.append(c);
                switch(c) {
                    case '{':
                    	depth++;
                        break;
                    case '}':
                        depth--;
                        if (depth == 0) {
                            try {
                            	listener.messageReceived(msg.toString());
                            }
                            finally {
                                msg = new StringBuilder();
                            }
                        }
                        else if (depth < 0) {
                            System.err.println("Invalid server message");
                            msg = new StringBuilder();
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

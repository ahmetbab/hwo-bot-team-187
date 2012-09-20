package redlynx.pong.client.network;

import java.util.ArrayList;

public class MessageLimiter {

    private double currentTime = 0;
    private ArrayList<Double> msgs = new ArrayList<Double>();

    public boolean canSend() {
        return msgs.size() < 9;
    }

    public void send() {
        msgs.add(currentTime);
    }

    public void tick(double dt) {
        currentTime += dt;
        while(!msgs.isEmpty() && currentTime - msgs.get(0) > 1) {
            msgs.remove(0);
        }
    }
}

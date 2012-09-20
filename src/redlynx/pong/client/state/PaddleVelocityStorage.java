package redlynx.pong.client.state;

import redlynx.pong.ui.UILine;
import redlynx.pong.util.Vector2i;

import java.util.ArrayList;
import java.awt.Color;

public class PaddleVelocityStorage {

    private double paddleSpeed = 0;
    ArrayList<Entry> history = new ArrayList<Entry>();

    public double estimate = 0;

    public PaddleVelocityStorage() {
    	this(50);
    }
    public PaddleVelocityStorage(double defaultMaxSpeedGuess) {
    	estimate = defaultMaxSpeedGuess;
    }
    
    public void update(double speed) {
        this.paddleSpeed = speed;
        history.clear();
    }

    public void update(double y, long time) {
        if(paddleSpeed == 0)
            return;

        history.add(new Entry(time, y));

        if(history.size() > 2) {
            Entry start = history.get(1);
            Entry end = history.get(history.size()-1);

            double dt = (end.time - start.time) * 0.001;

            if(dt < 0.1) {
                return;
            }

            double dy = end.y - start.y;
           // System.out.println("dt "+dt+" ps "+paddleSpeed+" es "+estimate );
            double sample = Math.abs((dy / dt) / paddleSpeed);
            
            int historyWeight = 5;
            estimate =  (estimate*historyWeight+ sample)/(historyWeight+1); // fast adaptation to varying paddle move speeds.
            //System.out.println("estimate: " + estimate);
        }
    }

    public void drawReachableArea(ArrayList<UILine> lines, double paddle_y, double timeLeft, double paddleHeight) {
        double reach = estimate * timeLeft + paddleHeight * 0.5;
        lines.add(new UILine(new Vector2i(5, paddle_y + reach), new Vector2i(5, paddle_y - reach), Color.cyan));
        lines.add(new UILine(new Vector2i(2, paddle_y + reach), new Vector2i(2, paddle_y - reach), Color.cyan));
        lines.add(new UILine(new Vector2i(8, paddle_y + reach), new Vector2i(8, paddle_y - reach), Color.cyan));
    }

    private class Entry {
        long time;
        double y;

        public Entry(long time, double y) {
            this.time = time;
            this.y = y;
        }
    }
}

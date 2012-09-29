package redlynx.pong.client.state;

import redlynx.pong.ui.UILine;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;
import java.awt.Color;

import java.util.ArrayList;

public class BallPositionHistory {

    private ArrayList<Vector2> history = new ArrayList<Vector2>();
    private int historyPointer = 0;

    private Vector2 lastCollision = null;
    private double lastCollisionTime = 0;

    public BallPositionHistory() {
        reset();
    }

    public void update(Vector2 position) {
        history.get(historyPointer).copy(position);
        historyPointer = ++historyPointer % history.size();
    }

    public boolean isReliable() {
        double error = PongUtil.pointDistance2Line(history.get(0), history.get(1), history.get(2));
        return error < 1;
    }

    public void storeCollision(Vector2 position, double totalTime) {
        this.lastCollision = position;
        this.lastCollisionTime = totalTime;
    }

    public void reset() {
        history.clear();
        history.add(new Vector2(0, 0));
        history.add(new Vector2(10, 10));
        history.add(new Vector2(10, 0));

        lastCollision = null;
    }

    public void drawLastCollision(ArrayList<UILine> lines) {
        if(lastCollision == null)
            return;

        int size = 5;
        lines.add(new UILine(new Vector2i(lastCollision.x-size, lastCollision.y-size), new Vector2i(lastCollision.x + size, lastCollision.y + size), Color.orange));
        lines.add(new UILine(new Vector2i(lastCollision.x-size, lastCollision.y+size), new Vector2i(lastCollision.x + size, lastCollision.y - size), Color.orange));
    }

    public Vector2 getLastCollisionPoint() {
        return lastCollision;
    }

    public double getLastCollisionTime() {
        return lastCollisionTime;
    }


}

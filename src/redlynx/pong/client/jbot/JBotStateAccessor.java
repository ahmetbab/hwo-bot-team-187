package redlynx.pong.client.jbot;

import java.awt.Color;
import java.util.ArrayList;

import redlynx.pong.client.state.GameStatusSnapShot;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.UILine;
import redlynx.pong.ui.UIString;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

public class JBotStateAccessor implements GameStateAccessorInterface{

	
	

    private final JBot bot;
    private final StateAnalyzer analyzer;
    private GameStatusSnapShot status;

    
    public JBotStateAccessor(JBot bot) {
        this.bot = bot;
        this.analyzer = bot.getAnalyzer();
        status = new GameStatusSnapShot();
    }
    public String getPlayerName(int id) {
    	return id == 0? status.left.name:status.right.name;
    }
 
    private int maxStates() {
    	return Math.min(7, analyzer.history.getHistorySize());
    }
    
	@Override
	public int getNumberOfStatesToRender() {
		return maxStates();
	}
	@Override
	public Color getRenderColor(int stateIdx) {
		int maxStates = maxStates();
		int id = maxStates()-1-stateIdx;
		Color c = Color.white;
		for (int i = 0; i < id; i++) 
			c = c.darker();
		return c;
	};
	@Override
	public void setRenderState(int stateIdx) {
		
		int id = maxStates()-1-stateIdx;
		status = analyzer.history.getStatus(id);
	}
    

    @Override
    public int getBallRadius() {
        return status.conf.ballRadius;
    }

    @Override
    public double getPedalY(int id) {
        return id == 0?status.left.y:status.right.y;
    }
    
    @Override
    public Vector2 getBallPos() {
    	return new Vector2(status.ball.x,status.ball.y);
    }
    
    
    
    @Override
    public Vector2i getPedalDimensions() {
    	return status.conf.paddleDimension;
    }
    
    
    @Override
    public Vector2i getAreaDimensions() {
    	return status.conf.screenArea;
    }
    
    private void drawX(Vector2 pos, ArrayList<UILine> lines) {
    	lines.add(new UILine(pos.x-5,pos.y-5, pos.x+5,pos.y+5, Color.yellow));
    	lines.add(new UILine(pos.x-5,pos.y+5, pos.x+5,pos.y-5, Color.yellow));
    	
    }
	@Override
	public ArrayList<UILine> getExtraLines() {
		
		Vector2 pos = analyzer.getLastBallPos();
		Vector2 vel = analyzer.getLastBallVel();
		ArrayList<UILine> lines = new ArrayList<UILine>();
		lines.add(UILine.createFromDirection(pos, vel, 1, Color.red));
		
		Vector2 colPos = analyzer.getNextOpponentCollision();
		if (colPos != null)
			drawX(colPos, lines);
		Vector2 homeColPos = analyzer.getNextHomeCollision();
		if (homeColPos != null)
			drawX(homeColPos, lines);
        return lines;
	}

    @Override
	public ArrayList<UIString> getExtraStrings() {
		return null;
	}
}

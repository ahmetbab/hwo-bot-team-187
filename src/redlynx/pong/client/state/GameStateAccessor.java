package redlynx.pong.client.state;

import java.awt.Color;
import java.util.ArrayList;

import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.PongGameBot.MissileHistoryItem;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.UILine;
import redlynx.pong.ui.UIString;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

public class GameStateAccessor implements GameStateAccessorInterface {

    private final PongGameBot bot;
    private ClientGameState status;
    private int renderState;

    
    public GameStateAccessor(PongGameBot bot) {
        this.bot = bot;
        status = bot.getExtrapolatedStatus();
        renderState = 0;
    }
    public String getPlayerName(int id) {
    	return id == 0? status.left.name:status.right.name;
    }
    

    //@Override
    public void fetchExtrapolated() {
        status = bot.getExtrapolatedStatus();
    }

    //@Override
    public void fetchLastKnown() {
        status = bot.getLastKnownStatus();
    }
    
	@Override
	public int getNumberOfStatesToRender() {
		return 2;
	}
	@Override
	public Color getRenderColor(int stateIdx) {return stateIdx==0?Color.white.darker().darker():Color.white;};
	@Override
	public void setRenderState(int stateIdx) {
		if (stateIdx == 0) {
			fetchLastKnown();
		}
		else  {
			fetchExtrapolated();
		}
		renderState = stateIdx;
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
    	return new Vector2i( status.conf.paddleWidth, status.conf.paddleHeight);
    }
    
    
    @Override
    public Vector2i getAreaDimensions() {
    	return new Vector2i(status.conf.maxWidth, status.conf.maxHeight);
    }
	@Override
	public ArrayList<UILine> getExtraLines() {
		
		ArrayList<UILine> lines = bot.getDrawLines(); 
		
		if (bot.missileHistory.size() > 0) {
			int tickInterval = bot.lastKnownStatus.conf.tickInterval;
			long currentTime = System.currentTimeMillis();
			
			for (int i = 0; i < bot.missileHistory.size(); i++) {
				MissileHistoryItem item = bot.missileHistory.get(i);
				long timeDiffMillis = currentTime - item.localTime;
				double diffInTicks = timeDiffMillis / (double) tickInterval;
				Vector2 pos = new Vector2(item.state.pos.x, item.state.pos.y);
				pos.x += item.state.vel.x*diffInTicks;
				pos.y += item.state.vel.y*diffInTicks;
				
				lines.add(new UILine(item.state.pos.x, item.state.pos.y,pos.x, pos.y, Color.red.darker()));
						
			}
			
		}
		
        return lines;
	}

    @Override
	public ArrayList<UIString> getExtraStrings() {
		return null;
	}
	@Override
	public ArrayList<Vector2> getMissilePositions() {
		
		ArrayList<Vector2> positions = new ArrayList<Vector2>();
		if (renderState == 0) {
			for (int i = 0; i < bot.missileHistory.size(); i++) {
				MissileHistoryItem item = bot.missileHistory.get(i);
				positions.add(item.state.pos);
			}
		}
		else { //extrapolated
			int tickInterval = bot.lastKnownStatus.conf.tickInterval;
			long currentTime = System.currentTimeMillis();
			
			for (int i = 0; i < bot.missileHistory.size(); i++) {
				MissileHistoryItem item = bot.missileHistory.get(i);
				long timeDiffMillis = currentTime - item.localTime;
				double diffInTicks = timeDiffMillis / (double) tickInterval;
				Vector2 pos = new Vector2(item.state.pos.x, item.state.pos.y);
				pos.x += item.state.vel.x*diffInTicks;
				pos.y += item.state.vel.y*diffInTicks;
				positions.add(pos);
			}
		}
		
		return positions;
	
	}
}

package redlynx.pong.client.state;

import java.awt.Color;

import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.UILine;
import redlynx.pong.ui.UIString;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

public class GameStateAccessor implements GameStateAccessorInterface {

    private final PongGameBot bot;
    private GameStatus status;

    
    public GameStateAccessor(PongGameBot bot) {
        this.bot = bot;
        status = bot.getExtrapolatedStatus();
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
	public UILine[] getExtraLines() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public UIString[] getExtraString() {
		// TODO Auto-generated method stub
		return null;
	}
    }

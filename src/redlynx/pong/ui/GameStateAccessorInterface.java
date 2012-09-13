package redlynx.pong.ui;

import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

public interface GameStateAccessorInterface {

    public double getPedalY(int id);
    public String getPlayerName(int id);
  
    public int getBallRadius();
    
    public Vector2 getBallPos();
 
    public Vector2i getAreaDimensions();
    public Vector2i getPedalDimensions();
    
    
    public UILine[] getExtraLines();
    public UIString[] getExtraString();
    
}

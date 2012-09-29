package redlynx.pong.ui;

import java.awt.Color;
import java.util.ArrayList;

import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

public interface GameStateAccessorInterface {

	public int getNumberOfStatesToRender();
	public Color getRenderColor(int stateIdx);
	public void setRenderState(int stateIdx);
	
	
    public double getPedalY(int id);
    public String getPlayerName(int id);
  
    public int getBallRadius();
    
    public Vector2 getBallPos();
 
    public Vector2i getAreaDimensions();
    public Vector2i getPedalDimensions();
    
    public ArrayList<UILine> getExtraLines();
    public ArrayList<UIString> getExtraStrings();
    
}

package redlynx.pong.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;




public class PongVisualizer extends JPanel

 {
	
	private Image imageBuffer;
	private Image renderBuffer;

	
	GameStateAccessorInterface model;
	int width;
	int height;
	
	public PongVisualizer(GameStateAccessorInterface model) {
		this.model = model;
		width = 800;
		height = 600;
		imageBuffer = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
		renderBuffer = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
	
	    this.setMinimumSize(new Dimension(width, height));
		this.setPreferredSize(new Dimension(width, height));
	
	}

	
	
	public synchronized void render() {

		Graphics g = renderBuffer.getGraphics();
		
		g.clearRect(0, 0, width, height);
		
		int cornerx = 20;
		int cornery = 40;
		int border = 5;
		//tickCounter++;
		
		
		
		String waitingForPlayer = "Waiting for player...";
		int ptx1 = cornerx;
		int ptx2 = cornerx+500;
		int pty = 20;
		g.setColor(Color.white);
		
		if (model.getPlayerName(0) != null) {
			g.drawString(model.getPlayerName(0), ptx1, pty);
		}
		else {
			g.drawString(waitingForPlayer, ptx1, pty);
		}
			
		if (model.getPlayerName(1)!= null) {
			g.drawString(model.getPlayerName(1), ptx2, pty);
		}
		else {
			g.drawString(waitingForPlayer, ptx2, pty);
		} 
		
		int renderStates = model.getNumberOfStatesToRender();
		for (int i = 0; i < renderStates; i++) {
			
			model.setRenderState(i);
			g.setColor(model.getRenderColor(i));
			
			Vector2i screenSize = model.getAreaDimensions();
			
			g.drawRect(cornerx-border, cornery-border, screenSize.x+2*border, screenSize.y+2*border);
	        g.drawRect(cornerx, cornery, screenSize.x,  screenSize.y);
	
	        Vector2i paddleDimensions = model.getPedalDimensions();
	        
	        g.drawRect(cornerx, cornery + (int)model.getPedalY(0), paddleDimensions.x, paddleDimensions.y);
			g.drawRect(cornerx+screenSize.x- paddleDimensions.x, cornery + (int)model.getPedalY(1), paddleDimensions.x, paddleDimensions.y);
			
			
			Vector2 ballPos = model.getBallPos();
			int r = model.getBallRadius();
			g.drawRect((int)(cornerx+ballPos.x-r), (int)(cornery+ballPos.y-r), 2*r, 2*r);
		}
		
		  
		
		g = imageBuffer.getGraphics();
		
		g.drawImage(renderBuffer,0,0,this);
		
		
		repaint();
		
	}
	public void paint(Graphics g) {
		g.drawImage(imageBuffer,0,0,this);
	}

	public void update(Graphics g) {
		paint(g);
	}
	
	
	
	
	
	
	

}
